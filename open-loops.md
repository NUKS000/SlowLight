# Open loops — SlowLight

Updated: 2026-10-12 (NZDT)

## Completed in source
- [x] Session completion sets controls invisible, removes the light cue immediately, and gives a true-black screen; tapping that screen intentionally returns home.
- [x] Rear torch is switched off by the existing completion-aware effect.
- [x] Screen window effects set brightness to 0 and release `FLAG_KEEP_SCREEN_ON` upon completion; Android may then put the phone to sleep after its configured display timeout.
- [x] Added regression tests for completed vs running/paused display policy.
- [x] Bumped version to 1.4.1 and added a CI build workflow that publishes a labelled debug APK prerelease.

## Needs verification / follow-up
- [ ] Check GitHub Actions **Build SlowLight test APK** succeeds (unit tests and Android build).
- [ ] Confirm prerelease `v1.4.1-debug` offers a downloadable APK and checksum, and install on a physical Android phone.
- [ ] Check end-of-session screen is immediately black and the device subsequently sleeps after the configured screen timeout on an S24 Ultra; check both screen and torch modes and dark-screen tap-to-return.
- [ ] For a production update, produce and test a signed release with the existing production keystore, instead of distributing a debug-signed APK.
- [ ] If replacing another debug-signing key, note uninstalling the old APK may be required and local settings may be lost.

## Context
The fix intentionally does **not** request device-administrator privileges or force-lock the device. Ordinary apps cannot reliably sleep/lock the screen instantly; clearing `FLAG_KEEP_SCREEN_ON` is the appropriate least-privilege behaviour. The user asked for the phone to go dark automatically without leaving settings visible.
