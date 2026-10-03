# SlowLight

<p align="center">
  <img src="assets/slowlight-banner.svg" alt="SlowLight — visual breathing pacer for Android" width="100%" />
</p>

<p align="center">
  <strong>A quiet, offline visual breathing pacer for Android.</strong><br />
  A central line opens into light, then returns to true black as the pace gradually slows.
</p>

<p align="center">
  <a href="https://github.com/NUKS000/SlowLight/blob/main/LICENSE"><img alt="PolyForm Noncommercial License" src="https://img.shields.io/badge/license-PolyForm%20Noncommercial-blue.svg"></a>
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android-3DDC84.svg">
  <img alt="Minimum Android" src="https://img.shields.io/badge/minimum%20Android-8.0%20(API%2026)-green.svg">
</p>

## What it does

SlowLight guides paced breathing with a minimal visual cue. In screen mode, a narrow **vertical white line** at the centre of the display expands symmetrically until the screen is white, then retracts back to black. The rhythm starts at about **11 breaths/minute** and ramps linearly to **6 breaths/minute** over the first two minutes.

It includes:

- 8- and 20-minute sessions
- screen mode with live per-session brightness control
- pulse-linked screen intensity with remembered white, red, green, and blue cue colors
- a true `#000000` active-session background, intended to let OLED pixels turn off outside the cue
- optional setup guidance with bedside-phone and ceiling-light illustrations
- rear-torch mode for face-down use
- adaptive rear-torch output: Android 13+ phones that report multiple torch levels breathe through those levels; other supported phones use an on/off rhythm
- pause, resume, repeat, and automatic pause when backgrounded
- local-only settings; no accounts, analytics, ads, or internet permission

## Screen mode and OLED black

The active screen is black (`#000000`), immersive, and has navigation/status-bar contrast enforcement disabled where Android supports it. That lets OLED screens render the inactive area with pixels off rather than a near-black grey. LCD screens will still display their normal black level.

## Rear-torch mode

Choose **Rear torch** in Settings, point the rear flash safely away from eyes, and place the phone face-down before starting. SlowLight requests Android's **Camera** permission only to operate the torch; it does not take photos, record video, access the network, or store camera data.

Torch brightness is hardware-dependent:

- **Variable-capable devices:** SlowLight detects the Android 13 (API 33) `FLASH_INFO_STRENGTH_MAXIMUM_LEVEL` capability and uses `turnOnTorchWithStrengthLevel` to follow the breath rhythm.
- **Other torch-capable devices:** Android exposes only torch on/off, so SlowLight uses an on/off rhythm instead.
- **No torch detected:** torch mode will not start.

Do not use torch mode while driving or walking, and do not use a flashing light if it may trigger a health condition.

## Research and health boundary

The pacing choice is research-informed, not a treatment claim. Slow, controlled breathing has been studied in relation to autonomic and psychological measures, while direct sleep evidence is still developing. SlowLight itself has not been clinically tested and is **not a medical device**, diagnosis, treatment, or substitute for professional care.

See [docs/RESEARCH.md](docs/RESEARCH.md) for the evidence summary, citations, and the exact scope of claims this project does *not* make.

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

## Verification

Version **1.3.0** was built with Gradle 8.10.2, Android SDK 35, JDK 17, and its timing unit tests. The debug APK is available under [`releases/SlowLight-v1.3.0-debug.apk`](releases/SlowLight-v1.3.0-debug.apk) and has SHA-256 `27c7209ab3271c1dac4f2f16fac72214b5c2a0a27bc2a6c2426629e9fc119f70`. It was initially tested on a Samsung Galaxy S24 Ultra; torch capability remains device-specific.

## Privacy

SlowLight stores its session settings locally through Android DataStore. It has no internet permission, no account system, no analytics SDK, no advertising, and no cloud service. Camera permission is requested only if you choose rear-torch mode.

## Contributing

Contributions are welcome when they preserve the app's offline-first, low-distraction design. Read [CONTRIBUTING.md](CONTRIBUTING.md), [docs/PROJECT-NOTES.md](docs/PROJECT-NOTES.md), and [SECURITY.md](SECURITY.md) first.

## Licence

SlowLight is source-available under the [PolyForm Noncommercial License 1.0.0](LICENSE).

You may use, study, modify, and redistribute SlowLight for permitted non-commercial purposes. **Commercial use is not permitted without a separate written licence from Josh Bohan.** This includes selling the app, charging for access to it, incorporating it into a paid product or service, or otherwise using it with an anticipated commercial application.

If you want to use SlowLight commercially, contact the copyright holder to discuss a separate commercial licence.
