# Project notes

## Product definition

SlowLight is an offline visual breathing pacer for Android. It uses a full-width illuminated area anchored to the bottom of the display: normal mode raises and lowers the light with the breath, while reduced-motion mode keeps the area stationary and gently varies opacity. The inactive session background remains true black (`#000000`).

## Current behavior

- Kotlin, Jetpack Compose, Material 3, Android DataStore
- Minimum Android 8.0 (API 26); target/compile SDK 35
- Package: `com.nuks.slowlight`
- 8- and 20-minute sessions
- BPM ramps linearly from 11 to 6 over 120 seconds
- The timing engine integrates BPM over elapsed time and uses fractional accumulated cycles, preserving phase continuity through the ramp
- 40% inhale / 60% exhale timing; at the target rate this is 4 seconds inhale and 6 seconds exhale
- screen-only brightness ceiling with pulse intensity linked to the breathing rhythm
- bedtime colours in order: Warm red, Amber, White, Green, Blue
- new installs default to softer deep Warm red; existing stored colour values retain their previous meanings where possible
- normal bottom-rising light plus a reduced-motion stationary-opacity alternative
- compact in-session overlay with remaining time, Pause/Resume, and Exit; it appears at session start or after a tap and fades after about five seconds while running
- optional setup guide with bedside-phone and ceiling-light illustrations; the preference is stored and can be changed in Settings
- optional rear-torch mode with runtime Camera permission
- Android 13+ variable torch strength when the device advertises `FLASH_INFO_STRENGTH_MAXIMUM_LEVEL > 1`; on/off torch fallback otherwise
- local settings only; no accounts, network permission, analytics, advertisements, notifications, cloud sync, or social features

## Compatibility of saved settings

Preferences are stored with Android DataStore. Existing colour values are read through explicit stable storage values rather than enum positions: old White, Red, Green, and Blue selections remain White, Warm red, Green, and Blue respectively. New installs use Warm red by default. Unknown values safely fall back to Warm red.

## Architecture

The app is a single Android application module and a single Compose activity:

- `MainActivity.kt` contains the Compose UI, integrated timing engine, local preferences repository, and rear-torch capability/controller.
- `BreathingEngine` is a pure timing model exercised by local JUnit tests.
- `PreferencesRepository` persists only user choices through Android DataStore.
- `SessionViewModel` owns session timing, pause/resume elapsed-time accounting, and control-overlay state.

## Display and torch behavior

Normal screen mode draws a full-width box from the bottom edge whose height follows the breathing scale and whose alpha follows the same scale. Reduced motion draws a stationary full-screen colour wash whose alpha follows the scale. OLED panels can render the inactive background with pixels off, while LCD panels render their normal black level.

Torch output is not standardized by Android hardware. On Android 13+ the app asks the active rear camera for `FLASH_INFO_STRENGTH_MAXIMUM_LEVEL`. If it returns a value greater than one, SlowLight maps the breathing scale to those discrete levels. On other torch-capable phones it uses torch on/off. The app turns the torch off when the session pauses, ends, exits, or leaves composition. Camera permission is requested only when the user selects torch mode and starts a session.

## Build and test

```bash
./gradlew testDebugUnitTest assembleDebug
```

The source is verified with JDK 17, Gradle 8.10.2, and Android SDK 35. GitHub Actions repeats the unit-test and debug-build gate on pushes and pull requests. A separate tag/manual workflow builds a signed release only when signing secrets are supplied, computes SHA-256, and publishes the APK as a GitHub Release asset.

## Release hygiene

Release APK binaries are not committed under `releases/`. The signed-release workflow uses a temporary runner keystore reconstructed from `ANDROID_KEYSTORE_BASE64`; passwords and aliases are repository secrets. `SHA256SUMS.txt` is uploaded with every release asset. The PolyForm Noncommercial License 1.0.0 and `Required Notice: Copyright 2026 Josh Bohan` remain unchanged.

## Deliberate non-goals

- medical or treatment claims
- accounts, cloud sync, analytics, advertising, notifications, or social features
- storing health data
- gamification, streaks, or unnecessary dependencies
- claiming variable torch strength on a phone that does not report it
- commercial use without separate written permission from Josh Bohan
