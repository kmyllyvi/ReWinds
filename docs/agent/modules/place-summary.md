# Place Summary Module

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. -->

**Last updated:** 2026-06-09 (bootstrap)
**Status:** Active

---

## Purpose

The per-place detail screen showing a year/month grid, stored days with weather summaries, nearby weather station map, and month-download controls. Also hosts `MonthlyStatisticsView` as a push destination for per-month drill-down.

---

## Responsibilities

- Loading all stored days for a place from the WeatherRepository (uses session cache).
- Auto-backfilling weather station data on first load (fire-and-forget network call when station table is empty).
- User-triggered station refresh.
- Rendering per-year, per-month completion state (`MonthCellState`: FULL / PARTIAL / NO_DATA) based on stored-day counts vs. expected calendar days.
- Calculating monthly average temperatures for the year selector.
- Triggering full-month downloads and reloading after completion.
- Navigating to `MonthlyStatisticsView` for a selected month.
- Exposing `StationMapSummary` (count + closest distance in km) pre-computed so the composable renders values only.
- Preserving year-selector scroll offset in ViewModel state to survive recomposition.

---

## Dependencies

### Internal
- `core.WeatherRepository` — `getSavedDataFor()`, `downloadFullMonth()`, `fetchAndPersistStations()`, `getPersistedStations()`.
- `core.KiteSpotterConfig` — `SUSTAINED_WIND_WINDOW_HOURS` constant for rolling average calculation.

---

## Key interfaces

### PlaceSummaryViewModel public API
```kotlin
val uiState: StateFlow<WeatherSummaryUiState>
val navigationEvent: Flow<NavigationEvent>       // NavigationEvent.ToMonthlySummary
val monthlyAverageTemps: StateFlow<Map<Int, Double?>>
val selectedYear: StateFlow<Int?>
val yearScrollOffset: StateFlow<Int>
val isRefreshingStations: StateFlow<Boolean>
val showStationMap: StateFlow<Boolean>
val stationMapSummary: StateFlow<StationMapSummary>

fun refreshData()
fun setSelectedYear(year: Int?)
fun setYearScrollOffset(offset: Int)
fun onDownloadFullMonth(year: Int, month: Int)
fun updateMonthTemperaturesForYear(year: Int?)
fun onShowMonth(year: Int?, month: Int?)
fun refreshStations()
fun openStationMap()
fun closeStationMap()

// Pure helpers (also available as Companion statics for tests)
fun calculateMonthCellStates(selectedYear: Int?, storedDays: List<DayWeatherSummary>): List<MonthCellInfo>
fun calculateMonthCompletionStatusMap(...): Map<Int, MonthCompletionInfo>
fun calculateMissingDaysMap(...): Map<Int, Int>
```

### WeatherSummaryUiState (sealed)
- `Loading`
- `Success(placeName, storedDays, latitude, longitude, currentPlaceDescription, isDownloadingMonth, stations, stationsError)`
- `Error(message)`

### MonthCellInfo
```kotlin
data class MonthCellInfo(month: Int, state: MonthCellState, presentDaysCount: Int, totalDaysInMonth: Int)
// MonthCellState: FULL | PARTIAL | NO_DATA
```

### StationDisplayData
```kotlin
data class StationDisplayData(name: String?, latitude: Double, longitude: Double, distance: Double?, quality: Int?, useCount: Int?)
```
`distance` is in metres as returned by Visual Crossing; display layer divides by 1000.

### PARTIAL_DAY_THRESHOLD constant
`20` — a month with at least this many stored days (but not the full count) renders as `FULL`. This is a placeholder value; the product threshold has not been formally specified.

---

## Known constraints

- `PARTIAL_DAY_THRESHOLD = 20` is noted in the code as a product decision pending specification.
- Station auto-backfill is fire-and-forget; if the network call fails silently, the station table remains empty and no error is surfaced to the user on first load.
- `getSavedDataFor` loads all days and all hours for the place; this can be slow for places with many years of data. The session-lifetime cache in `WeatherRepositoryImpl` mitigates repeat loads.

---

## Decisions log

- See `/docs/agent/decisions/` for records created after the bootstrap phase.
