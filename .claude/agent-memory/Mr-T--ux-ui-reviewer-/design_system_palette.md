---
name: design-system-palette
description: ReWinds Midnight Blue dark theme — full colour palette, token names, and file location
metadata:
  type: project
---

ReWinds uses a single dark-only theme called "Midnight Blue". All tokens live in `composeApp/src/commonMain/kotlin/ui/theme/ReWindsTheme.kt` under `object ReWindsColors`.

| Token | Hex | Role |
|---|---|---|
| pageBg | #030810 | Page / screen background |
| surface | #0D1B2E | Cards, panels, sheets |
| surfaceRaised | #19304D | Elevated surfaces |
| border | #1A3050 | Dividers, borders |
| textPrimary | #DDEEF8 | Main readable text |
| textSecondary | #7AB8D8 | Secondary labels |
| textTertiary | #4A7A9B | Hints, empty state, icons |
| accentBlue | #8ECFF0 | Primary accent, CTAs |
| accentBluePressed | #5BB3E0 | Pressed state of accent |
| attention | #E8A030 | Warnings, amber highlights |
| error | #D95060 | Errors |
| attentionContainer | #1FE8A030 | Attention bg at ~12% opacity |
| errorContainer | #1FD95060 | Error bg at ~12% opacity |

Theme is applied via `ReWindsTheme {}` composable. MaterialTheme slots are mapped. Extended tokens accessible via `MaterialTheme.rewinds`.

**How to apply:** Use these tokens in any design review or design guidance — never hard-code hex values in recommendations.
