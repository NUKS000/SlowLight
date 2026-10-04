# SlowLight roadmap

## Current release direction

- [x] Publish the offline Android source under the PolyForm Noncommercial License 1.0.0
- [x] Document reproducible build/test steps
- [x] Preserve Android 8.0 minimum support and Camera permission boundaries
- [x] Use a true-black active-session background for OLED-friendly inactive space
- [x] Use a bottom-anchored light that rises and falls with the breath
- [x] Integrate the changing 11 → 6 BPM pace continuously over the two-minute ramp
- [x] Add setup illustrations and a re-enableable first-use guide
- [x] Add remembered bedtime colours: Warm red, Amber, White, Green, Blue
- [x] Add reduced-motion stationary-intensity mode
- [x] Add compact controls that auto-hide after about five seconds
- [x] Add adaptive rear-torch mode with variable-strength detection and an on/off fallback
- [x] Run unit tests and debug builds in GitHub Actions
- [x] Prepare a secret-free source tree and a secrets-backed signed-release workflow

## Next — physical-device validation and signed release

- [ ] test normal and reduced-motion screen mode on OLED and LCD devices in a dark room
- [ ] test all bedtime colours for comfortable night use at low brightness
- [ ] test rear torch on several Android manufacturers and Android versions
- [ ] test camera-permission denial, revocation, and camera contention
- [ ] verify tap-to-show and five-second control auto-hide behavior on a phone
- [ ] create the first signed `v1.4.0` release through the GitHub Actions workflow
- [ ] verify the signed release APK installation and SHA-256 on a physical device
- [ ] complete an accessibility review

## Possible later improvements

Only if they improve the core experience without adding clutter:

- accessibility improvements
- additional gentle pace presets backed by tests
- more robust device-specific torch messaging

## Explicit non-goals

SlowLight is not a social network, gamified streak app, cloud account platform, advertising product, notification system, medical device, diagnostic tool, treatment, or source of health-data collection. Commercial use remains prohibited without separate written permission from Josh Bohan.
