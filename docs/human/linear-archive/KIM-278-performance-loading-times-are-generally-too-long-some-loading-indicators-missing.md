# KIM-278: performance: Loading times are generally too long / some loading indicators missing

**Status:** Done · **Priority:** Medium · **Labels:** in-review
**Created:** 2026-06-06T20:05:36.855Z · **Completed:** 2026-06-09T09:38:19.458Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-278/performance-loading-times-are-generally-too-long-some-loading

## Description

Initial loading is one thing but reloading already visited pages should be faster - cached/improved.

Reference tested data: 5 places with about 1500 stored days together:

* Home: initial load \~5 seconds — acceptable but loading indicator missing completely
* Monthly summary loading \~4 seconds for >1000 days — loading indicator present — fine as is
* Month details: loads fairly long, all at once. nit: placeholder loading cards etc. Still okay.
* Improve: reloading Monthly summary after coming back from selected month details and again when selecting same month? (coming from home to already loaded place it's displayed immediately)

---

## Technical Plan

### Root Causes

| Screen | Root cause |
| -- | -- |
| **Home (\~5 s)** | `loadSavedPlaces()` calls `getSavedDataFor()` for every place, which fires `getHoursForDay` once per day row — resulting in 1500+ SQLDelight queries for 5 places × \~300 days. Only `name`, `days.size`, and `stations` are actually used on this screen. |
| **Home** | `HomeUiState` has no `isLoading` field — the list silently populates with no indicator. |
| **MonthlyStatistics (re-loads on return)** | Same N+1 fetch on every load. Additionally, a `LaunchedEffect(placeName, year, month)` in `MonthlyStatisticsView.kt` double-fires on first composition — causing two back-to-back full fetches on entry. |

---

### Implementation Plan (ordered by priority)

#### 1+2 — Replace Home N+1 with a single `GROUP BY` query (S)

Add to `AppDatabase.sq`:

```sql
getAllPlaceDayCounts:
SELECT weatherResponseResolvedAddress, COUNT(*) AS dayCount
FROM Day GROUP BY weatherResponseResolvedAddress;
```

Update `Database.kt`, `WeatherRepository.kt`, `HomeViewModel.kt` to use this query instead of `getSavedDataFor`. Goes from \~1510 queries → 1. Home should load in milliseconds.

#### 3 — Add `isLoading` flag + skeleton rows to Home (S)

* Add `val isLoading: Boolean = false` to `HomeUiState`
* Set it `true` at start of `loadSavedPlaces()`, `false` when done
* In `HomeView.kt`: render 3–5 skeleton `PlaceRow` placeholders while loading

#### 5 — Remove the double-fetch `LaunchedEffect` in MonthlyStatisticsView (S)

The `LaunchedEffect(placeName, year, month)` in `MonthlyStatisticsView.kt` is redundant — month navigation is already driven by ViewModel methods (`navigateToPreviousMonth`/`navigateToNextMonth`). Removing it eliminates the duplicate fetch on first entry.

#### 4 — In-memory cache in `WeatherRepositoryImpl` (M)

Add a `HashMap<String, WeatherResponse>` cache in `WeatherRepositoryImpl`:

* `getSavedDataFor()` checks cache first, stores result on miss
* Invalidate the entry in `saveWeatherResponse` for the affected address
* Back-and-forward navigation across months becomes near-instant
* Cache is scoped to the `WeatherRepository` Koin singleton (session lifetime)

#### 7 — Skeleton/shimmer cards for MonthlyStatistics + split PlaceSummary station fetch (M)

* `MonthlyStatisticsView.kt`: show skeleton `DaySummaryRow` cards while `dailySummaries.isEmpty()`. When rows arrive but `statistics == null`, show day rows immediately with a spinner only where the stats card will appear (progressive render).
* `PlaceSummaryView.kt` / `PlaceSummaryViewModel.kt`: fire the station backfill as a fire-and-forget coroutine so day data renders without waiting for the network call.

#### 6 — Month-scoped DB query (M, lower priority)

Add `getDaysForMonth` to `AppDatabase.sq` — fetches \~30 rows per query instead of \~1500. Useful if the Kotlin-side filter proves a bottleneck after item 4 is in place, but defer until then.

---

### Effort Summary

| \# | Item | Key files | Effort |
| -- | -- | -- | -- |
| 1+2 | GROUP BY count query replaces N+1 on Home | `AppDatabase.sq`, `Database.kt`, `WeatherRepository.kt`, `HomeViewModel.kt` | S |
| 3 | isLoading flag + skeleton rows on Home | `HomeViewModel.kt`, `HomeView.kt` | S |
| 5 | Remove double-fetch LaunchedEffect | `MonthlyStatisticsView.kt` | S |
| 4 | In-memory WeatherResponse cache | `WeatherRepository.kt` | M |
| 7 | Skeleton cards + station fetch split | `MonthlyStatisticsView.kt`, `PlaceSummaryView.kt`, `PlaceSummaryViewModel.kt` | M |
| 6 | Month-scoped DB query | `AppDatabase.sq`, `Database.kt`, `WeatherRepository.kt`, `MonthlyStatisticsViewModel.kt` | M |

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-08T15:04:40.359Z

Implemented all in-scope items (1+2, 3, 5, 4, 7). Item 6 (month-scoped `getDaysForMonth` query) deferred per the ticket as lower priority.

**Branch:** `kim--278-performance-loading`
**PR:** https://github.com/kmyllyvi/ReWinds/pull/22
**Build:** PASS — `:composeApp:compileDebugKotlinAndroid` green. (Note: the `buildAndroidOnly` wrapper task hits a pre-existing configuration-cache error unrelated to this change — `Task.project` invoked at execution time — so compilation was verified via the compile task directly.)
**Tests:** PASS — `:composeApp:testDebugUnitTest`, 311 tests green (full `--rerun-tasks`).

**Summary of changes**
- **1+2 — Home N+1 → GROUP BY:** added `getAllPlaceDayCounts` to `AppDatabase.sq`, exposed via `Database.getPlaceDayCounts()` and `WeatherRepository.getPlaceDayCounts()`. `HomeViewModel.loadSavedPlaces()` now uses one query for day counts instead of a full per-place `getSavedDataFor` load.
- **3 — Home loading state:** added `isLoading` to `HomeUiState`; 3 muted skeleton rows render while the first load is in flight.
- **5 — Double-fetch fix:** removed the redundant `LaunchedEffect(placeName, year, month)` in `MonthlyStatisticsView` (init + VM navigation methods already drive loads).
- **4 — In-memory cache:** session-scoped `HashMap<String, WeatherResponse>` in `WeatherRepositoryImpl`; read-through in `getSavedDataFor`, invalidated on every save (via a `persistAndInvalidate` helper) and on delete.
- **7 — Progressive render + station split:** `MonthlyStatisticsView` shows skeleton day rows while loading and renders day rows immediately with a spinner only where the stats card lands; `PlaceSummaryViewModel` emits core weather data first and runs station backfill as a separate fire-and-forget coroutine.

Commits are split to match the ticket's strategy (1+2+3 / 5 / 4+7); each compiles independently. Updated all `WeatherRepository`/`Database` test fakes for the new method.

**Deviations:** Item 6 deferred (per ticket). The `getSavedDataFor` cache is also invalidated on `deletePlace` (not explicitly spec'd, but required for correctness). iOS not built via Gradle per project workflow — needs manual Xcode verification.

