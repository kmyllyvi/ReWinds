# Monthly Statistics Module

**Last updated:** 2026-06-18 (PR #39 — KIM-303)
**Status:** Active

---

## Purpose

The per-month drill-down screen showing daily weather summaries, aggregated statistics, a wind bar chart with peak-wind highlighting, and month-navigation controls. Applies the user's configurable days-of-interest filter to mark matching days. Each day card is tappable and opens a `ModalBottomSheet` with an hourly wind chart (09:00–21:00 local-time window).

---

## Responsibilities

- Filtering and mapping stored days for the selected year/month from `WeatherRepository.getSavedDataFor()`.
- Computing aggregated statistics: temperature extremes, total rainfall, total solar energy, average sustained wind.
- Counting days matching the active `DaysOfInterestFilter`.
- Calculating the rolling-window maximum sustained wind speed per day (window size from filter or `KiteSpotterConfig.SUSTAINED_WIND_WINDOW_HOURS`).
- Identifying the peak sustained-wind day index for chart highlighting.
- Month navigation (forward/back with year wrap-around).
- Full-month download trigger.
- Loading the `DaysOfInterestFilter` from `AppSettingsStore` on each statistics reload.
- Managing selected-day sheet state: opening on tap, resolving 09:00–21:00 hourly wind points from the already-loaded `WeatherResponse`, dismissing on swipe or month reload.

---

## Dependencies

### Internal
- `core.WeatherRepository` — `getSavedDataFor()`, `downloadFullMonth()`.
- `core.AppSettingsStore` — interface; reads `days_of_interest_filter` JSON string. (Previously typed as `AppSettingsRepository`; changed to the bound interface in KIM-303 to allow unit testing without a DB.)
- `core.DaysOfInterestFilter`, `core.DaysOfInterestFilter.matches()`, `core.DaysOfInterestFilter.filterSummary()`.
- `core.KiteSpotterConfig` — default `SUSTAINED_WIND_WINDOW_HOURS`.
- `place.DayWeatherSummary` — shared summary model used by both Place Summary and Monthly Statistics.
- `place.HourlyWindPoint` — chart data model produced by `hourlyWindWindow()`.
- `settings.SettingsViewModel.FILTER_KEY` — shared constant for the settings key.

---

## Key interfaces

### MonthlyStatisticsViewModel public API
```kotlin
val statistics: StateFlow<CalculatedStats?>
val dailySummaries: StateFlow<List<DayWeatherSummary>>
val isDownloading: StateFlow<Boolean>
val year: StateFlow<Int>
val month: StateFlow<Int>
val peakWindDayIndex: StateFlow<Int>       // -1 when no sustained-wind data

// Day detail sheet state (KIM-303)
val selectedDay: StateFlow<DayWeatherSummary?>          // non-null drives sheet visibility
val selectedDayHours: StateFlow<List<HourlyWindPoint>>  // 0–13 points; empty = no hourly data
val isLoadingHours: StateFlow<Boolean>                  // true while hours are being resolved

fun reloadStatistics(year: Int? = null, month: Int? = null)
fun navigateToPreviousMonth()
fun navigateToNextMonth()
fun downloadFullMonth()
fun getMissingDaysCount(dailySummaries: List<DayWeatherSummary>): Int
fun selectDay(summary: DayWeatherSummary)   // opens the detail sheet
fun dismissDaySheet()                       // closes the detail sheet

// internal / testable
internal fun peakSustainedWindIndex(daysData: List<DayWeatherSummary>): Int
```

### CalculatedStats (data class)
| Field | Type | Notes |
|---|---|---|
| `numberOfDaysWithData` | `Int` | |
| `averageMinTemp` / `averageMaxTemp` / `overallAverageTemp` | `Double?` | Celsius |
| `absoluteMinTemp` / `coldestDate` | `Double?`, `String?` | |
| `absoluteMaxTemp` / `hottestDate` | `Double?`, `String?` | |
| `daysOfInterestCount` | `Int` | Days matching active `DaysOfInterestFilter` |
| `filterSummary` | `String` | Human-readable filter description |
| `totalRainfall` | `Double?` | mm; null when zero |
| `totalSolarEnergy` | `Double?` | MJ/m²; null when zero |
| `averageSustainedWindSpeed` | `Double?` | m/s (raw DB value) |

### HourlyWindPoint (place/HourlyWind.kt)
```kotlin
data class HourlyWindPoint(
    val hour: Int,          // local hour 9..21; used to place the point in the fixed x-axis slot
    val label: String,      // "HH" zero-padded, e.g. "09"
    val windspeed: Double?,
    val windgust: Double?,
    val winddir: Double?    // meteorological degrees (direction wind blows from)
)
```

### Pure utility functions (place/HourlyWind.kt)
| Function | Signature | Purpose |
|---|---|---|
| `hourlyWindWindow` | `(hours: List<Hour>?, tzoffset: Double?): List<HourlyWindPoint>` | Filters a day's `Hour` rows to the 09:00–21:00 local-time window using `tzoffset` (hours east of UTC). No timezone database required. Drops rows without `datetimeEpoch` — no fabrication. |
| `yAxisTicks` | `(yMax: Double, tickIntervals: Int = 4): List<Double>` | Returns evenly-spaced "nice" tick values (1/2/5×10ⁿ step) from 0 to a rounded ceiling at or above `yMax`, ordered high-to-low. Used by `HourlyWindChart` and separately unit-testable. |
| `windFlowBearing` | `(meteorologicalDegrees: Double): Float` | Converts a meteorological wind bearing (direction wind blows *from*) to a flow bearing (direction it blows *toward*), normalised to [0, 360). Used to rotate direction arrows in the chart. |

Constants in `HourlyWind.kt`:
- `WINDOW_START_HOUR = 9`, `WINDOW_END_HOUR = 21`
- `HOURLY_WIND_SLOT_COUNT = 13` (inclusive count of hours in window)

---

## UI Components (place/)

### DayDetailSheet (place/DayDetailSheet.kt)
`ModalBottomSheet` wrapping the hourly chart for a single day. Visibility is driven entirely by `selectedDay` in the ViewModel (non-null = visible). Three content states:
- **Loading** — `CircularProgressIndicator` while `isLoadingHours` is true.
- **Empty** — centred label when `selectedDayHours` is empty after loading.
- **Chart** — `HourlyWindChart` when points are available.

Dismiss (swipe or scrim tap) routes through `onDismiss` callback to `vm.dismissDaySheet()`.

### HourlyWindChart (place/components/HourlyWindChart.kt)
Dual-series (wind speed + gust) line chart on a transparent `Canvas`. Key layout details:
- Fixed 13-slot x-axis (09:00–21:00). Points land in their matching local-hour slot; empty slots break the line rather than interpolating.
- Y-axis: reserved 40dp left gutter with `YAxisLabels` positioned to align exactly with Canvas gridlines.
- Below the chart: hour labels row and per-hour wind-direction arrow row (both inset by the same 40dp gutter for column alignment).
- Series colours: `accentBlue` (speed), `attention` (gust) — both from `MaterialTheme.rewinds`.
- Unit label (default `"km/h"`) is a parameter so future unit preference can be threaded through without changing chart internals.
- Chart has a `contentDescription` semantics node derived from `AppStrings.hourlyWindChartDesc`.

### DaySummaryRow (place/components/DaySummaryRow.kt) — KIM-303 MV* fix
The inline `AnimatedVisibility` expand/collapse block and its local `mutableStateOf` were removed. The row is now a stateless collapsed card (date, temperature range, peak gust) with an `onClick` parameter. Expansion state and day selection are the ViewModel's responsibility.

---

## Known constraints

- The filter is re-read from `AppSettingsStore` on every `loadStatistics()` call, so changes in Settings take effect on next navigation to Monthly Statistics (or explicit `reloadStatistics()`).
- `averageSustainedWindSpeed` in `CalculatedStats` is stored in m/s (DB unit). The view layer is responsible for any unit conversion for display.
- If `DaysOfInterestFilter.DEFAULT` is active (i.e. no custom filter saved), the default criteria are: sustained wind ≥ 20 km/h for 2+ hours, temperature ≥ 10°C.
- Hourly wind data is sourced from the already-loaded `WeatherResponse` held in `loadedData` — no new repository method is needed. `DayWeatherSummary` is not extended with raw hours.
- Hours without a `datetimeEpoch` are silently dropped; there is no fabrication or interpolation (KIM-303 partial-data rule).
- The ViewModel constructor now takes `AppSettingsStore` (bound interface) rather than `AppSettingsRepository` (concrete class). DI already binds `AppSettingsRepository as AppSettingsStore`.
- Beaufort background bands are explicitly out of scope for the v1 chart (KIM-303 spec).
- The now-unused `DaySummaryRow` strings (`expand`, `collapse`, `dayMinTemp`, etc.) were intentionally left in `AppStrings` in KIM-303 to keep the diff scoped.

---

## Decisions log

- `/docs/agent/decisions/2026-06-18-day-detail-bottom-sheet-pattern.md` — ModalBottomSheet pattern for day detail and MV* fix for DaySummaryRow.
