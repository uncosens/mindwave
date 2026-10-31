# Mindwave — UI/UX design

Offline Android app (Kotlin, Jetpack Compose, Material 3). Reads mood from the voice.
Tokens: `design/tokens.md`. Clickable prototype: `design/prototype.html`.
Reference code: `design/compose/Theme.kt`, `design/compose/VoiceOrb.kt`.

---

## 1. Principles

1. **One thing per screen.** Each screen has one job and one primary button.
2. **The orb is the interface.** While the user speaks, nothing moves except the orb (and the
   read-progress of the text). No meters, no waveforms, no numbers.
3. **Quiet surfaces, colorful voice.** UI is near-monochrome. Color lives in the orb and in data.
4. **Compare to yourself, not to others.** Every number is shown as "vs your usual". No absolute
   scores, no "good/bad" labels, no red/green judgment.
5. **Calm language.** Short, warm, never clinical. Never claim a diagnosis.
   Footer on results: "Mindwave is not a medical tool."
6. **Private by default and say it once.** "Your voice never leaves this phone." appears on
   onboarding and in Settings. Raw audio is not stored (only features).
7. **About one minute.** The whole daily check-in should take ~60 s.

---

## 2. User flow

```
First launch
  Welcome ──► Mic permission ──► Calibration intro ──► Calibrate 1/3 ─► 2/3 ─► 3/3 ──► Ready ──► Home
                   │ denied
                   └──► Mic needed (error) ──► [Allow] / [Open settings]

Daily
  Home ──► A Anchor ──► B Sentence of the day ──► C Free talk ──► D Mood 1–5 ──► Analyzing ──► Result ──► Home
   │                         (each step: Ready → Listening → Captured; or an error → Try again)
   ├──► History ──► Day detail (bottom sheet)
   └──► Settings ──► Recalibrate (same calibration flow) / Delete all data
```

Rules:
- Check-in is a full-screen flow (no top bar actions except close ✕). Back or ✕ asks:
  **"Stop check-in?"** — "Nothing from today will be saved." — [Keep going] [Stop].
- One check-in per day. Doing it again the same day replaces today's result
  (Home shows "Redo today's check-in" as a text button).
- Baseline: rolling 14 days. Days 1–2 use calibration as the baseline. Until 7 check-ins,
  the result shows a small "Early estimate · day N of 14" label.
- Steps advance automatically after a good capture (400 ms "captured" pause, then slide).

---

## 3. Global layout

- Phone portrait only. Side padding **24 dp**. Content max width 480 dp (centered on tablets).
- Edge-to-edge; respect system bars with `WindowInsets.safeDrawing`.
- Primary button: full width, 56 dp, pill (`CircleShape`), `primary` fill, `labelLarge`.
  Sits **24 dp** above the bottom inset.
- Secondary action: `TextButton` centered under the primary button.
- Check-in top bar (56 dp): ✕ icon button left; 4 step dots centered (6 dp, 8 dp gap;
  done = `onSurface`, current = `primary` and 18 dp wide pill, upcoming = `outline`).
- Screen transitions: shared-axis X (slide 30 dp + fade, 500 ms, `easeStandard`).
  The orb is placed in a shared container so it **does not move** between steps A→B→C;
  only the text around it cross-fades.

---

## 4. Screens

Copy below is final English copy. `{}` = variable.

### 4.1 Onboarding

**Welcome**
- Top 96 dp: idle orb 160 dp, centered.
- 32 dp below: `headlineLarge` "Hear how you feel."
- 12 dp: `bodyLarge`, `onSurfaceVariant`, centered, max 300 dp wide:
  "Read a sentence, talk for a few seconds, and Mindwave tracks your mood from your voice."
- 24 dp: lock icon 16 dp + `bodyMedium` "Your voice never leaves this phone."
- Bottom: [Get started]

**Mic permission** (system dialog is shown right after a short pre-prompt)
- Mic icon in a 64 dp `primaryContainer` circle, `headlineSmall` "Mindwave listens only while
  the orb is lit.", `bodyMedium` "Audio is analyzed on the device and then thrown away."
- Bottom: [Allow microphone]

**Calibration intro**
- `labelMedium` `onSurfaceVariant` "Setup · 1 minute".
- `headlineLarge` "Let's learn your voice."
- `bodyLarge` "Read the same sentence three times, in your normal voice. Find a quiet spot."
- Bottom: [Start]

**Calibrate n/3** — same layout as Check-in A (4.3), with:
- Top bar: ✕ + text "{n} of 3" instead of dots.
- Sentence: "Every morning I walk slowly to the river and back."
- After capture 3: Ready screen.

**Ready**
- Orb in `Done · neutral` palette, 160 dp. `headlineLarge` "You're all set."
- `bodyLarge` "Check in once a day. After two weeks, Mindwave knows your usual."
- Bottom: [Go to today]

### 4.2 Home / Today

```
┌────────────────────────────────┐
│ Good evening            ◷  ⚙   │  headlineLarge + 2 icon buttons (History, Settings)
│ Thursday, October 30           │  bodyMedium onSurfaceVariant
│ (• 5-day streak)               │  pill: primaryContainer, labelMedium, 8 dp dot
│                                │
│            ( orb )             │  idle orb 160 dp, breathing, tap = start
│                                │
│  Today's check-in              │  titleLarge
│  About a minute. Read two      │  bodyMedium onSurfaceVariant
│  sentences and talk a little.  │
│                                │
│  ┌─ Last check-in · Yesterday ┐│  card surfaceContainer, radius 16, padding 16
│  │ Calm and steady             ││  titleMedium
│  │ Mood   ───●──────           ││  mini bipolar bars (valence, arousal, stability)
│  │ Energy ─────●────           ││  height 4 dp, dot 8 dp
│  │ Steady ───────●──           ││
│  └─────────────────────────────┘│
│  [ Start check-in ]            │  primary
└────────────────────────────────┘
```

Spacing: title block top 24 dp; orb 32 dp below; text 32 dp below orb; card 24 dp below.

States:
- **Done today:** orb uses today's done palette. Title "Today: {summary}". Primary [See today's result],
  text button "Redo today's check-in".
- **No check-ins yet** (after calibration): card hidden, streak pill hidden.
- **Streak 0** (missed yesterday): pill hidden. Never show "streak lost".
- **Baseline building:** under the card, `bodyMedium` "Building your baseline · day {n} of 14".

Greeting: 05–12 "Good morning", 12–18 "Good afternoon", else "Good evening".

### 4.3 Check-in A — Anchor sentence

```
┌────────────────────────────────┐
│ ✕        ▬ • • •               │
│                                │  48 dp
│  Warm up                       │  labelMedium onSurfaceVariant, centered
│  "The sun rises over the       │  headlineMedium, centered, max 320 dp
│   quiet hills."                │
│                                │  32 dp
│            ( ORB )             │  240 dp (+40 dp glow each side)
│                                │
│     Tap the orb and read       │  bodyMedium onSurfaceVariant (hint line, 20 dp tall)
│                                │
│        [ Start reading ]       │  primary → becomes [ Done ] while listening
└────────────────────────────────┘
```

- Anchor sentence is fixed for all days (same words = clean comparison).
- **Ready:** orb Idle. Hint "Tap the orb and read aloud".
- **Listening:** orb Listening, reacts to voice. Hint "Listening…". Button text "Done" (tonal style).
  Auto-finish: ≥ 1.2 s silence after ≥ 80 % of the expected words (or ≥ 70 % of expected duration if
  no word tracking). Hard cap 12 s.
- **Captured:** orb returns to idle size with one soft pulse (scale 1.0 → 1.06 → 1.0, 400 ms), hint
  "Got it", then auto-advance.
- Read progress highlight on the sentence (same as 4.4).

### 4.4 Check-in B — Sentence of the day

Same layout as A. Label: "Sentence of the day". Sentence changes daily (neutral, 10–16 words,
no emotional words), e.g. "A small boat drifted past the old wooden pier in the morning light."

**Read-progress highlight** (subtle, optional by data):
- Unread words: `onSurface` at **38 %** alpha. Read words: **100 %**. Fade per word 200 ms.
- No underline, no box. The current word is not marked separately.
- Source of progress, in order of preference:
  1. Word timestamps from the on-device recognizer used for reading accuracy
     (`SpeechRecognizer.createOnDeviceSpeechRecognizer` partial results on API 31+, or Vosk).
     Greedy-align recognized words to target words; a target word counts as read when matched
     or when a later word is matched.
  2. Fallback: estimate `wordsRead = voicedSeconds × wordsPerSecond`, where wordsPerSecond comes from
     calibration. Cap at `total − 1` until the capture finishes.
- Progress never goes backwards. If no progress source is available, show all words at 100 %.

### 4.5 Check-in C — Free talk

```
┌────────────────────────────────┐
│ ✕        • • ▬ •               │
│  Talk freely                   │  labelMedium
│  How was your day?             │  headlineMedium (rotating prompt)
│  Anything is fine. 10–20 s.    │  bodyMedium onSurfaceVariant
│                                │
│         ╭─────────╮            │  countdown ring around the orb
│        (   ORB    )            │
│         ╰─────────╯            │
│             0:14               │  labelLarge, tabular, onSurfaceVariant
│                                │
│        [ Start talking ]       │  → [ Done ] (disabled until 10 s)
└────────────────────────────────┘
```

- Prompts (rotate daily): "How was your day?", "What's on your mind?", "What did you do this morning?",
  "Tell me about something you saw today.", "What are you looking forward to?"
- **Countdown ring:** circle with radius = orb radius + 20 dp, stroke 2 dp, round cap.
  Track: `outlineVariant`. Progress: starts at 12 o'clock, clockwise, sweeps 0→360° over 20 s.
  0–10 s: progress color `onSurfaceVariant`. At 10 s a small 4 dp tick at 180° fills and the
  progress color fades to `primary` (300 ms) → "Done" enables. At 20 s capture stops automatically.
- Timer text shows remaining seconds ("0:20" → "0:00").
- Hint under timer while < 10 s: "Keep going…" ; ≥ 10 s: "Tap Done anytime".

### 4.6 Check-in D — Mood rating

```
┌────────────────────────────────┐
│ ✕        • • • ▬               │
│                                │
│  How do you feel right now?    │  headlineMedium, centered
│                                │  48 dp
│   (1)  (2)  (3)  (4)  (5)      │  5 circles, 52 dp, 12 dp gap
│            Okay                │  titleMedium, label of the selected value
│                                │
│   Very low            Great    │  labelMedium end labels under circles 1 and 5
│                                │
│         [ See result ]         │  disabled until a value is picked
└────────────────────────────────┘
```

- No orb here (the voice part is over). Circle unselected: `secondaryContainer`; selected: `primary`
  with `onPrimary` number and a 1.0 → 1.12 spring scale. Haptic `TextHandleMove` on change.
- Labels: 1 Very low · 2 Low · 3 Okay · 4 Good · 5 Great.

### 4.7 Analyzing (transition, 1–3 s)

- Full screen, orb 240 dp centered in **Analyzing** state. `bodyLarge` under it: "Listening back…".
- Minimum display 1.2 s (so it doesn't flash). Then the orb shrinks to 120 dp and moves up to the
  Result header (shared element, 500 ms), changing to the Done palette.

### 4.8 Result

Scrollable. Top bar: ✕ (closes to Home) and title "Today" (`titleLarge`).

```
  ( small orb, done palette )        120 dp, centered
  Calm, a little low                 headlineSmall, centered (summary)
  Early estimate · day 4 of 14       labelMedium onSurfaceVariant (only while < 7 days)

  ┌ Your state ───────────────────┐  card
  │ Energy   calm ───●──── energized│  bipolar track, dot = arousal (−1..1)
  │ Mood     low  ──●───── positive │  dot = valence
  │ Stability     ─────────●  steady│  unipolar 0..1
  │ You said 3 · Okay · Words felt mostly neutral │ bodyMedium
  └───────────────────────────────┘

  Compared to your usual              titleMedium, 24 dp above
  ┌───────────────────────────────┐
  │ Tone            A bit darker  │  titleMedium + bodyMedium right (onSurfaceVariant)
  │ ───[░░░░●░]─────               │  deviation bar (see below)
  │ Speech rate     As usual      │
  │ Pitch           A bit lower   │
  │ Pitch variety   Flatter       │
  │ Pauses & breath More pauses   │
  │ Reading accuracy As usual     │
  │ Voice tremor    As usual      │
  └───────────────────────────────┘

  Mindwave is not a medical tool.    labelMedium, centered, onSurfaceVariant
  [ Done ]
```

**Summary sentence** = `{energy word}, {mood word}` + optional ", less steady":
- Energy (arousal a): a < −0.33 "Calm" · −0.33..0.33 "Balanced" · > 0.33 "Energized".
- Mood (valence v): v < −0.5 "low" · −0.5..−0.15 "a little low" · −0.15..0.15 "steady" ·
  0.15..0.5 "a little bright" · > 0.5 "bright".
- Stability s < 0.4 adds ", less steady than usual".

**Deviation bar** (per metric, z-score vs 14-day baseline):
- Track: full width, 6 dp tall, radius 3, `deviationTrack`. Range −2σ..+2σ (clamp beyond).
- Band: centered ±1σ, `deviationBand`, label "usual" not shown (explained in the info sheet).
- Dot: 10 dp, `onSurface`. Center tick 1 dp `outline`.
- Phrase: |z| < 0.5 "As usual"; 0.5–1.5 "A bit {higher/lower}"; > 1.5 "Much {higher/lower}".
  Metric-specific words: Speech rate faster/slower, Pitch variety more varied/flatter,
  Pauses more/fewer pauses, Tone brighter/darker, Tremor steadier/shakier, Accuracy more/fewer slips.
- Tap a row → bottom sheet: metric name, one-line explanation, today vs usual values, 14-day sparkline.
  Explanations:
  - Tone: "The color of your voice — brighter or darker than usual."
  - Speech rate: "How fast you read, in syllables per second."
  - Pitch: "Your average voice pitch."
  - Pitch variety: "How much your pitch moves up and down. Flatter can mean tired or low."
  - Pauses & breath: "How often and how long you paused while reading."
  - Reading accuracy: "How closely you read the sentence as written."
  - Voice tremor: "Small fast wobbles in your voice."

### 4.9 History

Top bar: back arrow + "History". Segmented button: **7 days · 30 days · All** (default 30).

1. **Mood & energy** card (height 200 dp chart):
   - Two lines: valence (`valence` color), arousal (`arousal` color), 2 dp stroke, round joins,
     monotone cubic smoothing. Y range −1..1 with a 1 dp `outlineVariant` zero line. No other grid.
   - Points 4 dp only at existing days; missing days = gap in the line (don't interpolate > 1 day).
   - Legend: two 8 dp dots + labels above the chart ("Mood", "Energy").
   - X ticks: 4–5 dates, `labelSmall`.
2. **Metric** card: horizontally scrollable `FilterChip` row (Tone, Speech rate, Pitch, Pitch variety,
   Pauses, Accuracy, Tremor, Stability, Self rating). One selected. Chart shows the metric's z-score
   with the ±1σ band shaded (`deviationBand`), line in `metric` color. Self rating uses 1–5 axis.
3. **Days** list: each row 56 dp: date (`titleMedium`) · summary (`bodyMedium`) · 10 dp dot in the
   day's done-palette color. Tap → Day detail.

Tap a chart point → vertical 1 dp guide + tooltip ("Oct 28 · Mood +0.3 · Energy −0.1"). Tap tooltip
→ **Day detail** bottom sheet: date, summary, the 3 state bars, self rating, the 7 metric rows
(same component as Result). Sheet uses `surfaceContainerHigh`, top radius 24 dp, drag handle.

Empty: orb idle 120 dp + "Your graph starts after your first check-in." + [Check in now].
Fewer than 3 days: show points, no lines, caption "A few more days and lines will appear."

### 4.10 Settings

Top bar: back + "Settings". Plain list rows (56 dp, `titleMedium` + `bodyMedium` subtitle):
- **Recalibrate voice** — "Read the setup sentence again. Use this if your voice changed (new mic,
  cold, etc.)." → calibration flow; baseline restarts from new calibration but history stays.
- **Delete all data** — text in `error`. Dialog: title "Delete everything?" body
  "All check-ins, your baseline and calibration will be removed from this phone. This can't be
  undone." [Cancel] [Delete] (`error` text). After: return to Welcome.
- Divider, then **About**: "Version {x}", "All analysis happens on this device. No account, no
  server, no internet." and "Mindwave is not a medical tool."

Theme follows the system (no toggle).

---

## 5. The voice orb

The single visual signature. Soft, glossy, organic blob that breathes when idle and moves with the
voice when listening. Reference implementation: `design/compose/VoiceOrb.kt`; the prototype uses the
same math in WebGL so it can be checked by eye.

### 5.1 Inputs (from the audio engine, ~50–100 Hz updates)

| Input | Type | Source |
|---|---|---|
| `level` | Float 0..1 | RMS of the last 20 ms frame → dBFS → `clamp((db + 60) / 50, 0, 1)` (−60 dBFS = 0, −10 dBFS = 1) |
| `pitchHz` | Float, 0 = unvoiced | YIN / autocorrelation, 70–400 Hz |
| `speaking` | Boolean | VAD (level above noise floor + 6 dB for ≥ 100 ms, hang 250 ms) |
| `state` | Idle / Listening / Analyzing / Done / Error | UI state |

The audio thread writes these into a lock-free holder (e.g. `AtomicReference` or `@Volatile` fields).
The orb reads them **once per frame inside the draw phase**, never through recomposition.

### 5.2 Smoothing (per frame, `dt` in seconds)

```
k(tau)   = 1 - exp(-dt / tau)
L       += (level - L) * k(level > L ? 0.06 : 0.25)       // fast attack, slow release
S       += ((speaking ? 1 : 0) - S) * k(0.30)
pn_tgt   = pitchHz > 0 ? clamp(0.5 + 0.5 * log2(pitchHz / basePitchHz), 0, 1) : 0.5
P       += (pn_tgt - P) * k(pitchHz > 0 ? 0.15 : 1.0)     // drifts back to 0.5 when unvoiced
phase   += dt * (0.35 + 1.6 * L)                          // accumulate, never t*speed (no jumps)
```

`basePitchHz` = median pitch from calibration (default 165 Hz before calibration).
So ±1 octave around the user's own pitch maps to 0..1.

### 5.3 Shape (polar blob, signed-distance style)

Coordinates: `p` in −1..1 across the canvas; base radius `R0 = 0.62` (rest is room for the glow).

```
breath = sin(2π · t / 5.0)                                     // 5 s breathing cycle
scale  = 1 + 0.025 · breath · (1 − S) + 0.16 · L              // passed through springOrb
amp    = 0.030 + 0.10 · L + 0.02 · S                           // edge wobble size
w2 = 0.55 − 0.20·P,  w3 = 0.35,  w5 = 0.15 + 0.35·P,  w7 = 0.10 + 0.15·P   // higher pitch = finer ripples

a = atan2(p.y, p.x) + 0.15 · phase                             // slow rotation
R = R0 · scale · (1 + amp · ( w2·sin(2a + 1.0·phase)
                            + w3·sin(3a − 1.3·phase + 1.7)
                            + w5·sin(5a + 1.9·phase + 4.1)
                            + w7·sin(7a − 2.3·phase + 2.3) ))
r = length(p)
alpha = 1 − smoothstep(R − aa, R + aa, r)                      // aa = 1.5 px in p units
```

Integer harmonics keep the shape seamless at ±π. The sum stays well under 1, so the blob never
pinches or self-intersects.

### 5.4 Fill and glow

```
q   = p / R + 0.25 · (sin(2.2·p.y + 0.9·phase), cos(2.0·p.x − 0.7·phase))   // flowing warp
dir = (cos θg, sin θg),   θg = 0.4·phase + (P − 0.5)·0.7                     // pitch tilts the flow
col = mix(c1, c2, smoothstep(−0.8, 0.8, dot(q, dir)))
hl  = 1 − length(p − (−0.30, 0.38)·R) / (0.95·R)                            // soft sheen, top-left
col = mix(col, c3, smoothstep(0.25, 1.0, hl) · (0.55 + 0.35·P))
col *= 0.90 + 0.10·(1 − r/R)                                                 // gentle rim depth
glow = glowA · (0.6 + 0.4·L) · exp(−max(r − R, 0) · 7 / R0)                 // outside only
out.rgb = col·alpha + c1·glow·(1 − alpha)                                    // premultiplied
out.a   = alpha + glow·(1 − alpha)
```

(`p.y` points **up** in this math; flip the y of the screen coordinate.)

### 5.5 States

| State | Palette | Driver override | Size |
|---|---|---|---|
| Idle | Idle | `L = 0`, `S = 0` (only breathing; phase speed 0.35) | 1.0 |
| Listening | Listening | real inputs | 1.0 (+0.16·L) |
| Analyzing | Analyzing | `L = 0.15 + 0.10·sin(2π·t / 1.2)`, `P = 0.5 + 0.2·sin(t)`, flow angle ×2 | 0.85 |
| Done | Done (lerped by valence) | `amp` eases to 0.02 over 800 ms, breathing continues | 1.0 |
| Error / too quiet | Error grey | Idle motion, glow 0.15 | 0.92 |

- Palette and `glowA` changes tween over **300 ms** (`easeStandard`), per color channel.
- Size changes use `springOrb` (damping 0.55, stiffness 180) — slight overshoot feels alive.
- Tap feedback: scale 0.96 on press, spring back.
- Captured pulse: 1.0 → 1.06 → 1.0, 400 ms.
- **Reduced motion** (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`): no rotation, `amp` × 0.5,
  breathing off; color still reacts to level so the user still sees the mic is live.

### 5.6 Rendering and performance

- **API 33+:** one `RuntimeShader` (AGSL) wrapped in `ShaderBrush`, drawn with a single
  `drawRect` in `Modifier.drawWithCache`. Uniforms (`size`, `time`, `phase`, `L`, `S`, `P`,
  `scale`, `amp`, colors, `glowA`) are set inside the draw lambda each frame.
- **API 26–32 fallback:** Canvas `Path` with 96 points sampled from the same `R(a)` formula, joined
  with quadratic midpoints (smooth). Fill with a `Brush.linearGradient(c1, c2)` rotated by `θg`,
  a `radialGradient(c3 → transparent)` sheen at the highlight point, and the glow as a
  `radialGradient(c1·glowA → transparent)` circle drawn first. Reuse one `Path` (`rewind()`).
- **Frame clock:** a single `withFrameNanos` loop in `LaunchedEffect` writes `time` into a
  `mutableFloatStateOf`; only the draw phase reads it → no recomposition per frame, no layout.
- Target 60 fps (120 on high-refresh panels is fine, cost is one full-quad shader at ~320 dp).
  Budget: < 1 ms GPU on mid-range devices. Do not use `Modifier.blur` on the orb (expensive and
  API 31+); the glow is part of the shader / gradient.
- Stop the frame loop when not visible: the loop lives in the composable, so leaving the screen
  stops it; also pause on `Lifecycle.State < STARTED` (`repeatOnLifecycle`).
- Audio analysis runs on its own thread (`AudioRecord`, 16 kHz mono, 20 ms frames). The orb must
  never wait for it.

---

## 6. Other motion

| Element | Motion |
|---|---|
| Step change | Text cross-fade 300 ms + 24 dp slide up of the new sentence; orb stays in place |
| Read highlight | Per word alpha 0.38 → 1.0, 200 ms |
| Countdown ring | Linear sweep with real time; color change at 10 s, 300 ms |
| Mood circles | Selected: spring scale 1.12, color fade 150 ms |
| Result reveal | Rows fade + 8 dp rise, 40 ms stagger, 300 ms each; bar dots slide from center 500 ms |
| Charts | Lines draw left → right 600 ms on first show only |
| Bottom sheet | M3 default |

---

## 7. Empty and error states

Errors during a check-in step replace the hint line + button; the sentence stays visible.
The orb turns to the Error palette. Haptic `Reject` once.

| Case | Detection | UI |
|---|---|---|
| **Mic permission denied** | permission result | Full screen: grey orb 160 dp, `headlineSmall` "Mindwave needs the microphone", `bodyMedium` "It listens only during a check-in, and your voice never leaves this phone." [Allow microphone]. If "don't ask again": button becomes [Open settings] (app details intent). |
| **Too quiet (live)** | Listening, `L < 0.08` for 3 s and not speaking | Hint changes to "I can barely hear you — move a little closer". Orb stays listening. |
| **Too quiet (after)** | Mean speech level < −45 dBFS | `titleMedium` "That was too quiet to analyze." `bodyMedium` "Hold the phone about 20 cm from your mouth." [Try again] |
| **Too short (A/B)** | voiced < 60 % of expected or < 1.5 s | "That was a bit short." "Please read the whole sentence." [Try again] |
| **Too short (C)** | voiced speech < 5 s within the 10–20 s | "Talk a little longer." "Anything works — what you ate, what you saw." [Try again] |
| **Too noisy** | noise floor > −35 dBFS before speaking | "It's noisy around you." "Find a quieter spot and try again." [Try again] · text button "Continue anyway" |
| **No speech** | no VAD hit in 6 s | Hint "Tap the orb when you're ready" and return to Ready (no error colors). |
| **Interrupted** | audio focus loss / call | "Recording stopped." "Let's do this step again." [Try again] |
| **Analysis failed** | exception | Result screen replaced by: "Something went wrong reading today's check-in." [Try again] (re-runs analysis on stored features) · "Your other days are safe." |
| **Empty home** | no check-ins | Home without last-result card and streak. |
| **Empty history** | 0 days | See 4.9. |

All "Try again" actions restart only the current step.

---

## 8. Accessibility

- Contrast ≥ 4.5:1 for text (checked for `onSurfaceVariant` on `background` in both themes).
- Orb has `contentDescription` by state: "Start listening", "Listening, tap to finish",
  "Analyzing", "Done". It is a button (role) on check-in screens.
- The reading sentence is a single text node for TalkBack (highlight is visual only).
- Countdown: announce "10 seconds, you can finish now" once via `LiveRegion.Polite`.
- Every metric phrase is text, color is never the only signal.
- Supports font scale up to 200 %: the sentence wraps; orb shrinks to 200 dp when height < 640 dp.

---

## 9. Logo

Files in `design/logo/` (preview: `design/logo/logo.html`).

- **Mark ("Slices"):** the voice orb (same blob formula as 5.3, phase 0.6, amp 0.07) cut into five slices.
  The cuts bow outward and lean like meridians on a turning sphere; gaps widen toward the poles. The first
  slice is small and set apart, and slice lengths differ like a voice at different loudness. Corner radius
  20 (of 512).
- **Color:** linear gradient bottom-left → top-right `#3554FF` → `#8366FF` → `#DB86F2`, plus a cyan
  `#6FE3FF` sheen at 60% from the top-left. Always on white.
- **App icon:** mark on white `#FFFFFF`, inside the 66 dp safe zone. Android adaptive icon XML in
  `design/logo/android/` → copy `ic_launcher_*.xml` to `res/drawable/` and `ic_launcher.xml` to
  `res/mipmap-anydpi-v26/` (also as `ic_launcher_round.xml`). Includes a `monochrome` layer for Android 13
  themed icons.
- **Mono mark** (`mark-mono.svg`): the slices as one color; use below 24 px.
- **Lockup** (`lockup.svg`): mark + lowercase "mindwave", Roboto Regular, tracking −1.
  The text is live text; outline it before using outside the app.
- Clear space = half the mark width. Never rotate, outline, add shadows or change the slice shapes.
- Other explored directions are kept for reference in `design/logo/concepts.html`.
