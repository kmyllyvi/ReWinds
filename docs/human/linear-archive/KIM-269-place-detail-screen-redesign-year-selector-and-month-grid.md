# KIM-269: Place Detail screen redesign — year selector and month grid

**Status:** Done · **Priority:** Medium · **Labels:** design, in-review, spec-ready, Feature
**Created:** 2026-06-04T12:10:30.257Z · **Completed:** 2026-06-06T08:09:49.916Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-269/place-detail-screen-redesign-year-selector-and-month-grid
**Related:** related: KIM-270 — Month Summary screen redesign — stat cards and daily bar chart; related: KIM-267 — Tab bar navigation — Places / Chat / Settings; related: KIM-266 — Background isobar texture — shared Canvas layer; related: KIM-265 — Design token / theme system — Midnight Blue palette

## Description

## Spec

Redesign the Place Detail screen with the Midnight Blue theme: a back-navigation header with a Chat shortcut button, a horizontal year-selector strip, and a 2-column month grid where each cell shows one of three states — full data (solid surface card), partial data (dimmed), or no data (empty/outline only). The isobar texture is applied as the background layer.

## Acceptance criteria

- [ ] Screen background is `pageBg` with isobar texture as the lowest layer
- [ ] Header shows a back arrow (left), place name in `textPrimary` bold, and a "Chat" button (right) in `accentBlue`
- [ ] A horizontal scrollable year-selector strip is shown below the header; the selected year is highlighted with `accentBlue` text and an underline or pill indicator; non-selected years use `textSecondary`
- [ ] Selected year and scroll position are tracked in the ViewModel (not `remember` state in the composable)
- [ ] The month grid has exactly 2 columns and up to 12 cells (one per month)
- [ ] Each month cell shows three distinct visual states: (a) full data — `surface` card background, month name in `textPrimary`, day count in `textSecondary`; (b) partial data — same card but at reduced opacity or with a "partial" label; (c) no data — outline card only, month name in `textTertiary`
- [ ] Which state each cell renders is determined by data exposed from the ViewModel, not computed inside the composable
- [ ] Tapping a month cell with data navigates to the Month Summary screen
- [ ] The Chat button in the header opens the Chat tab pre-filtered to this place (or logs a TODO comment if deep-link is deferred)

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`); ViewModel logic for month-state mapping has unit test coverage
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations
- [ ] All colour values reference theme tokens
- [ ] Blocked by: [KIM-265](https://linear.app/kimmo-m/issue/KIM-265/design-token-theme-system-midnight-blue-palette) (tokens), [KIM-266](https://linear.app/kimmo-m/issue/KIM-266/background-isobar-texture-shared-canvas-layer) (isobar), [KIM-267](https://linear.app/kimmo-m/issue/KIM-267/tab-bar-navigation-places-chat-settings) (tab nav shell for back-nav)

## Notes

Priority: Medium — depends on foundation tickets; no dependency on Home screen ticket.
Proposed size: Medium (2–3 hours). Month-state ViewModel logic warrants qa-test-agent attention.
Reviewers needed: code-reviewer, ux-ui-reviewer, qa-test-agent.
Reference: `docs/designs/screens-v1.html` (second mockup, "Place Detail").
Assumption: "partial data" definition (e.g. fewer than N days) is a product decision not yet spec'd — developer should use a placeholder threshold and flag it for Kimmo.

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-05T15:22:16.653Z

Implemented and ready for review.

**Branch:** `kimmomyllyviita/kim-269-place-detail-screen-redesign-year-selector-and-month-grid`
**PR:** https://github.com/kmyllyvi/ReWinds/pull/15 (base: `develop`)
**Build:** Kotlin compilation passes. `./gradlew buildAndroidOnly` itself fails on a pre-existing `Task.project`-at-execution-time config-cache issue (plus a lint-config issue) — confirmed identical failure on a clean tree, so it is not introduced by this change.
**Tests:** Pass — full `:composeApp:testDebugUnitTest` suite green, including the new `PlaceSummaryMonthCellStateTest`.

**Summary**
- Place Detail screen redesigned with the Midnight Blue theme. Root `Box` applies `pageBg` + isobar texture as the lowest layer (the white-background miss from KIM-270 explicitly avoided).
- Header: back arrow + bold place name in `textPrimary`, plus an `accentBlue` "Chat" text button.
- Year-selector strip: horizontal scrollable; selected year in `accentBlue` bold with a pill underline, others in `textSecondary`. Selected year and the strip scroll offset are tracked in the ViewModel.
- Month grid: exactly 2 columns, 12 cells, three states driven by `MonthCellInfo`/`MonthCellState` from the ViewModel — FULL (solid `surface` card), PARTIAL (`surface` @ 0.5 alpha + "Partial" label), NO_DATA (border-only card, `textTertiary`). Tap-with-data navigates to Month Summary; empty cell opens the download dialog.
- Month-state mapping extracted to pure ViewModel companion functions and unit-tested directly. All colours reference theme tokens.

**Deviations / things needing your call**
- **Partial threshold:** `PARTIAL_DAY_THRESHOLD = 20` is a placeholder (a month with ≥20 stored days but not the whole month renders FULL; fewer renders PARTIAL). Flagged with `TODO(Kimmo)` — please confirm the value/rule.
- **Chat deep-link:** the Chat tab has no place pre-filter yet, so the header Chat button navigates to Chat with a seeded "Chat about <place>" message and a `TODO` comment, as the acceptance criteria's fallback allows.

