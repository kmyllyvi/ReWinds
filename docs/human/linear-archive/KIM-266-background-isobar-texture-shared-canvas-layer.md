# KIM-266: Background isobar texture — shared Canvas layer

**Status:** Done · **Priority:** High · **Labels:** design, spec-ready, Feature
**Created:** 2026-06-04T12:09:37.828Z · **Completed:** 2026-06-05T14:16:04.316Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-266/background-isobar-texture-shared-canvas-layer

## Description

## Spec

Implement the isobar spiral as a reusable full-screen background composable that any screen can drop in. The spiral is rendered very large so its centre/eye is off-screen top-right; only the outer flowing curves are visible, giving an abstract instrument-panel feel without showing a recognisable logo. Stroke colour `#8ecff0` at 6 % opacity. This composable is a pure UI element with no ViewModel.

## Acceptance criteria

- [ ] A `IsobarBackground` composable exists in `commonMain` and can be placed as the bottom layer of any screen
- [ ] The spiral centre is positioned off-screen top-right (matching the design: `top: -480px, right: -415px` at 980×980 equivalent scale)
- [ ] Stroke colour is `#8ecff0` at exactly 6 % opacity (alpha ≈ 0.06)
- [ ] The composable renders using `Canvas` (Compose); no external SVG asset file is required (paths are drawn in code), OR an SVG asset is loaded via a KMP-safe mechanism — either approach is acceptable, but the choice must be noted in the PR
- [ ] The composable is stateless and accepts no parameters other than an optional `Modifier`
- [ ] Placing `IsobarBackground` behind content does not clip or obscure any interactive element
- [ ] Composable is used on at least the Home/Places screen as a smoke test (remaining screens applied in their own redesign tickets)

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`); new logic has basic coverage
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations
- [ ] Token colour references theme system (blocked on Design token ticket); hex literal `#8ecff0` must not be hard-coded — use `accentBlue` token at 0.06 alpha

## Notes

Priority: High — dependency on design token ticket; all screen redesigns benefit from it.
Proposed size: Small–Medium (1–3 hours depending on SVG-vs-Canvas approach).
Reviewers needed: code-reviewer, ux-ui-reviewer.
Assumption: the spiral path is derived from `docs/designs/screens-v1.html` (inline SVG). Developer should extract the path data from that file rather than redrawing from scratch.
Blocked by: Design token / theme system ticket.

## Comments

_No comments._
