# UI Theme Module

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. -->

**Last updated:** 2026-06-09 (bootstrap)
**Status:** Active

---

## Purpose

Provides the single dark-only Midnight Blue design system for ReWinds. Wraps Compose Material 3 with a custom colour scheme and exposes extended semantic tokens via a `CompositionLocal`.

---

## Responsibilities

- Defining the `ReWindsColors` object with all palette tokens.
- Mapping palette tokens to Material 3 `darkColorScheme` semantic slots.
- Exposing extended tokens (e.g. `attention`, `error`, `accentBluePressed`) via `MaterialTheme.rewinds`.
- Providing the `ReWindsTheme` root composable which all screens must be wrapped in.
- Defining the `IsobarBackground` and `IsobarLogo` components from the design system.

---

## Key interfaces

### ReWindsColors (object)
| Token | Value | Usage |
|---|---|---|
| `pageBg` | `#030810` | App/screen background |
| `surface` | `#0D1B2E` | Cards, sheets |
| `surfaceRaised` | `#19304D` | Elevated surface |
| `border` | `#1A3050` | Dividers, outlines |
| `textPrimary` | `#DDEEF8` | Main text |
| `textSecondary` | `#7AB8D8` | Subtitles |
| `textTertiary` | `#4A7A9B` | Inactive tab labels, hints |
| `textMuted` | `#5A90B0` | Map sheet subtitle |
| `accentBlue` | `#8ECFF0` | Active tab, primary action |
| `accentBluePressed` | `#5BB3E0` | Pressed state |
| `attention` | `#E8A030` | Warning banners |
| `error` | `#D95060` | Error state |
| `attentionContainer` | attention @ 12 % opacity | Warning banner background |
| `errorContainer` | error @ 12 % opacity | Error banner background |

### ReWindsTheme composable
```kotlin
@Composable
fun ReWindsTheme(content: @Composable () -> Unit)
```
No parameters accepted — single dark scheme only.

### MaterialTheme.rewinds extension
```kotlin
val MaterialTheme.rewinds: ReWindsColors
```
Provides access to extended tokens from any composable inside `ReWindsTheme`.

---

## Known constraints

- Light mode is not supported. The app forces dark mode on iOS via `.preferredColorScheme(.dark)` in `iOSApp.swift`.
- All colour values are sourced from the design spec file `docs/designs/color-themes.html` (Design System v1). Changes to palette must be coordinated with that file.
- Do not apply `.background()` at page or header level — it creates visible box artefacts against the Midnight Blue background. Only use `.background()` on self-contained boxed components (e.g. `ChatInputArea`).

---

## Decisions log

- See `/docs/agent/decisions/` for records created after the bootstrap phase.
