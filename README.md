# SlowLight

<p align="center">
  <img src="assets/slowlight-banner.svg" alt="SlowLight — visual breathing pacer for Android" width="100%" />
</p>

<p align="center">
  <strong>A quiet, offline visual breathing pacer for Android.</strong><br />
  No account. No audio. No cloud dependency. Just a simple visual rhythm that gradually slows.
</p>

<p align="center">
  <a href="https://github.com/NUKS000/SlowLight/blob/main/LICENSE"><img alt="MIT License" src="https://img.shields.io/badge/license-MIT-blue.svg"></a>
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android-3DDC84.svg">
  <img alt="Status" src="https://img.shields.io/badge/status-working%20prototype-6f42c1.svg">
</p>

---

## What it does

SlowLight is a deliberately minimal Android app for guided breathing and relaxation.

The working prototype uses a full-screen expanding and contracting visual pacer that progressively slows from roughly **11 breaths per minute to 6 breaths per minute**.

Current prototype behaviour includes:

- 8-minute and 20-minute sessions
- progressive breathing pace
- full-screen visual guidance
- brightness control
- pause, resume and stop
- offline operation
- no accounts
- no analytics
- no advertising
- no network dependency for the core experience

The prototype has been built, installed and used on a **Samsung Galaxy S24 Ultra**.

## Why I built it

The idea came from a simple question:

> If the useful part of a breathing device is a gradually slowing visual rhythm, why does it need another piece of hardware?

SlowLight strips the concept down to the behaviour that matters: a calm, predictable visual cue with as little interface friction as possible.

This is not positioned as a medical device or treatment. It is a small software product focused on guided breathing for relaxation.

## Project status

**Working prototype. Source publication in progress.**

The Android source code will be added to this repository once it has been cleaned up for public release.

Until then, this repository documents:

- the product intent
- the current behaviour
- the design constraints
- the open-source direction
- the roadmap

When the source lands, this README will be updated with build instructions, architecture notes and release APK details.

## Design principles

SlowLight is intentionally opinionated:

1. **Offline first** — the core session should work without connectivity.
2. **Low distraction** — no feeds, streaks, gamification or noisy dashboards.
3. **Immediate use** — open the app and start.
4. **No unnecessary data collection** — the current concept does not need an account or analytics.
5. **Clear product boundary** — relaxation guidance, not medical claims.
6. **Simple over clever** — features only earn their place if they improve the breathing session.

## Planned repository structure

Once the Android source is published, the project will be organised so it is easy to understand and fork.

```text
SlowLight/
├── app/                  # Android application source
├── docs/                 # product and architecture notes
├── assets/               # repository artwork/screenshots
├── .github/              # contribution templates
├── README.md
├── CONTRIBUTING.md
├── ROADMAP.md
└── LICENSE
```

## Roadmap

See [ROADMAP.md](ROADMAP.md).

The immediate priority is publishing the working Android source in a clean, reproducible state.

## Contributing

Contributions will be welcome once the source is available.

Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

For security issues, see [SECURITY.md](SECURITY.md).

## Licence

SlowLight is released under the [MIT License](LICENSE).

That means you can use, modify, fork and redistribute the code, subject to the licence terms.

## About the builder

SlowLight was designed and built by **Josh Bohan** as a small end-to-end product exercise: identify a real problem, reduce it to the useful behaviour, build the software, put it on a physical device, and iterate from actual use.

I work across project delivery, software-enabled products and practical digital tools.

- [GitHub — NUKS000](https://github.com/NUKS000)
- [BOBOX Studio](https://www.boboxstudio.co.nz/)

BOBOX is where I publish and build websites, apps and useful digital products.

---

### Current milestone

**Prototype proven on-device → public repository established → source release next.**
