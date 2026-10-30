# Mindwave design tokens

Every token maps 1:1 to a Compose `MaterialTheme` slot. A ready-made version is in
`design/compose/Theme.kt`. If a value is not listed, use the Material 3 default.

## 1. Color

Calm, near-neutral surfaces. Color is reserved for the orb and for data.
No dynamic color (Material You): the orb palette must stay stable across devices.

### 1.1 `ColorScheme` (light / dark)

| M3 slot | Light | Dark | Use |
|---|---|---|---|
| `background` | `#F6F5F2` | `#0D0D10` | Screen background |
| `onBackground` | `#16161A` | `#ECECEF` | Main text |
| `surface` | `#F6F5F2` | `#0D0D10` | Same as background (flat UI) |
| `onSurface` | `#16161A` | `#ECECEF` | Main text |
| `surfaceVariant` | `#ECEBE7` | `#1B1B20` | Chips, track of bars |
| `onSurfaceVariant` | `#6A6A72` | `#9A9AA3` | Secondary text, captions |
| `surfaceContainerLowest` | `#FFFFFF` | `#0A0A0D` | — |
| `surfaceContainerLow` | `#FBFAF8` | `#121216` | — |
| `surfaceContainer` | `#FFFFFF` | `#16161B` | Cards |
| `surfaceContainerHigh` | `#F0EFEB` | `#1D1D23` | Bottom sheet, dialogs |
| `surfaceContainerHighest` | `#E8E7E2` | `#25252C` | Pressed card |
| `primary` | `#3D4BE0` | `#9AA5FF` | Main button, active chip, links |
| `onPrimary` | `#FFFFFF` | `#10166B` | Text on primary |
| `primaryContainer` | `#E3E6FF` | `#262E86` | Streak pill, selected chip |
| `onPrimaryContainer` | `#121A6B` | `#E0E3FF` | — |
| `secondary` | `#5E5C70` | `#C4C2D6` | Rarely used |
| `onSecondary` | `#FFFFFF` | `#2E2D3D` | — |
| `secondaryContainer` | `#E7E5F2` | `#2E2D3D` | Mood-rating unselected |
| `onSecondaryContainer` | `#1C1B29` | `#E7E5F2` | — |
| `tertiary` | `#D9653A` | `#FFB08F` | Warm accent (valence) |
| `onTertiary` | `#FFFFFF` | `#4A1A06` | — |
| `error` | `#BA3A26` | `#FF8A75` | Errors |
| `onError` | `#FFFFFF` | `#4A0D04` | — |
| `errorContainer` | `#FBE3DE` | `#5C1A10` | Error banner |
| `onErrorContainer` | `#4A0D04` | `#FFDAD3` | — |
| `outline` | `#D3D2CC` | `#36363E` | Card border, dividers |
| `outlineVariant` | `#E6E5E0` | `#24242B` | Hairlines, chart grid |
| `scrim` | `#000000` | `#000000` | Sheet scrim (32% alpha) |

### 1.2 Extended colors (`MindwaveColors`, via `CompositionLocal`)

Data colors (same hue in both themes, dark is lighter):

| Token | Light | Dark | Use |
|---|---|---|---|
| `valence` | `#E2683F` | `#FF9C78` | Valence line/dot |
| `arousal` | `#4F5FF0` | `#8F9BFF` | Arousal line/dot |
| `stability` | `#23977C` | `#5FD3B4` | Stability bar |
| `metric` | `#16161A` | `#ECECEF` | Selected metric line |
| `deviationTrack` | `#ECEBE7` | `#1F1F25` | Deviation bar track |
| `deviationBand` | `#DCDBD5` | `#2C2C34` | ±1σ "your usual" band |

### 1.3 Orb gradients

Each state has 3 colors: `c1` (body), `c2` (flow), `c3` (highlight), plus `glow` alpha.
Values are the same in both themes; in light theme multiply glow alpha by 0.7.

| State | c1 | c2 | c3 | glow |
|---|---|---|---|---|
| `Idle` | `#A9B8FF` | `#D3C6FF` | `#F4F2FF` | 0.25 |
| `Listening` | `#4F6BFF` | `#9B7BFF` | `#6FE3FF` | 0.45 |
| `Analyzing` | `#7C5CFF` | `#FF6FB5` | `#FFC48A` | 0.40 |
| `Done · low valence` | `#5E86D9` | `#8FB3FF` | `#D6E2FF` | 0.30 |
| `Done · neutral` | `#3FB59A` | `#9BE3C9` | `#E6F8F0` | 0.30 |
| `Done · high valence` | `#FF8E5E` | `#FF7FA0` | `#FFE0B0` | 0.30 |
| `Error / too quiet` | `#9A9AA3` | `#C4C2D6` | `#ECECEF` | 0.15 |

`Done` interpolates between the three done palettes by valence in −1..1 (OKLab or plain sRGB lerp is fine).

## 2. Typography (`Typography`)

Font: `FontFamily.Default` (Roboto on Android). No bundled font, keeps the APK small and offline.
Only the slots below are used; leave others at M3 default.

| M3 slot | Size / line | Weight | Letter spacing | Use |
|---|---|---|---|---|
| `displayMedium` | 44 / 52 sp | Light (300) | −0.5 sp | Big numbers (streak on onboarding end) |
| `headlineLarge` | 30 / 38 sp | Normal (400) | −0.4 sp | Screen title ("Good evening") |
| `headlineMedium` | 24 / 34 sp | Normal (400) | −0.2 sp | **Reading sentence** (centered) |
| `headlineSmall` | 20 / 28 sp | Normal (400) | 0 | Result summary line |
| `titleLarge` | 18 / 26 sp | Medium (500) | 0 | Card titles |
| `titleMedium` | 16 / 24 sp | Medium (500) | 0.1 sp | List row titles |
| `bodyLarge` | 16 / 24 sp | Normal (400) | 0.1 sp | Body text |
| `bodyMedium` | 14 / 20 sp | Normal (400) | 0.2 sp | Secondary text |
| `labelLarge` | 14 / 20 sp | Medium (500) | 0.1 sp | Buttons |
| `labelMedium` | 12 / 16 sp | Medium (500) | 0.5 sp | Step label, chip, axis label (ALL CAPS not used) |
| `labelSmall` | 11 / 16 sp | Medium (500) | 0.5 sp | Chart ticks |

Numbers in charts and timers: `fontFeatureSettings = "tnum"` (tabular figures).

## 3. Spacing (`Dimens` object)

4 dp grid.

| Token | dp | Use |
|---|---|---|
| `xxs` | 4 | Icon-to-text gap |
| `xs` | 8 | Inside chips, between label and value |
| `s` | 12 | Between list rows |
| `m` | 16 | Card inner padding |
| `l` | 24 | **Screen side padding**, between sections |
| `xl` | 32 | Above/below orb block |
| `xxl` | 48 | Top of check-in screens |
| `xxxl` | 64 | Bottom safe area above primary button |

Fixed sizes:

| Token | dp |
|---|---|
| `orbSize` (check-in) | 240 |
| `orbSizeSmall` (home, result header) | 120 |
| `orbCanvasPadding` (room for glow) | 40 each side |
| `countdownRingGap` (orb edge → ring) | 20 |
| `countdownRingStroke` | 2 |
| `buttonHeight` | 56 |
| `minTouch` | 48 |
| `progressDotSize` | 6 |

## 4. Shapes (`Shapes`)

| M3 slot | Radius | Use |
|---|---|---|
| `extraSmall` | 6 dp | Tooltip |
| `small` | 10 dp | Chips |
| `medium` | 16 dp | Cards |
| `large` | 24 dp | Bottom sheet top corners |
| `extraLarge` | 32 dp | Dialog |
| Buttons | `CircleShape` (pill) | All buttons |

## 5. Elevation

Flat design. All `tonalElevation` and `shadowElevation` = **0 dp**, except:

| Element | Tonal | Shadow |
|---|---|---|
| Bottom sheet | 0 (uses `surfaceContainerHigh`) | 0 |
| Dialog | 0 (uses `surfaceContainerHigh`) | 0 |
| Snackbar | M3 default | M3 default |

Separation is done with color (`surfaceContainer` card on `background`) and a 1 dp `outlineVariant` border in light theme only.

## 6. Motion tokens

| Token | Value | Use |
|---|---|---|
| `durShort` | 150 ms | Press, chip toggle |
| `durMedium` | 300 ms | Fade, orb state color change |
| `durLong` | 500 ms | Screen transitions, orb resize |
| `easeStandard` | `CubicBezierEasing(0.2f, 0f, 0f, 1f)` | Most motion (M3 emphasized) |
| `easeExit` | `CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)` | Leaving screen |
| `springOrb` | `spring(dampingRatio = 0.55f, stiffness = 180f)` | Orb scale |
| Breath period | 5000 ms | Orb idle breathing |
