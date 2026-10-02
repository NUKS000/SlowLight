# Project notes

## Product definition

SlowLight is an offline visual breathing pacer for Android. It uses a vertical centre line that expands symmetrically into a full white screen and retracts into a true-black (`#000000`) background.

## Version 1.1.0

- Kotlin, Jetpack Compose, Material 3, Android DataStore
- Minimum Android 8.0 (API 26); target/compile SDK 35
- Package: `com.nuks.slowlight`
- 8- and 20-minute sessions
- 11 BPM to 6 BPM linear two-minute ramp
- 40% inhale / 60% exhale timing
- immersive true-black screen mode with session-only brightness override
- optional rear-torch mode with runtime Camera permission
- Android 13+ variable torch strength when the device advertises `FLASH_INFO_STRENGTH_MAXIMUM_LEVEL > 1`; on/off torch fallback otherwise
- local settings only; no network permission or analytics
- unit tests for timing, ramp, phase calculation, completion, invalid configuration, and pause/resume time accounting

## Architecture

The app is a single Android application module and a single Compose activity:

- `MainActivity.kt` contains the Compose UI, timing engine, local preferences repository, and rear-torch capability/controller.
- `BreathingEngine` is a pure timing model exercised by local JUnit tests.
- `PreferencesRepository` persists only user choices through Android DataStore.
- `SessionViewModel` owns session timing and lifecycle state.

## Display and torch behaviour

Screen mode draws a white box with full display height and a width proportional to the breathing scale. At 0 it disappears into black; at 1 it reaches the full screen width. OLED panels can render the inactive background with pixels off, while LCD panels render their normal black level.

Torch output is not standardized by Android hardware. On Android 13+ the app asks the active rear camera for `FLASH_INFO_STRENGTH_MAXIMUM_LEVEL`. If it returns a value greater than one, SlowLight maps the breathing scale to those discrete levels. On other torch-capable phones it uses torch on/off. The app always turns the torch off when the session pauses, ends, exits, or leaves composition.

## Build and test

```bash
./gradlew testDebugUnitTest assembleDebug
```

The source has been verified with JDK 17, Gradle 8.10.2, and Android SDK 35. The visual and torch experience still needs review on a range of physical devices, particularly OLED/LCD black levels and manufacturer camera implementations.

## Deliberate non-goals

- medical or treatment claims
- accounts, cloud sync, analytics, advertising, or social features
- storing health data
- claiming variable torch strength on a phone that does not report it
