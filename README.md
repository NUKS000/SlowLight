# SlowLight

<p align="center">
  <img src="assets/slowlight-banner.svg" alt="SlowLight — visual breathing pacer for Android" width="100%" />
</p>

<p align="center">
  <strong>A quiet, offline visual breathing pacer for Android.</strong><br />
  A soft light rises from the bottom of the screen, then returns to true black as the pace gradually slows.
</p>

<p align="center">
  <a href="https://github.com/NUKS000/SlowLight/blob/main/LICENSE"><img alt="PolyForm Noncommercial License" src="https://img.shields.io/badge/license-PolyForm%20Noncommercial-blue.svg"></a>
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android-3DDC84.svg">
  <img alt="Minimum Android" src="https://img.shields.io/badge/minimum%20Android-8.0%20(API%2026)-green.svg">
</p>

## What it does

SlowLight guides paced breathing with a minimal visual cue. In screen mode, a full-width illuminated area rises from the bottom of the display during inhale and falls back toward black during exhale. The rhythm starts at about **11 breaths/minute** and ramps continuously to **6 breaths/minute** over the first two minutes. The timing engine integrates the changing BPM so the cue never jumps when the pace changes.

It includes:

- 8- and 20-minute sessions
- screen mode with a remembered maximum brightness ceiling
- five bedtime pulse colours, ordered **Warm red, Amber, White, Green, Blue**
- pulse intensity linked to the breathing rhythm
- reduced-motion mode: a stationary illuminated area with gentle opacity changes instead of changing height
- a true `#000000` active-session background, intended to let OLED pixels turn off outside the cue
- optional setup guidance with bedside-phone and ceiling-light illustrations
- rear-torch mode for face-down use
- adaptive rear-torch output: Android 13+ phones that report multiple torch levels breathe through those levels; other supported phones use an on/off rhythm
- compact in-session controls that appear briefly at start or after a tap, then fade away after about five seconds
- pause, resume, repeat, and automatic pause when backgrounded
- local-only settings; no accounts, analytics, ads, notifications, or internet permission

## Screen mode and OLED black

The active screen is black (`#000000`), immersive, and has navigation/status-bar contrast enforcement disabled where Android supports it. The selected brightness is a maximum ceiling; the breathing cue varies its own intensity within that ceiling. OLED screens can render inactive areas with pixels off, while LCD screens display their normal black level.

During a session, the default overlay contains only remaining time, Pause/Resume, and Exit. Brightness, pulse colour, reduced motion, and other choices live in Settings so the breathing screen remains quiet.

## Reduced motion

Reduced motion keeps the illuminated area stationary and varies its opacity with the same inhale/exhale rhythm. Normal mode retains the current bottom-anchored rising and falling light.

## Setup guidance

New installations show two setup illustrations before the first session: how to place the phone beside the bed with the screen facing the ceiling, and how to lie comfortably while watching the ceiling light. The guide can be disabled at the start of a session or re-enabled in Settings.

## Rear-torch mode

Choose **Rear torch** in Settings, point the rear flash safely away from eyes, and place the phone face-down before starting. SlowLight requests Android's **Camera** permission only to operate the torch; it does not take photos, record video, access the network, or store camera data.

Torch brightness is hardware-dependent:

- **Variable-capable devices:** SlowLight detects the Android 13 (API 33) `FLASH_INFO_STRENGTH_MAXIMUM_LEVEL` capability and uses `turnOnTorchWithStrengthLevel` to follow the breath rhythm.
- **Other torch-capable devices:** Android exposes only torch on/off, so SlowLight uses an on/off rhythm instead.
- **No torch detected:** torch mode will not start.

Do not use torch mode while driving or walking, and do not use a flashing light if it may trigger a health condition.

## Research and health boundary

The pacing choice is research-informed, not a treatment claim. SlowLight itself has not been clinically tested and is **not a medical device**, diagnosis, treatment, or substitute for professional care. Stop if you feel dizzy, breathless, or unwell.

See [docs/RESEARCH.md](docs/RESEARCH.md) for the evidence summary and claim boundaries.

## Build from source

### Requirements

- Android Studio or JDK 17
- Android SDK Platform 35 / Build Tools 35
- An Android device or emulator running Android 8.0 (API 26) or later

```bash
git clone https://github.com/NUKS000/SlowLight.git
cd SlowLight
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The package name is `com.nuks.slowlight`.

## Release process

Debug builds are for development and local testing. Release APKs are published as GitHub Release assets, not committed to the source tree. The signed release workflow runs for a `v*` tag or from **Actions → Build signed release** and requires these repository secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

The workflow writes the keystore only to the runner's temporary directory, builds the signed APK, generates `SHA256SUMS.txt`, and uploads both to the GitHub Release. Never commit a keystore, signing password, or local signing properties.

## Verification

The current main branch is version **1.4.0**. The continuous timing engine, reduced-motion behavior, settings persistence, and session timing are covered by local JUnit tests. Physical OLED/LCD appearance, device-specific torch strength, camera-permission denial/revocation, and signed release installation still require device validation.

## Privacy

SlowLight stores only its settings on this phone through Android DataStore. It has no internet permission, account system, analytics SDK, advertising, notifications, or cloud service. Camera permission is requested only if you choose rear-torch mode.

## Licence

SlowLight is source-available under the [PolyForm Noncommercial License 1.0.0](LICENSE).

You may use, study, modify, and redistribute SlowLight for permitted non-commercial purposes. **Commercial use is not permitted without a separate written licence from Josh Bohan.** The required notice is preserved in [LICENSE](LICENSE).
