# KIM-332: Place summary: month cells show no indicator during on-demand month download

**Status:** Done · **Priority:** No priority · **Labels:** Bug
**Created:** 2026-07-01T09:16:41.478Z · **Completed:** 2026-07-01T09:34:01.397Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-332/place-summary-month-cells-show-no-indicator-during-on-demand-month
**Related:** related: KIM-327 — Month card missing loading state

## Description

Follow-up to [KIM-327](https://linear.app/kimmo-m/issue/KIM-327/month-card-missing-loading-state) (the "month card loading state" memo). [KIM-327](https://linear.app/kimmo-m/issue/KIM-327/month-card-missing-loading-state) / PR #60-62 added skeleton **stat cards** on the monthly summary. Kimmo confirmed the memo *also* meant the **place-summary month grid cells** (`PlaceSummaryView.MonthCell`).

**Problem:** `isDownloadingMonth` is tracked in state but never rendered, so when a month is downloaded on demand the cell shows no per-cell loading indicator.

**Expected:** Render a per-cell loading indicator on `MonthCell` while that month is downloading (driven by the existing `isDownloadingMonth` state).

**Files (per Randy's investigation):** `composeApp/src/commonMain/kotlin/.../place/PlaceSummaryView.kt` (`MonthCell`), plus wherever `isDownloadingMonth` is set.

*Source: voice memo "Loading state" — second interpretation, confirmed by Kimmo.*

## Comments

### kimmo.myllyviita@gmail.com — 2026-07-01T09:34:09.547Z

Reviewed PR #63 against expected behaviour, MV*, and KMP rules — all met, no blocking issues. Per-cell indicator renders on the cell where `cell.month == downloadingMonth`; state lives in the ViewModel (View is pure render); clear-on-reload verified (`loadWeatherData()` emits a fresh Success). New ViewModel tests use an in-memory fake covering null-at-rest, in-flight, cleared-after-reload, and only-requested-month. Moving to Completed. (Model-family blind-spot caveat: this is a small, well-scoped UI-state change — no deep design risk.)

### kimmo.myllyviita@gmail.com — 2026-07-01T09:31:57.783Z

**Dev handoff → review**

- **Branch:** `kimmomyllyviita/kim-332-month-cell-download-indicator`
- **PR:** https://github.com/kmyllyvi/ReWinds/pull/63
- **Build:** pass (`./gradlew buildAndroidOnly`)
- **Tests:** pass (`./gradlew :composeApp:testDebugUnitTest`)
- **New tests written:** yes — `composeApp/src/commonTest/kotlin/place/PlaceSummaryViewModelDownloadTest.kt`
- **CI:** green (Build Android, unit tests, coverage floor, diff-cover patch gate all pass)

**Summary:** Replaced `isDownloadingMonth: Boolean` on `WeatherSummaryUiState.Success` with `downloadingMonth: Int?` (the month number being fetched). `PlaceSummaryView.MonthCell` now renders a small `CircularProgressIndicator` + the shared `strings.downloading` label while its month downloads on demand, and ignores taps to avoid re-triggering. State derivation stays in the ViewModel; the cell is a pure render of an `isDownloading` flag (MV*). Reuses `MaterialTheme.rewinds.*` tokens and the existing localized "Downloading…" string (EN + DE). Indicator clears when `loadWeatherData()` emits a fresh `Success`.

**Deviations:** None. (This ticket was in Backlog; Kimmo dispatched it directly with full context, so it was worked ahead of the usual Gate 1 — flagging for transparency.)

