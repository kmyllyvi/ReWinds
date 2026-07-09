# KIM-328: Monthly summary: highlight all qualifying days, not just the top day

**Status:** Done · **Priority:** No priority · **Labels:** Bug
**Created:** 2026-07-01T06:57:33.069Z · **Completed:** 2026-07-01T07:28:57.810Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-328/monthly-summary-highlight-all-qualifying-days-not-just-the-top-day

## Description

On the monthly summary, the daily chart currently highlights only the single top day.

**Expected:** Highlight **all** days that exceed the preferred-day criteria, not just the best one.

*Source: voice memo bug report ("Rewinds, highlighted days").*

## Comments

### kimmo.myllyviita@gmail.com — 2026-07-01T07:29:08.512Z

**Marcy — code review: PASS** (PR #61)

Expected behaviour met: daily wind chart now highlights every day matching the preferred-day (days-of-interest) criteria, not just the peak. Derivation is `qualifyingDayIndices` over `DayWeatherSummary.isMatch` — the same `filter.matches(...)` source already driving the days-of-interest count and the `DaySummaryRow` highlight, so chart/rows/count stay consistent. MV* clean (logic in VM, chart is pure render, view only collects+passes). Tests swapped from peak-index to qualifying-index (empty / none / several / all). No stale `peak*` references remain. Build + testDebugUnitTest green.

Minor (non-blocking, pre-existing convention): the VM-constructor DB dependency means the test mirrors `qualifyingDayIndices` rather than calling it, so a mirror/real divergence wouldn't be caught. Consistent with the existing pattern in this file — noted, not a change request.

Completing.

### kimmo.myllyviita@gmail.com — 2026-07-01T07:22:16.275Z

**Dev handoff (Randy)**

- **Branch:** `kimmomyllyviita/kim-328-highlight-all-qualifying-days`
- **PR:** https://github.com/kmyllyvi/ReWinds/pull/61
- **Build:** pass (`./gradlew buildAndroidOnly`)
- **Tests:** pass (`./gradlew :composeApp:testDebugUnitTest`)
- **New tests written:** yes — replaced the peak-index mirror tests in `MonthlyStatisticsViewModelTest` with `qualifyingDayIndices` tests (empty, none-match, several-match, all-match).

**Summary:** The daily wind chart highlighted only the single peak day. It now highlights every day meeting the preferred-day criteria. Replaced `peakSustainedWindIndex` / `peakWindDayIndex` with `qualifyingDayIndices` / `highlightedDayIndices` (`Set<Int>`), derived from `DayWeatherSummary.isMatch` (already computed by the VM via the active filter). `DailyWindBarChart` now takes `highlightedIndices: Set<Int>` and colours every matching bar; no selection logic in the composable.

**Deviations / notes:** Highlighting is now driven by the same `isMatch` flag that tints the matching day rows, so chart and list agree. Old single-peak behaviour fully removed per the ticket. Issue was in Backlog (no `spec-ready`) but explicitly dispatched by Kimmo; moving to In Progress + in-review.

