# Open loops — SlowLight

Updated: 2026-10-17 (NZDT)

## Completed in source
- [x] Session completion sets controls invisible, removes the light cue immediately, and gives a true-black screen; tapping that screen intentionally returns home.
- [x] Rear torch is switched off by the existing completion-aware effect.
- [x] Screen window effects set brightness to 0 and release `FLAG_KEEP_SCREEN_ON` upon completion; Android may then put the phone to sleep after its configured display timeout.
- [x] Added regression tests for completed vs running/paused display policy.
- [x] Bumped version to 1.4.1 and added a CI build workflow that publishes a labelled debug APK prerelease.

## Google Play distribution readiness (17 October 2026)
- [x] Connected Play Console Store listings page shows default listing **Live** (last updated 11 October 2026); notification says an app update was published on 14 October 2026. These signals do **not** independently confirm a production release is publicly installable.
- [x] Android application ID confirmed as `com.nuks.slowlight`, latest source 1.4.1, versionCode 6.
- [x] GitHub prerelease `v1.4.1-debug` has a downloadable test APK at https://github.com/NUKS000/SlowLight/releases/tag/v1.4.1-debug (not production-signed).
- [ ] **BLOCKER: confirm Play Console > Test and release > Production shows an active released app, rollout status, and countries/regions.** Current connector can read the open Store listings tab but cannot navigate to Production or obtain authenticated release state.
- [x] 17 Oct: Owner opened Google Play Store listing while signed into a Google account associated with SlowLight and saw **Install**; this confirms that account can install but is **NOT evidence of public production availability**.
- [ ] Owner believes Play Store distribution is currently limited to enrolled testers. Check Play Console Dashboard / Production and **Closed testing** for production-access eligibility, 12 testers opted in continuously for 14 days, and whether the approval/application remains outstanding. Google requires an explicit Apply for production submission after qualifying test.
- [ ] Verify public availability via an Android account **not** enrolled in testing using https://play.google.com/store/apps/details?id=com.nuks.slowlight and confirm successful installation and launch.
- [ ] If production is unavailable, inspect Dashboard for required app setup / access; new personal accounts may require a closed test with at least 12 opted-in testers for 14 continuous days, then application for production access. Use internal-test opt-in link or debug prerelease for limited testers in the meantime.
- [ ] Record the final verified public install URL and production release/version in README once confirmed. Do not claim Play Store public availability solely from listing state.

## Needs verification / follow-up
- [x] GitHub Actions **Build SlowLight test APK** run 38137101346 succeeded on 2026-10-12 NZDT, including `testDebugUnitTest assembleDebug`.
- [x] Prerelease `v1.4.1-debug` contains a downloadable APK and SHA-256 checksum: https://github.com/NUKS000/SlowLight/releases/tag/v1.4.1-debug
- [ ] Install the APK on a physical Android phone; assess whether an existing installation has a different debug signing key.
- [ ] Check end-of-session screen is immediately black and the device subsequently sleeps after the configured screen timeout on an S24 Ultra; check both screen and torch modes and dark-screen tap-to-return.
- [ ] For a production update, produce and test a signed release with the existing production keystore, instead of distributing a debug-signed APK.
- [ ] If replacing another debug-signing key, note uninstalling the old APK may be required and local settings may be lost.

## Context
The fix intentionally does **not** request device-administrator privileges or force-lock the device. Ordinary apps cannot reliably sleep/lock the screen instantly; clearing `FLAG_KEEP_SCREEN_ON` is the appropriate least-privilege behaviour. The user asked for the phone to go dark automatically without leaving settings visible.
