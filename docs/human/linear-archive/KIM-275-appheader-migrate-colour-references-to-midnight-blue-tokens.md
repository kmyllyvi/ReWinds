# KIM-275: AppHeader — migrate colour references to Midnight Blue tokens

**Status:** Done · **Priority:** Medium · **Labels:** design, spec-ready, Feature
**Created:** 2026-06-05T16:54:57.039Z · **Completed:** 2026-06-09T13:18:11.691Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-275/appheader-migrate-colour-references-to-midnight-blue-tokens
**Related:** related: KIM-265 — Design token / theme system — Midnight Blue palette

## Description

## Spec

`AppHeader` is the shared composable rendered on every screen (Home, Place Detail, Month Summary, Settings, and any future screen). It currently resolves four colour references through the M3 `MaterialTheme.colorScheme` mapping layer rather than directly from the Midnight Blue design tokens (`MaterialTheme.rewinds.*`). This is a pure token-swap: no layout changes, no new parameters, no behaviour changes. The fix closes the risk that the M3 mapping layer drifts out of sync with the Midnight Blue palette as the design system evolves.

[KIM-265](https://linear.app/kimmo-m/issue/KIM-265/design-token-theme-system-midnight-blue-palette) (design token foundation) is already merged, so this issue is unblocked.

Reference file: `composeApp/src/commonMain/kotlin/components/AppHeader.kt`

## Acceptance criteria

- [ ] Back arrow `Icon` tint is `MaterialTheme.rewinds.textPrimary`; `MaterialTheme.colorScheme.onBackground` is no longer used for this tint
- [ ] Logo `Icon` tint is `MaterialTheme.rewinds.accentBlue`; `MaterialTheme.colorScheme.primary` is no longer used for this tint
- [ ] Title `Text` colour is `MaterialTheme.rewinds.textPrimary`; `MaterialTheme.colorScheme.onBackground` is no longer used for this colour
- [ ] Fade divider gradient start colour is `MaterialTheme.rewinds.border` (with `alpha = 0.2f`); `MaterialTheme.colorScheme.outlineVariant` is no longer referenced in this gradient
- [ ] Fade divider gradient end colour is `Color.Transparent`; `MaterialTheme.colorScheme.surface.copy(alpha = 0f)` is no longer used
- [ ] No hardcoded hex colour values are introduced anywhere in `AppHeader.kt`
- [ ] No other structural changes are made (parameters, layout, slot API, sizing, padding all stay identical)

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md — not applicable here as AppHeader is a pure composable with no state logic, but confirm no state or logic is added)
- [ ] No new lint violations
- [ ] Each of the five old token references (`colorScheme.onBackground` ×2, `colorScheme.primary`, `colorScheme.outlineVariant`, `colorScheme.surface`) is absent from the file after the change (grep check)

## Notes

Priority: Medium — affects visual correctness on every screen; low effort, no risk.
Proposed size: Small (< 1 hour, single-file token swap).
Blocked by: [KIM-265](https://linear.app/kimmo-m/issue/KIM-265/design-token-theme-system-midnight-blue-palette) (Midnight Blue design tokens) — already completed, so this issue is immediately actionable.
Assumption: `MaterialTheme.rewinds` extension is available in `commonMain` after [KIM-265](https://linear.app/kimmo-m/issue/KIM-265/design-token-theme-system-midnight-blue-palette); if the extension property path differs, the developer should use whatever the canonical accessor is from that implementation.
Reviewers needed: code-reviewer, ux-ui-reviewer (touches Compose UI on every screen).

## Comments

_No comments._
