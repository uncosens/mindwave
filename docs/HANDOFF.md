# Mindwave hand-off

Read this first if you are picking up the project.

## What it is

An offline Android app (Kotlin, Jetpack Compose, Material 3). No server, no browser.
Each day the user reads two sentences aloud and talks freely for 10-20 s.
The app compares the voice to the user's own baseline and shows mood (valence),
energy (arousal) and stability, plus a history graph.

Background:
- Design spec, tokens, prototype, orb reference: `design/` (start with `design/DESIGN.md`).
- Research notes and citations: `docs/research/voice-emotion-accuracy.md`.

## Product rules from the user

- App language is English. Replies to the user are in Korean.
- Do not show "how it works" text in the app (no algorithm, baseline or research
  explanations). Keep only instructions, errors, confirmations and the
  "Mindwave is not a medical tool." line.
- Reading accuracy is used only to check that the right text was read.
  It is not part of the analysis.
- "Start over" during a recording is allowed once per step.

## Flow

1. First run: Welcome -> mic permission -> calibration: read the anchor 3 times.
2. Check-in: anchor (`Sentences.ANCHOR`, 62 words) -> sentence of the day ->
   free talk with live transcript (or read a random passage instead) -> mood 1-5
   -> analyzing -> result.
3. History (7/30/all days) and Settings (recalibrate, delete all data).

If `Sentences.ANCHOR` changes, the app asks for recalibration, and only check-ins
read with the current anchor join the baseline.

## Code map

| Path | What |
|---|---|
| `app/.../MainViewModel.kt` | Screen state machine, recording, check-in flow |
| `app/.../MainActivity.kt` | Screen routing, mic permission |
| `audio/Recorder.kt`, `audio/Dsp.kt` | 16 kHz capture (UNPROCESSED source if supported), RMS, YIN pitch |
| `speech/Transcriber.kt` | Vosk offline STT, word timestamps, live streaming for free talk |
| `analysis/VoiceFeatures.kt` | Feature extraction and the quality gate |
| `analysis/Analyzer.kt` | Metrics, robust baseline (median/MAD, 14 days), arousal/valence/stability |
| `analysis/PersonalWeights.kt` | Ridge regression from mood ratings to voice-valence weights |
| `analysis/Sentiment.kt` | Small lexicon sentiment for the free-talk transcript |
| `data/` | Sentences, passages, JSON store (`filesDir/data.json`) |
| `ui/` | Compose screens, theme, voice orb (AGSL shader on API 33+, Canvas below) |

## Done and verified

- `gradlew testDebugUnitTest assembleDebug` passes (8 unit tests).
- Installed on a Galaxy Tab S7 (SM-T870, Android 13): the Vosk model loads
  and the welcome screen and orb render correctly.

Not verified yet: the full check-in by voice on a real device, so thresholds and
the orb's reaction to a real voice have never been seen working.

## Build and test

1. JDK 21+ (on the original machine: `C:\Users\junan\AndroidDev\jdk`) and the Android
   SDK with platform 37. Put `sdk.dir=...` in `local.properties`.
2. Fetch the speech model (about 40 MB, not in git):
   `powershell -File scripts/fetch-model.ps1`
3. `gradlew testDebugUnitTest assembleDebug`
4. `gradlew installDebug` with a device connected.

## Open tasks, in priority order

1. **End-to-end test on a phone.** Do calibration and a few check-ins. Tune:
   `FeatureExtractor.MIN_SNR_DB` (15 dB), `MIN_SPEECH_DB` (-45 dB),
   `VOICED_SEC_PER_SYLLABLE` (0.09 s), the 0.3 accuracy check in
   `MainViewModel.measure`, and the speaking threshold in `Recorder` (level 0.35).
2. **Praat-style cycle-level jitter and shimmer** (approved by the user, not started).
   Today's values are frame-to-frame pitch/amplitude changes from 40 ms YIN frames,
   which mostly track intonation, not cycle perturbation. A smaller hop does not fix it.
   - Use YIN F0 per frame as a guide (70-400 Hz) to find voiced runs.
   - Mark one waveform peak per glottal cycle, searching near the expected period
     (like Praat "To PointProcess (periodic, cc)"). Refine marks with parabolic
     interpolation; at 16 kHz a sample is 62.5 us and normal jitter is 30-100 us.
   - Keep the middle of voiced runs with at least about 5 cycles.
   - Praat limits: period within the F0 range, period factor 1.3, amplitude factor 1.6.
   - jitter (local) = mean |T_i - T_(i-1)| / mean T * 100.
     shimmer (local) = mean |A_i - A_(i-1)| / mean A * 100, A = peak amplitude per cycle.
   - Needs about 130-190 valid cycles. If fewer, drop the value, not the sample.
   - Retune `Metric.JITTER` / `Metric.SHIMMER` `minSpread` (new scale: about
     0.2-1 % jitter, 2-6 % shimmer). Old baselines become invalid: version the
     feature or trigger recalibration.
   - Optionally record at 44.1/48 kHz and downsample to 16 kHz for Vosk only.
   - Check against Praat on a few recordings (sustained /a/ and the anchor).
3. **Optional, not approved by the user yet:** add CPP/CPPS as a voice clarity metric
   and merge jitter, shimmer and CPP into one voice-quality score for stability
   (similar to AVQI), so the same thing is not counted three times.
4. Design items not built yet (see `design/DESIGN.md`): read-progress word highlight,
   step transitions with a fixed orb, live "too quiet" hint, metric detail sheet
   and chart tooltips, captured pulse. The app icon is not set; logo concepts
   are in `design/logo/`.

The three accuracy items from the research are already implemented:
per-user weights from mood (`PersonalWeights`, starts after 28 check-ins),
the quality gate with retry (`Capture`/`Reject`), and speech rate and pauses from
Vosk word timestamps (falls back to energy VAD). They still need tuning on real data.

## Known issues

- Passage mode on small phones: the passage, orb and transcript share one screen
  and may feel cramped.
- `vosk-android` 0.3.47 native libraries may not be 16 KB page aligned, which
  Google Play requires for new apps targeting Android 15+. Fine for local testing.
- Sentiment is a small word list. A small on-device text model would be better.
- The store is one JSON file with default values for new fields; there is no
  other migration.
