package com.nuks.slowlight

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

private val Context.dataStore by preferencesDataStore("slowlight_preferences")
private const val MIN_SCREEN_BRIGHTNESS = 0.03f
private const val DEFAULT_SCREEN_BRIGHTNESS = 0.50f
private const val MAX_BRIGHTNESS = 1.00f

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SlowLightApp() }
    }
}

enum class SessionDuration(val minutes: Int) { EIGHT(8), TWENTY(20) }
enum class CueMode(val label: String) { SCREEN("Screen"), TORCH("Rear torch") }
enum class PulseColor(val label: String, val color: Color, val storedValue: Int) {
    WARM_RED("Warm red", Color(0xFFB24646), 1),
    AMBER("Amber", Color(0xFFD08A3B), 4),
    WHITE("White", Color.White, 0),
    GREEN("Green", Color(0xFF55E889), 2),
    BLUE("Blue", Color(0xFF62A8FF), 3),
    ;

    companion object {
        fun fromStored(value: Int): PulseColor = entries.firstOrNull { it.storedValue == value } ?: WARM_RED
    }
}
enum class BreathingPhase { INHALE, EXHALE }
enum class SessionStatus { IDLE, RUNNING, PAUSED, COMPLETE }

data class BreathingConfiguration(
    val startBpm: Double = 11.0,
    val targetBpm: Double = 6.0,
    val rampDurationSeconds: Double = 120.0,
    val inhaleRatio: Double = 0.40,
    val exhaleRatio: Double = 0.60,
    val sessionDurationSeconds: Double = 8 * 60.0,
) {
    fun validate() {
        require(startBpm > 0); require(targetBpm > 0); require(rampDurationSeconds >= 0)
        require(sessionDurationSeconds > 0); require(inhaleRatio > 0); require(exhaleRatio > 0)
        require(kotlin.math.abs(inhaleRatio + exhaleRatio - 1.0) < 0.0001)
    }
}

data class BreathingSnapshot(
    val elapsedSeconds: Double,
    val remainingSeconds: Double,
    val currentBpm: Double,
    val cycleDurationSeconds: Double,
    val inhaleDurationSeconds: Double,
    val exhaleDurationSeconds: Double,
    val phase: BreathingPhase,
    val phaseProgress: Double,
    val cueScale: Double,
    val isComplete: Boolean,
)

object BreathingEngine {
    fun snapshot(elapsedSecondsInput: Double, config: BreathingConfiguration): BreathingSnapshot {
        config.validate()
        val elapsed = elapsedSecondsInput.coerceIn(0.0, config.sessionDurationSeconds)
        val rampProgress = if (config.rampDurationSeconds == 0.0) 1.0 else (elapsed / config.rampDurationSeconds).coerceIn(0.0, 1.0)
        val bpm = config.startBpm + ((config.targetBpm - config.startBpm) * rampProgress)
        val cycle = 60.0 / bpm
        val inhale = cycle * config.inhaleRatio
        val exhale = cycle * config.exhaleRatio
        val completedCycles = accumulatedCyclesAt(elapsed, config)
        val cycleFraction = completedCycles - kotlin.math.floor(completedCycles)
        val cyclePosition = cycleFraction * cycle
        val phase = if (cycleFraction < config.inhaleRatio - 1e-10) BreathingPhase.INHALE else BreathingPhase.EXHALE
        val rawProgress = if (phase == BreathingPhase.INHALE) cyclePosition / inhale else (cyclePosition - inhale) / exhale
        val breathingProgress = if (phase == BreathingPhase.INHALE) rawProgress else 1.0 - rawProgress
        val scale = smooth01(breathingProgress.coerceIn(0.0, 1.0))
        return BreathingSnapshot(elapsed, config.sessionDurationSeconds - elapsed, bpm, cycle, inhale, exhale, phase, rawProgress.coerceIn(0.0, 1.0), scale, elapsed >= config.sessionDurationSeconds)
    }
    fun accumulatedCyclesAt(elapsedSecondsInput: Double, config: BreathingConfiguration): Double {
        config.validate()
        val elapsed = elapsedSecondsInput.coerceAtLeast(0.0)
        val ramp = config.rampDurationSeconds
        val slopePerSecond = if (ramp == 0.0) 0.0 else (config.targetBpm - config.startBpm) / ramp
        val cyclesDuringRamp = (config.startBpm * ramp + 0.5 * slopePerSecond * ramp * ramp) / 60.0
        return if (ramp == 0.0 || elapsed >= ramp) {
            cyclesDuringRamp + (elapsed - ramp).coerceAtLeast(0.0) * config.targetBpm / 60.0
        } else {
            (config.startBpm * elapsed + 0.5 * slopePerSecond * elapsed * elapsed) / 60.0
        }
    }
    private fun smooth01(x: Double): Double = 0.5 - 0.5 * cos(PI * x)
}

data class PreferencesState(
    val duration: SessionDuration = SessionDuration.EIGHT,
    val cueMode: CueMode = CueMode.SCREEN,
    val pulseColor: PulseColor = PulseColor.WARM_RED,
    val screenBrightness: Float = DEFAULT_SCREEN_BRIGHTNESS,
    val torchStrength: Float = 0.50f,
    val darkTheme: Boolean = true,
    val reducedMotion: Boolean = false,
    val showSessionGuide: Boolean = true,
)

class PreferencesRepository(private val context: Context) {
    private val durationKey = intPreferencesKey("duration_minutes")
    private val modeKey = intPreferencesKey("cue_mode")
    private val pulseColorKey = intPreferencesKey("pulse_color")
    private val screenBrightnessKey = floatPreferencesKey("screen_brightness")
    private val torchStrengthKey = floatPreferencesKey("torch_strength")
    private val darkKey = booleanPreferencesKey("dark_theme")
    private val reducedMotionKey = booleanPreferencesKey("reduced_motion")
    private val showSessionGuideKey = booleanPreferencesKey("show_session_guide")
    val preferences: Flow<PreferencesState> = context.dataStore.data.map { p ->
        PreferencesState(
            duration = SessionDuration.entries.firstOrNull { it.minutes == (p[durationKey] ?: 8) } ?: SessionDuration.EIGHT,
            cueMode = CueMode.entries.getOrElse(p[modeKey] ?: CueMode.SCREEN.ordinal) { CueMode.SCREEN },
            pulseColor = PulseColor.fromStored(p[pulseColorKey] ?: PulseColor.WARM_RED.storedValue),
            screenBrightness = (p[screenBrightnessKey] ?: DEFAULT_SCREEN_BRIGHTNESS).coerceIn(MIN_SCREEN_BRIGHTNESS, MAX_BRIGHTNESS),
            torchStrength = (p[torchStrengthKey] ?: 0.50f).coerceIn(0.05f, 1.0f),
            darkTheme = p[darkKey] ?: true,
            reducedMotion = p[reducedMotionKey] ?: false,
            showSessionGuide = p[showSessionGuideKey] ?: true,
        )
    }
    suspend fun setDuration(value: SessionDuration) = context.dataStore.edit { it[durationKey] = value.minutes }
    suspend fun setCueMode(value: CueMode) = context.dataStore.edit { it[modeKey] = value.ordinal }
    suspend fun setPulseColor(value: PulseColor) = context.dataStore.edit { it[pulseColorKey] = value.storedValue }
    suspend fun setScreenBrightness(value: Float) = context.dataStore.edit { it[screenBrightnessKey] = value.coerceIn(MIN_SCREEN_BRIGHTNESS, MAX_BRIGHTNESS) }
    suspend fun setTorchStrength(value: Float) = context.dataStore.edit { it[torchStrengthKey] = value.coerceIn(0.05f, 1.0f) }
    suspend fun setDarkTheme(value: Boolean) = context.dataStore.edit { it[darkKey] = value }
    suspend fun setReducedMotion(value: Boolean) = context.dataStore.edit { it[reducedMotionKey] = value }
    suspend fun setShowSessionGuide(value: Boolean) = context.dataStore.edit { it[showSessionGuideKey] = value }
}

data class AppState(
    val prefs: PreferencesState = PreferencesState(),
    val route: String = "home",
    val status: SessionStatus = SessionStatus.IDLE,
    val snapshot: BreathingSnapshot = BreathingEngine.snapshot(0.0, BreathingConfiguration()),
    val controlsVisible: Boolean = true,
    val controlsGeneration: Long = 0L,
)

class SessionViewModel(private val repo: PreferencesRepository) : ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state
    private var startedAtMs = 0L
    private var accumulatedMs = 0L

    init { kotlinx.coroutines.MainScope().launch { repo.preferences.collect { prefs -> _state.update { it.copy(prefs = prefs) } } } }
    fun show(route: String) = _state.update { it.copy(route = route, controlsVisible = true) }
    fun start(duration: SessionDuration = _state.value.prefs.duration) {
        val prefs = _state.value.prefs.copy(duration = duration)
        if (prefs.showSessionGuide) {
            _state.update { it.copy(route = "intro", status = SessionStatus.IDLE, prefs = prefs, controlsVisible = true) }
        } else beginSession(duration)
    }
    fun beginSession(duration: SessionDuration = _state.value.prefs.duration) {
        accumulatedMs = 0
        startedAtMs = SystemClock.elapsedRealtime()
        val prefs = _state.value.prefs.copy(duration = duration)
        _state.update { it.copy(route = "session", status = SessionStatus.RUNNING, prefs = prefs, snapshot = BreathingEngine.snapshot(0.0, configFor(prefs)), controlsVisible = true, controlsGeneration = it.controlsGeneration + 1) }
    }
    fun tick() {
        val current = _state.value
        if (current.status != SessionStatus.RUNNING) return
        val elapsed = accumulatedMs + (SystemClock.elapsedRealtime() - startedAtMs)
        val snapshot = BreathingEngine.snapshot(elapsed / 1000.0, configFor(current.prefs))
        _state.update { it.copy(snapshot = snapshot, status = if (snapshot.isComplete) SessionStatus.COMPLETE else SessionStatus.RUNNING, controlsVisible = if (snapshot.isComplete) true else it.controlsVisible) }
    }
    fun pause() { if (_state.value.status == SessionStatus.RUNNING) { accumulatedMs += SystemClock.elapsedRealtime() - startedAtMs; _state.update { it.copy(status = SessionStatus.PAUSED, controlsVisible = true) } } }
    fun resume() { if (_state.value.status == SessionStatus.PAUSED) { startedAtMs = SystemClock.elapsedRealtime(); showControls(); _state.update { it.copy(status = SessionStatus.RUNNING) } } }
    fun exitSession() { accumulatedMs = 0; _state.update { it.copy(route = "home", status = SessionStatus.IDLE, controlsVisible = true, snapshot = BreathingEngine.snapshot(0.0, configFor(it.prefs))) } }
    fun showControls() = _state.update { it.copy(controlsVisible = true, controlsGeneration = it.controlsGeneration + 1) }
    fun hideControls() = _state.update { it.copy(controlsVisible = false) }
    suspend fun setDuration(value: SessionDuration) = repo.setDuration(value)
    suspend fun setCueMode(value: CueMode) = repo.setCueMode(value)
    suspend fun setPulseColor(value: PulseColor) = repo.setPulseColor(value)
    suspend fun setScreenBrightness(value: Float) = repo.setScreenBrightness(value)
    suspend fun setTorchStrength(value: Float) = repo.setTorchStrength(value)
    suspend fun setDarkTheme(value: Boolean) = repo.setDarkTheme(value)
    suspend fun setReducedMotion(value: Boolean) = repo.setReducedMotion(value)
    suspend fun setShowSessionGuide(value: Boolean) = repo.setShowSessionGuide(value)
    private fun configFor(prefs: PreferencesState) = BreathingConfiguration(sessionDurationSeconds = prefs.duration.minutes * 60.0)
}

private data class TorchCapability(val cameraId: String, val maxStrength: Int) {
    val supportsVariableStrength: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxStrength > 1
}

private class TorchController(private val cameraManager: CameraManager, private val capability: TorchCapability?) {
    fun apply(breathScale: Double, selectedStrength: Float) {
        val target = capability ?: return
        val scale = breathScale.coerceIn(0.0, 1.0)
        try {
            if (scale < 0.01) {
                cameraManager.setTorchMode(target.cameraId, false)
            } else if (target.supportsVariableStrength) {
                val maximum = target.maxStrength
                val selectedMaximum = (1 + (maximum - 1) * selectedStrength.coerceIn(0.05f, 1f)).roundToInt()
                val level = (1 + (selectedMaximum - 1) * scale).roundToInt().coerceIn(1, maximum)
                cameraManager.turnOnTorchWithStrengthLevel(target.cameraId, level)
            } else {
                cameraManager.setTorchMode(target.cameraId, true)
            }
        } catch (_: SecurityException) { }
    }
    fun off() { capability?.let { try { cameraManager.setTorchMode(it.cameraId, false) } catch (_: SecurityException) { } } }
    companion object {
        fun discover(context: Context): TorchCapability? {
            val manager = context.getSystemService(CameraManager::class.java) ?: return null
            return try {
                manager.cameraIdList.firstNotNullOfOrNull { id ->
                    val chars = manager.getCameraCharacteristics(id)
                    if (chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                        TorchCapability(id, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1 else 1)
                    } else null
                }
            } catch (_: Exception) { null }
        }
    }
}

@Composable fun SlowLightApp() {
    val context = LocalContext.current.applicationContext
    val vm: SessionViewModel = viewModel(factory = object : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = SessionViewModel(PreferencesRepository(context)) as T })
    val state by vm.state.collectAsState()
    PauseSessionOnBackground(state, vm)
    val dark = state.prefs.darkTheme || state.route == "session"
    ApplySystemBars(dark, state.route == "session")
    MaterialTheme(colorScheme = if (dark) darkScheme() else lightScheme()) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (state.route) { "settings" -> SettingsScreen(state, vm); "intro" -> SessionIntroScreen(state, vm); "session" -> SessionScreen(state, vm); else -> HomeScreen(state, vm) }
        }
    }
}

@Composable fun PauseSessionOnBackground(state: AppState, vm: SessionViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state.status) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP && state.status == SessionStatus.RUNNING) vm.pause() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable fun HomeScreen(state: AppState, vm: SessionViewModel) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? Activity
    val cameraPermission = ContextCompat.checkSelfPermission(LocalContext.current, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val startSession: () -> Unit = { if (state.prefs.cueMode == CueMode.SCREEN || cameraPermission) vm.start(state.prefs.duration) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) vm.start(state.prefs.duration) }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp)) {
        Column(Modifier.weight(1f)) {
            Text("SlowLight", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text("A quiet visual rhythm that rises from the foot of the screen to guide pre-sleep breathing.", color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(28.dp))
            DurationChooser(state.prefs.duration) { scope.launch { vm.setDuration(it) } }
            Spacer(Modifier.height(18.dp))
            Text("Light source: ${state.prefs.cueMode.label}", color = MaterialTheme.colorScheme.secondary)
            Text(if (state.prefs.cueMode == CueMode.SCREEN) "Screen brightness: ${percent(state.prefs.screenBrightness)}" else "Rear torch: turn the phone face-down before starting.", color = MaterialTheme.colorScheme.secondary)
        }
        Column(Modifier.fillMaxWidth()) {
            Button(onClick = { if (state.prefs.cueMode == CueMode.TORCH && !cameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA) else startSession() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Start ${state.prefs.duration.minutes} min") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { vm.show("settings") }, modifier = Modifier.fillMaxWidth()) { Text("Settings, safety & privacy") }
        }
    }
}

@Composable fun DurationChooser(selected: SessionDuration, onSelect: (SessionDuration) -> Unit) {
    Column { Text("Session length", fontWeight = FontWeight.Medium); SessionDuration.entries.forEach { duration -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSelect(duration) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected == duration, onClick = { onSelect(duration) }); Text("${duration.minutes} minutes") } } }
}

@Composable fun SessionScreen(state: AppState, vm: SessionViewModel) {
    val context = LocalContext.current
    val capability = remember { TorchController.discover(context) }
    val controller = remember(capability) { TorchController(context.getSystemService(CameraManager::class.java), capability) }
    ApplySessionWindowEffects(if (state.prefs.cueMode == CueMode.SCREEN) state.prefs.screenBrightness else MIN_SCREEN_BRIGHTNESS)
    LaunchedEffect(state.status) { while (state.status == SessionStatus.RUNNING) { vm.tick(); delay(33) } }
    LaunchedEffect(state.status, state.controlsVisible, state.controlsGeneration) {
        if (state.status == SessionStatus.RUNNING && state.controlsVisible) {
            delay(5_000)
            vm.hideControls()
        }
    }
    LaunchedEffect(state.status, state.snapshot.cueScale, state.prefs.cueMode, state.prefs.torchStrength) {
        if (state.prefs.cueMode == CueMode.TORCH && state.status == SessionStatus.RUNNING) controller.apply(state.snapshot.cueScale, state.prefs.torchStrength) else controller.off()
    }
    DisposableEffect(controller) { onDispose { controller.off() } }
    Box(Modifier.fillMaxSize().background(Color.Black).clickable { vm.showControls() }.semantics { contentDescription = "SlowLight breathing session. ${state.snapshot.phase.name.lowercase()}." }, contentAlignment = Alignment.Center) {
        if (state.prefs.cueMode == CueMode.SCREEN) ExpandingVerticalCue(state.snapshot.cueScale.toFloat(), state.prefs.pulseColor.color, state.prefs.reducedMotion)
        AnimatedVisibility(state.controlsVisible, Modifier.align(Alignment.BottomCenter).padding(14.dp)) { SessionControls(state, vm) }
    }
}

@Composable fun ExpandingVerticalCue(scale: Float, color: Color, reducedMotion: Boolean) {
    val pulseAlpha = (0.10f + 0.90f * scale.coerceIn(0f, 1f))
    Box(Modifier.fillMaxSize()) {
        if (reducedMotion) {
            Box(Modifier.fillMaxSize().background(color.copy(alpha = pulseAlpha)))
        } else {
            Box(Modifier.fillMaxWidth().fillMaxHeight(scale.coerceIn(0f, 1f)).background(color.copy(alpha = pulseAlpha)).align(Alignment.BottomCenter))
        }
    }
}

@Composable private fun SessionControls(state: AppState, vm: SessionViewModel) {
    Surface(color = Color.Black.copy(alpha = 0.78f), shape = RoundedCornerShape(18.dp)) {
    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(formatRemaining(state.snapshot.remainingSeconds), color = Color(0xFFBDBDBD))
        if (state.status == SessionStatus.RUNNING) OutlinedButton(onClick = vm::pause) { Text("Pause") }
        if (state.status == SessionStatus.PAUSED) Button(onClick = vm::resume) { Text("Resume") }
        if (state.status == SessionStatus.COMPLETE) Button(onClick = { vm.start(state.prefs.duration) }) { Text("Repeat") }
        OutlinedButton(onClick = vm::exitSession) { Text("Exit") }
    }
    }
}

@Composable
fun SessionIntroScreen(state: AppState, vm: SessionViewModel) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("Set up your space", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Choose a comfortable position before the light begins.", color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(10.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SetupIllustration(phoneBesideBed = true)
            Text("Place the phone beside the bed with the screen facing upward toward the ceiling. Keep it stable and out of reach while you relax.", color = MaterialTheme.colorScheme.onSurface)
            SetupIllustration(phoneBesideBed = false)
            Text("Lie comfortably in bed and watch the soft light on the ceiling. Follow the words and the rising/falling light without forcing your breath.", color = MaterialTheme.colorScheme.onSurface)
        }
        SwitchRow("Show this guide at the start", state.prefs.showSessionGuide) { scope.launch { vm.setShowSessionGuide(it) } }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { vm.beginSession(state.prefs.duration) }, modifier = Modifier.fillMaxWidth()) { Text("Begin ${state.prefs.duration.minutes}-minute session") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.show("home") }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
fun SetupIllustration(phoneBesideBed: Boolean) {
    val image = if (phoneBesideBed) R.drawable.setup_phone else R.drawable.setup_bed
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().height(240.dp)) {
        Image(
            painter = painterResource(image),
            contentDescription = if (phoneBesideBed) "Phone on bedside table shining toward the ceiling" else "Person lying in bed watching the ceiling light",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
        )
    }
}

@Composable fun SettingsScreen(state: AppState, vm: SessionViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val capability = remember(context) { TorchController.discover(context) }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            DurationChooser(state.prefs.duration) { scope.launch { vm.setDuration(it) } }
            HorizontalDivider(Modifier.padding(vertical = 14.dp))
            Text("Light source", fontWeight = FontWeight.Medium)
            CueMode.entries.forEach { mode -> ChoiceRow(mode.label, state.prefs.cueMode == mode) { scope.launch { vm.setCueMode(mode) } } }
            if (state.prefs.cueMode == CueMode.SCREEN) {
                Text("Maximum screen brightness: ${percent(state.prefs.screenBrightness)}", fontWeight = FontWeight.Medium)
                Slider(value = state.prefs.screenBrightness, onValueChange = { scope.launch { vm.setScreenBrightness(it) } }, valueRange = MIN_SCREEN_BRIGHTNESS..MAX_BRIGHTNESS)
            }
            Text("Screen pulse colour", fontWeight = FontWeight.Medium)
            PulseColor.entries.forEach { color -> ChoiceRow(color.label, state.prefs.pulseColor == color) { scope.launch { vm.setPulseColor(color) } } }
            if (state.prefs.cueMode == CueMode.TORCH) {
                Text(when {
                    capability == null -> "No rear torch was detected on this phone. SlowLight will not start torch mode."
                    capability.supportsVariableStrength -> "This phone reports ${capability.maxStrength} rear-torch levels. SlowLight will breathe through the available brightness range."
                    else -> "This phone does not report variable rear-torch brightness. SlowLight will use the torch on/off rhythm instead."
                }, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                if (capability?.supportsVariableStrength == true) {
                    Text("Maximum torch strength: ${percent(state.prefs.torchStrength)}", fontWeight = FontWeight.Medium)
                    Slider(value = state.prefs.torchStrength, onValueChange = { scope.launch { vm.setTorchStrength(it) } }, valueRange = 0.05f..1.0f)
                }
                Text("Place the phone face-down with the rear torch aimed safely away from eyes. Do not use torch mode while driving, walking, or if flashes may trigger a condition.", color = MaterialTheme.colorScheme.secondary)
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp))
            SwitchRow("Dark app theme", state.prefs.darkTheme) { scope.launch { vm.setDarkTheme(it) } }
            SwitchRow("Reduced motion", state.prefs.reducedMotion) { scope.launch { vm.setReducedMotion(it) } }
            SwitchRow("Show setup illustration at session start", state.prefs.showSessionGuide) { scope.launch { vm.setShowSessionGuide(it) } }
            HorizontalDivider(Modifier.padding(vertical = 14.dp))
            Text("Safety", fontWeight = FontWeight.Medium)
            Text("Breathe gently and comfortably. Do not try to breathe as deeply as possible. Stop if you feel dizzy, breathless or unwell. This is a relaxation tool, not a medical device or treatment. Discuss persistent sleep or breathing problems with an appropriate health professional.", color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(14.dp))
            Text("Privacy", fontWeight = FontWeight.Medium)
            Text("SlowLight stores only its settings on this phone. It has no accounts, analytics, advertisements, internet permission or cloud service.", color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(18.dp))
        }
        Button(onClick = { vm.show("home") }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

@Composable fun ChoiceRow(text: String, selected: Boolean, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected, onClick); Text(text) } }
@Composable fun SwitchRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(text); Switch(checked, onChange) } }

@Suppress("DEPRECATION")
@Composable fun ApplySystemBars(dark: Boolean, inSession: Boolean) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(dark, inSession) {
        val window = activity?.window
        val background = if (dark || inSession) android.graphics.Color.BLACK else android.graphics.Color.rgb(247, 246, 242)
        window?.statusBarColor = background; window?.navigationBarColor = background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { window?.isNavigationBarContrastEnforced = false; window?.isStatusBarContrastEnforced = false }
        val lightFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window?.decorView?.systemUiVisibility = if (dark || inSession) (window.decorView.systemUiVisibility and lightFlags.inv()) else (window.decorView.systemUiVisibility or lightFlags)
        onDispose { }
    }
}

@Suppress("DEPRECATION")
@Composable fun ApplySessionWindowEffects(brightness: Float) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(brightness) {
        val window = activity?.window
        val oldBrightness = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        val oldUi = window?.decorView?.systemUiVisibility ?: 0
        val oldFlags = window?.attributes?.flags ?: 0
        window?.attributes = window?.attributes?.apply { screenBrightness = brightness.coerceIn(0.01f, 1.0f) }
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window?.decorView?.systemUiVisibility = oldUi or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        onDispose { window?.attributes = window?.attributes?.apply { screenBrightness = oldBrightness }; if (oldFlags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == 0) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); window?.decorView?.systemUiVisibility = oldUi }
    }
}

fun percent(value: Float): String = "${(value.coerceIn(0f, 1f) * 100).roundToInt()}%"
fun formatRemaining(seconds: Double): String { val total = seconds.toInt().coerceAtLeast(0); return "%d:%02d".format(total / 60, total % 60) }
fun darkScheme() = androidx.compose.material3.darkColorScheme(background = Color(0xFF0B0B0C), surface = Color(0xFF121214), primary = Color(0xFFE7E7E7), onPrimary = Color(0xFF111111), onSurface = Color(0xFFE7E7E7), secondary = Color(0xFFB5B5B8), outline = Color(0xFF333338))
fun lightScheme() = androidx.compose.material3.lightColorScheme(background = Color(0xFFF7F6F2), surface = Color.White, primary = Color(0xFF202124), onPrimary = Color.White, onSurface = Color(0xFF202124), secondary = Color(0xFF66666A), outline = Color(0xFFD8D6D0))
