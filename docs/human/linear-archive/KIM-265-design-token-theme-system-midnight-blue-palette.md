# KIM-265: Design token / theme system — Midnight Blue palette

**Status:** Done · **Priority:** High · **Labels:** design, spec-ready, Feature
**Created:** 2026-06-04T12:09:23.919Z · **Completed:** 2026-06-05T14:16:07.032Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-265/design-token-theme-system-midnight-blue-palette

## Description

## Spec

Introduce a centralised Compose `MaterialTheme`-compatible colour scheme for the new Midnight Blue palette, replacing the current light/lime-green theme. All downstream screen redesigns will reference these tokens rather than hard-coded hex values. This ticket is the prerequisite for every other redesign ticket; no screen work can start until the tokens exist.

## Acceptance criteria

- [ ] A `ReWindsTheme` composable wraps the app in `commonMain`; it accepts no colour parameters (the dark scheme is the only scheme for now)
- [ ] The following semantic colour tokens are defined and accessible via `MaterialTheme.colorScheme` or a custom `ReWindsColors` object: `pageBg` (`#030810`), `surface` (`#0d1b2e`), `surfaceRaised` (`#19304d`), `border` (`#1a3050`), `textPrimary` (`#ddeef8`), `textSecondary` (`#7ab8d8`), `textTertiary` (`#4a7a9b`), `accentBlue` (`#8ecff0`), `accentBluePressed` (`#5bb3e0`), `attention` (`#e8a030`), `error` (`#d95060`)
- [ ] No screen or composable outside the theme file contains any of the above hex literals
- [ ] The existing app still compiles and renders after the theme is applied (no visual regression on screens not yet redesigned is required, but a crash-free build is)
- [ ] A unit test or compile-time assertion confirms the token object is non-null at runtime

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`); new logic has basic coverage
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations
- [ ] Theme file lives in `commonMain` so it is available to both platforms
- [ ] All hex values sourced from design file `docs/designs/color-themes.html`

## Notes

Priority: High — all screen redesign tickets are blocked on this.
Proposed size: Small (1–2 hours). Single file, no logic, no ViewModel changes needed.
Reviewers needed: code-reviewer, ux-ui-reviewer (token naming and completeness).
Assumption: one dark scheme only for now; light mode is out of scope per the design session.

## Comments

_No comments._
