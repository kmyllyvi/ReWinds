# Monthly Statistics Module

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. -->

**Last updated:** 2026-06-09 (bootstrap)
**Status:** Active

---

## Purpose

The per-month drill-down screen showing daily weather summaries, aggregated statistics, a wind bar chart with peak-wind highlighting, and month-navigation controls. Applies the user's configurable days-of-interest filter to mark matching days.

---

## Responsibilities

- Filtering and mapping stored days for the selected year/month from `WeatherRepository.getSavedDataFor()`.
- Computing aggregated statistics: temperature extremes, total rainfall, total solar energy, average sustained wind.
- Counting days matching the active `DaysOfInterestFilter`.
- Calculating the rolling-window maximum sustained wind speed per day (window size from filter or `KiteSpotterConfig.SUSTAINED_WIND_WINDOW_HOURS`).
- Identifying the peak sustained-wind day index for chart highlighting.
- Month navigation (forward/back with year wrap-around).
- Full-month download trigger.
- Loading the `DaysOfInterestFilter` from `AppSettingsRepository` on each statistics reload.

---

## Dependencies

### Internal
- `core.WeatherRepository` — `getSavedDataFor()`, `downloadFullMonth()`.
- `core.AppSettingsRepository` — reads `days_of_interest_filter` JSON string.
- `core.DaysOfInterestFilter`, `core.DaysOfInterestFilter.matches()`, `core.DaysOfInterestFilter.filterSummary()`.
- `core.KiteSpotterConfig` — default `SUSTAINED_WIND_WINDOW_HOURS`.
- `place.DayWeatherSummary` — shared summary model used by both Place Summary and Monthly Statistics.
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
val peakWindDayIndex: StateFlow<Int>     // -1 when no sustained-wind data

fun reloadStatistics(year: Int? = null, month: Int? = null)
fun navigateToPreviousMonth()
fun navigateToNextMonth()
fun downloadFullMonth()
fun getMissingDaysCount(dailySummaries: List<DayWeatherSummary>): Int

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

---

## Known constraints

- The filter is re-read from `AppSettingsRepository` on every `loadStatistics()` call, so changes in Settings take effect on next navigation to Monthly Statistics (or explicit `reloadStatistics()`).
- `averageSustainedWindSpeed` in `CalculatedStats` is stored in m/s (DB unit). The view layer is responsible for any unit conversion for display.
- If `DaysOfInterestFilter.DEFAULT` is active (i.e. no custom filter saved), the default criteria are: sustained wind ≥ 20 km/h for 2+ hours, temperature ≥ 10°C.

---

## Decisions log

- See `/docs/agent/decisions/` for records created after the bootstrap phase.
