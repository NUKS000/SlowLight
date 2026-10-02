# Research basis and claim boundary

SlowLight is a visual pacing tool for gentle, self-directed breathing before sleep. It is deliberately conservative about what that means: the app offers a rhythm; it does **not** diagnose a sleep problem, treat insomnia, change blood pressure, or promise a clinical outcome.

## Why the pace slows

The app begins around 11 breaths per minute and linearly moves toward 6 breaths per minute over two minutes. The final cycle is 10 seconds: 4 seconds inhaling and 6 seconds exhaling. This is a product design choice informed by slow-breathing literature, not a prescription. Users should remain comfortable and stop if dizzy, breathless, or unwell.

A longer exhale is a common relaxation-oriented cue, but it is not appropriate to claim that it works identically for everyone.

## What the literature supports

- Reviews of slow breathing report associations with changes in autonomic measures, including heart-rate variability, and with psychological outcomes. The studies use varied protocols, populations, and outcome measures, so the evidence does not establish one universal pace or prove that a visual app produces the same results.
- Research on sleep specifically is more limited and heterogeneous than the general slow-breathing literature. A 2026 systematic review is listed below for readers who want the current evidence synthesis; its presence does not mean SlowLight has been clinically validated.
- Breathing practices are not a replacement for assessment or treatment of persistent insomnia, sleep-disordered breathing, panic symptoms, or other health concerns.

## References

1. Zaccaro A, Piarulli A, Laurino M, et al. *How Breath-Control Can Change Your Life: A Systematic Review on Psycho-Physiological Correlates of Slow Breathing.* Frontiers in Human Neuroscience. 2018;12:353. [doi:10.3389/fnhum.2018.00353](https://doi.org/10.3389/fnhum.2018.00353)
2. Lehrer PM, Gevirtz R. *Heart rate variability biofeedback: how and why does it work?* Frontiers in Psychology. 2014;5:756. [doi:10.3389/fpsyg.2014.00756](https://doi.org/10.3389/fpsyg.2014.00756)
3. Laborde S, Mosley E, Thayer JF. *Heart Rate Variability and Cardiac Vagal Tone in Psychophysiological Research: Recommendations for Experiment Planning, Data Analysis, and Data Reporting.* Frontiers in Psychology. 2017;8:213. [doi:10.3389/fpsyg.2017.00213](https://doi.org/10.3389/fpsyg.2017.00213)
4. *Slow breathing techniques before bedtime and the effects on sleep: A systematic review.* Sleep Medicine Reviews. 2026. [doi:10.1016/j.smrv.2026.102284](https://doi.org/10.1016/j.smrv.2026.102284)

## Safety language used in the app

> Breathe gently and comfortably. Do not try to breathe as deeply as possible. Stop if you feel dizzy, breathless or unwell. This is a relaxation tool, not a medical device or treatment. Discuss persistent sleep or breathing problems with an appropriate health professional.

## Research-informed engineering choices

- The app has no sound, account, feed, notifications, or data collection, reducing friction for bedtime use.
- The visual cue uses an inhale expansion and an exhale contraction; it does not force a breath hold.
- The app pauses if backgrounded, rather than continuing a session the user can no longer see.
- The rear-torch feature is optional and guarded by Android's runtime camera permission. It is an alternative light source, not a clinical feature.

## For researchers and contributors

If you add a health-related claim, cite a source, state the population and outcome precisely, and distinguish a result observed in a study from a result established for this application. Avoid efficacy promises in code, documentation, release notes, and issue discussion.