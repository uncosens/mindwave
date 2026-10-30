# Mindwave

An offline Android app that reads your mood from your voice. No server, no browser.

## Daily flow

1. First run: read the anchor sentence 3 times. This is your baseline.
2. Each check-in:
   - Read the short anchor sentence.
   - Read the sentence of the day.
   - Talk freely for 10 to 20 seconds.
   - Rate your mood from 1 to 5.
3. See your result: mood (negative to positive), energy (calm to energized), stability, and how each voice metric differs from your baseline.
4. History shows a graph of every analyzed day.

## How it works

- Voice metrics: pitch, pitch variation, tone energy, speech rate, breathing pauses, reading accuracy, pitch tremor and volume tremor.
- The baseline is the median of your last 14 anchor readings, so it follows slow changes.
- Speech-to-text runs on the phone with Vosk. Free talk is scored with a small word list.
- All data stays in one JSON file inside the app.

This is not a medical or psychological diagnosis.

## Build

1. Get the speech model (about 40 MB):
   `powershell -File scripts/fetch-model.ps1`
2. Build and install:
   `gradlew installDebug`

UI design specs live in `design/`.

For the current state and open tasks, see `docs/HANDOFF.md`.
