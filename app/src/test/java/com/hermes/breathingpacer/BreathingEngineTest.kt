package com.nuks.slowlight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathingEngineTest {
    private val eightMinute = BreathingConfiguration(sessionDurationSeconds = 8 * 60.0)
    private val twentyMinute = BreathingConfiguration(sessionDurationSeconds = 20 * 60.0)

    @Test fun startsAtElevenBreathsPerMinute() {
        assertEquals(11.0, BreathingEngine.snapshot(0.0, eightMinute).currentBpm, 0.0001)
    }

    @Test fun reachesSixBreathsPerMinuteAfterRamp() {
        assertEquals(6.0, BreathingEngine.snapshot(120.0, eightMinute).currentBpm, 0.0001)
        assertEquals(6.0, BreathingEngine.snapshot(300.0, eightMinute).currentBpm, 0.0001)
    }

    @Test fun rampIsLinearForOneHundredTwentySeconds() {
        assertEquals(8.5, BreathingEngine.snapshot(60.0, eightMinute).currentBpm, 0.0001)
    }

    @Test fun targetCycleIsFourSecondInhaleAndSixSecondExhale() {
        val snap = BreathingEngine.snapshot(120.0, eightMinute)
        assertEquals(10.0, snap.cycleDurationSeconds, 0.0001)
        assertEquals(4.0, snap.inhaleDurationSeconds, 0.0001)
        assertEquals(6.0, snap.exhaleDurationSeconds, 0.0001)
    }

    @Test fun integratedCyclesAreContinuousAtRampBoundary() {
        val justBefore = BreathingEngine.accumulatedCyclesAt(119.999, eightMinute)
        val atBoundary = BreathingEngine.accumulatedCyclesAt(120.0, eightMinute)
        val justAfter = BreathingEngine.accumulatedCyclesAt(120.001, eightMinute)
        assertTrue(justBefore < atBoundary)
        assertTrue(atBoundary < justAfter)
        assertEquals(17.0, atBoundary, 0.0001)
        assertEquals(0.001 * 6.0 / 60.0, justAfter - atBoundary, 0.00001)
    }

    @Test fun cueScaleDoesNotJumpAtRampBoundary() {
        val before = BreathingEngine.snapshot(119.999, eightMinute).cueScale
        val boundary = BreathingEngine.snapshot(120.0, eightMinute).cueScale
        val after = BreathingEngine.snapshot(120.001, eightMinute).cueScale
        assertEquals(0.0, boundary, 0.0001)
        assertTrue(kotlin.math.abs(before - boundary) < 0.001)
        assertTrue(kotlin.math.abs(after - boundary) < 0.001)
    }

    @Test fun calculatesInhaleAndExhalePhasesAtTargetRate() {
        assertEquals(BreathingPhase.INHALE, BreathingEngine.snapshot(120.5, eightMinute).phase)
        assertEquals(BreathingPhase.EXHALE, BreathingEngine.snapshot(124.5, eightMinute).phase)
    }

    @Test fun targetRateHasFourSecondInhaleAndSixSecondExhaleAcrossCycles() {
        assertEquals(BreathingPhase.INHALE, BreathingEngine.snapshot(130.0, eightMinute).phase)
        assertEquals(BreathingPhase.EXHALE, BreathingEngine.snapshot(134.0, eightMinute).phase)
        assertEquals(BreathingPhase.INHALE, BreathingEngine.snapshot(140.0, eightMinute).phase)
        assertEquals(BreathingPhase.EXHALE, BreathingEngine.snapshot(144.0, eightMinute).phase)
        assertEquals(BreathingPhase.INHALE, BreathingEngine.snapshot(150.0, eightMinute).phase)
    }

    @Test fun completesEightAndTwentyMinuteSessions() {
        assertFalse(BreathingEngine.snapshot(479.0, eightMinute).isComplete)
        assertTrue(BreathingEngine.snapshot(480.0, eightMinute).isComplete)
        assertFalse(BreathingEngine.snapshot(1199.0, twentyMinute).isComplete)
        assertTrue(BreathingEngine.snapshot(1200.0, twentyMinute).isComplete)
    }

    @Test fun rejectsInvalidConfigurationValues() {
        val invalid = BreathingConfiguration(startBpm = 0.0)
        try {
            BreathingEngine.snapshot(0.0, invalid)
            throw AssertionError("Expected invalid configuration to fail")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun savedPulseColourValuesRemainCompatibleAndNewDefaultIsWarmRed() {
        assertEquals(PulseColor.WARM_RED, PreferencesState().pulseColor)
        assertEquals(PulseColor.WHITE, PulseColor.fromStored(0))
        assertEquals(PulseColor.WARM_RED, PulseColor.fromStored(1))
        assertEquals(PulseColor.GREEN, PulseColor.fromStored(2))
        assertEquals(PulseColor.BLUE, PulseColor.fromStored(3))
        assertEquals(PulseColor.AMBER, PulseColor.fromStored(4))
        assertEquals(PulseColor.WARM_RED, PulseColor.fromStored(99))
    }

    @Test fun pauseResumeTimingModelCanUseAccumulatedElapsedTime() {
        val beforePause = 42.0
        val afterResumeActiveOnly = beforePause + 8.0
        assertEquals(BreathingEngine.snapshot(afterResumeActiveOnly, eightMinute).phase, BreathingEngine.snapshot(50.0, eightMinute).phase)
    }
}
