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
  "mindwave is not a medical tool." line. A short status such as
  "Still learning your usual voice" is fine.
- The app name is always lowercase: "mindwave".
- Reading accuracy is used only to check that the right text was read.
  It is not part of the analysis.
- "Try again" during a recording is allowed once per step.
- Voice and mood data never leave the phone: backup and device transfer are off.

## Flow

1. First run: Welcome -> mic pre-prompt (only if not yet granted) -> calibration:
   read the anchor 3 times.
2. Check-in: anchor (`Sentences.ANCHOR`, 62 words) -> sentence of the day ->
   free talk with live transcript (or read a random passage instead) -> review and
   correct the transcript (free talk only) -> mood 1-5 and energy 1-5 -> analyzing -> result.
   Reading steps end by themselves once the whole text is heard and the voice stops.
3. History (7/30/all days) and Settings (recalibrate, delete all data).

If `Sentences.ANCHOR` changes, the app asks for recalibration, and only check-ins
read with the current anchor join the baseline. Each calibration and check-in also stores
the phone model and mic source (UNPROCESSED or VOICE_RECOGNITION); only matching ones are
compared, and Home suggests recalibrating when they differ from the calibration.

## Code map

| Path | What |
|---|---|
| `app/.../MainViewModel.kt` | Screen state machine, recording, check-in flow |
| `app/.../MainActivity.kt` | Screen routing, mic permission |
| `audio/Recorder.kt`, `audio/Dsp.kt` | 16 kHz capture (UNPROCESSED source if supported), RMS, YIN pitch |
| `speech/Transcriber.kt` | Vosk offline STT, word timestamps, live streaming for free talk |
| `analysis/VoiceFeatures.kt` | Feature extraction, the quality gate and `FeatureExtractor.VERSION` |
| `analysis/Perturbation.kt` | Cycle-level jitter and shimmer (Praat-style cross-correlation marks) |
| `analysis/Analyzer.kt` | Metrics, robust baseline (median/MAD, 14 days), arousal/valence/stability |
| `analysis/PersonalWeights.kt` | Ridge regression: mood rating -> valence weights, energy rating -> arousal weights |
| `analysis/Sentiment.kt` | Small lexicon sentiment for the free-talk transcript |
| `data/` | Sentences, passages, JSON store (`filesDir/data.json`) |
| `ui/` | Compose screens, theme, voice orb (AGSL shader on API 33+, Canvas below) |

## Done and verified

- `gradlew testDebugUnitTest assembleDebug` passes (14 unit tests) on the original machine.
- Installed on a Galaxy Tab S7 (SM-T870, Android 13): the app opens, the Slices launcher
  icon shows, the Vosk model loads, and the welcome and calibration screens render.
- The merged manifest has no INTERNET permission (only RECORD_AUDIO).
- Recently added, built but not yet tried by voice:
  - Backup off: `allowBackup="false"`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`.
  - Energy 1-5 next to mood (`CheckIn.energy`, null on older days); `PersonalWeights.arousal`
    learns from it like `valence` learns from mood.
  - Clipping check: more than 0.1 % of speech samples at |x| >= 32700 -> `Reject.CLIPPED`.
  - Device model and mic source stored and compared (see Flow).
  - Results are shown at half strength with "Still learning your usual voice" while the
    day's anchor baseline has fewer than `MainViewModel.MIN_BASELINE` (7) readings.
  - Free-talk transcript review: the edited text is scored; `CheckIn.transcriptRaw` keeps
    the original.
  - Read-progress uses edit-distance alignment (`TextTools.readProgress`), so misheard words
    no longer stall it; a take counts as long enough when accuracy >= 0.8.
  - Mic pre-prompt screen, Home icon buttons, lowercase app name.
- Native libraries are 16 KB page aligned (Vosk 0.3.75, JNA 5.19.1); checked in the APK and
  on a Galaxy S23+ (Android 16), where the compatibility warning no longer appears.
- Light mode only (`Theme.Mindwave` with `forceDarkAllowed=false`); checked on the S23+ with
  system dark mode on.

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
   `VOICED_SEC_PER_SYLLABLE` (0.09 s), the clipping limit (0.1 %), the 0.3 accuracy check in
   `MainViewModel.measure`, and the speaking threshold in `Recorder` (level 0.35).
   Test checklist, each with calibration and at least one check-in:
   - quiet room
   - fan or road noise
   - whisper
   - loud voice (expect the clipping retry when very close)
   - fast speech
   - hoarse voice
   - airplane mode (everything must still work)
   - 30 days of daily use (baseline, "still learning" state, personal weights after 28 days)
2. **Check cycle jitter/shimmer on real voices.** The Praat-style method is built (see
   "Cycle-level jitter and shimmer" below) but has only been compared with Praat on
   synthetic voices. Record sustained /a/ and the anchor on the phone, run Praat
   (`To PointProcess (periodic, cc)` 70-400 Hz, then jitter/shimmer local with 0.0001, 0.02,
   1.3, 1.6) and compare. Also check how often the anchor and the daily sentence reach
   `Perturbation.MIN_CYCLES` (130); if the daily sentence often falls short, lower it.
   Optional, not done: record at 44.1/48 kHz and downsample to 16 kHz for Vosk only.
3. **Optional, not approved by the user yet:** add CPP/CPPS as a voice clarity metric
   and merge jitter, shimmer and CPP into one voice-quality score for stability
   (similar to AVQI), so the same thing is not counted three times.
4. Check the new UI on a phone (see "UI items built" below), then the design items still
   missing (see `design/DESIGN.md`): captured pulse (4.3), the Analyzing → Result shared-element
   orb (4.7), result reveal and chart draw-in motion (6), the History "Day detail" bottom sheet
   (the tooltip opens the Result screen instead), and the read-progress fallback from voiced
   time when no recognizer words arrive.

The three accuracy items from the research are already implemented:
per-user weights from mood and energy (`PersonalWeights`, starts after 28 check-ins),
the quality gate with retry (`Capture`/`Reject`), and speech rate and pauses from
Vosk word timestamps (falls back to energy VAD). They still need tuning on real data.

## Cycle-level jitter and shimmer

`Perturbation.measure` replaces the old frame-to-frame values.
- YIN frames (70-400 Hz) give the voiced runs and the expected period; an octave jump ends a run.
- Marks: start on the largest peak (stronger polarity), then find each next cycle by normalized
  cross-correlation with the current one at lags of 0.83-1.2 x the expected period, with a
  parabolic fit for sub-sample periods. Correlation below 0.7 breaks the chain.
- Runs need 5 periods; the first and last period are dropped. Praat limits: period
  1/400-1/70 s, period factor 1.3, amplitude factor 1.6. Amplitude = parabolic peak per cycle.
- Fewer than 130 valid pairs: `jitter`/`shimmer` are null. The take is kept, and the
  baseline and z-scores skip missing values.
- Versioning: `VoiceFeatures.version` (old data reads as 1), `FeatureExtractor.VERSION = 2`.
  A calibration with an older version sends the user to recalibrate. Only check-ins with the
  current version join the baseline and personal weights.
- `minSpread`: jitter `0.2 x value + 0.05`, shimmer `0.2 x value + 0.3`. Tune on real data.

Compared with Praat (via parselmouth 0.4.7) on 7 synthetic source-filter voices
(100-300 Hz, glides, 20-25 dB SNR noise): jitter within 1 %, shimmer within 1-4 % when clean
and 5-10 % higher with noise.

## UI items built (not yet seen on a device)

- **Read-progress highlight** (DESIGN 4.4): every step now streams to Vosk while recording.
  `TextTools.readProgress` aligns the heard words to the text by edit distance;
  `ReadingText` dims unread words to 38 % and fades each in over 200 ms. It is one text node.
  Reading steps still re-transcribe the whole take afterwards for word timings, so Vosk runs
  twice on those steps; check that the delay after "Done" is acceptable.
  Passage mode no longer shows the transcript, since the highlight shows progress.
- **Step transitions** (DESIGN 3, 6): all voice steps go through `VoiceStepScreen`, which calls
  `VoiceStep` once so the orb keeps its state. The step text cross-fades with a 24 dp rise
  (300 ms). Other screens use shared-axis X in `MainActivity` (fade + 30 dp slide, 500 ms);
  voice steps share one transition key so they do not slide.
- **Metric detail sheet** (DESIGN 4.8): tap a row on the Result screen. It shows today's
  anchor value, the usual value (median of that day's anchor baseline,
  `MainViewModel.baselineBefore`) and a sparkline of the last 14 check-ins. The design's
  one-line metric explanations are left out because of the "no how-it-works text" rule.
- **Chart tooltip** (DESIGN 4.9): tap a chart to pick the nearest day; a guide line and a
  tooltip appear, and tapping the tooltip opens that day's result.
- **Live too-quiet hint** (DESIGN 7): `Recorder.tooQuiet` is true after 3 s below level 0.08
  (about -56 dBFS); the hint becomes "I can barely hear you — move a little closer".
  A quiet room before the user starts talking can also trigger it; tune on a device.

## Known issues

- Passage mode on small phones: the passage and orb share one screen and may feel
  cramped (the transcript is gone from passage mode, which helps).
- Sentiment is a small word list. A small on-device text model would be better.
- The store is one JSON file with default values for new fields; there is no
  other migration.
