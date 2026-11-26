# Sukoon design system: "Midnight Ink & Gold"

Calm editorial minimalism: it should read like a beautifully typeset prayer book. Arabic is the hero element. The only ornament is a faint eight-point star (khatam) lattice, used behind headers, empty states and the counter, never as wallpaper.

Code: `app/src/main/java/app/sukoon/core/designsystem/theme/` (`Color.kt`, `Type.kt`, `Tokens.kt`, `Theme.kt`).

## Colour

| Token | Light | Dark | AMOLED |
|---|---|---|---|
| background / surface | Parchment100 `#F6F0E3` | Ink900 `#0E1320` | `#000000` |
| surfaceContainer | Parchment50 `#FBF7EE` | Ink850 `#131A2A` | `#07090E` |
| surfaceContainerHigh | Parchment200 `#EFE6D3` | Ink800 `#182033` | `#10141E` |
| onSurface | Umber `#1B1A17` | Cream `#ECE6D8` | Cream |
| onSurfaceVariant | UmberMuted `#5F5849` | CreamMuted `#A8A496` | CreamMuted |
| primary (Gold accent) | `#8A6A2E` | `#D4AF6A` | `#D4AF6A` |
| secondary (Sage) | `#4F6B4C` | `#9DB89A` | `#9DB89A` |
| outline | `#D6CAB0` | `#2E3850` | `#2E3850` |

Accent picker: Gold (default), Sage, Rose, Sky, Terracotta, each with a light and a dark tone chosen for AA contrast on its surface. Dynamic colour (Android 12+) is opt-in; AMOLED forces pure black even with dynamic colour on.

Section tints (card wash at 8–10% alpha, icon at full): Tranquility `#7FA7C9`, Solace `#B39DDB`, Success `#D4AF6A`, Knowledge `#8DB596`, Love `#E09A9A`, Forgiveness `#8FC1C1`, Protection `#9FA8C9`, Health `#A9C98A`, Sustenance `#D9B27C`, Straight Path `#C9B48F`.

## Type

| Role | Font | Size / line |
|---|---|---|
| display L/M/S | Fraunces Regular | 52/60 · 42/50 · 34/42 |
| headline L/M/S, title L | Fraunces SemiBold | 30/38 · 26/34 · 22/30 · 20/28 |
| title M/S, body, label | Inter | 16/24 … 11/16 |
| Arabic (reading) | Amiri Quran (default) or Amiri | 28sp × user scale, line height 2.1em, RTL, `ar` locale |
| Transliteration | Inter Italic | 15sp × scale, 1.6em |
| Translation | Fraunces | 18sp × scale, 1.55em |
| Eyebrow | Inter SemiBold, +1.6 tracking, uppercase | 11sp |

All sizes are `sp`; reading scales run 80–200% on top of system font scale.

## Spacing, shape, elevation

- Spacing (4/8 grid): 2 · 4 · 8 · 12 · 16 · 24 · 32 · 48 · 64; page gutter 20dp; reading max width 640dp.
- Radii: 6 · 10 · 16 · 24 · 32 · pill. Cards use 24, the daily card and section hero 32.
- Elevation: 0 · 1 · 3 · 6 · 10dp. Surfaces separate by tone, not shadow.

## Motion

- Springs: gentle (0.85 damping, medium-low stiffness) for progress, bouncy for the favourite heart and counter pulse.
- Shared elements: section card → section hero, dua row/daily card → reader title, home search pill → search field.
- Reader content fades and expands in sequence (Arabic 0ms, transliteration 120ms, translation 220ms).
- Haptics: page change tick, favourite confirm, every counter tap (user strength), waveform on round completion, error buzz on a wrong PIN.
- Predictive back enabled; edge-to-edge everywhere with inset-aware padding.

## Iconography

Material Symbols (outlined) for UI; custom vectors for the launcher (adaptive + monochrome themed icon), splash, notification and onboarding art, all built from the same eight-point star and crescent.
