# KIM-140: Dynamic "Days of Interest" — Implementation Plan

## Overview

Replace the hardcoded `KiteSpotterConfig` "kiteable day" thresholds with a user-configurable, AI-parsed "days of interest" filter. The user describes their criteria in natural language; Claude parses it once into a structured filter that is persisted and applied at runtime.

---

## Problem Statement

Currently, `KiteSpotterConfig` defines hardcoded thresholds:
- `MIN_SUSTAINED_WIND_SPEED_KMH = 20.0`
- `MIN_TEMP_CELSIUS = 10.0`
- `SUSTAINED_WIND_WINDOW_HOURS = 2`

These are applied in `MonthlyStatisticsViewModel` and `WeatherCards` to count/highlight "kiteable days". There is no way for the user to change these criteria.

---

## Solution Architecture

### Flow

```
User types criteria → Save button → AI parses → Structured filter stored in DB
                                                          ↓
                         MonthlyStatisticsViewModel ← loads filter at runtime
                         WeatherCards               ← loads filter at runtime
```

### AI Parsing (one-time, on settings save)

- Send user's natural language text to Claude API
- System prompt instructs Claude to extract structured filter fields from the text
- Claude returns a JSON object
- JSON is parsed into `DaysOfInterestFilter` and stored

No AI calls happen at runtime — the filter is pure Kotlin logic once parsed.

---

## Data Model

### `DaysOfInterestFilter`

```kotlin
@Serializable
data class DaysOfInterestFilter(
    val naturalLanguageCriteria: String,   // original user text (for display)
    val minTempC: Double? = null,
    val maxTempC: Double? = null,
    val minWindSpeedKmh: Double? = null,
    val maxWindSpeedKmh: Double? = null,
    val windDirections: List<String>? = null,  // e.g. ["S", "SW", "W"]
    val sustainedWindHours: Int? = null,        // consecutive hours of min wind
    val daylightOnly: Boolean? = null,          // only count hours between sunrise/sunset
    val noRain: Boolean? = null,
    val maxCloudCoverPct: Double? = null
) {
    companion object {
        /** Default filter matching existing KiteSpotterConfig behaviour */
        val DEFAULT = DaysOfInterestFilter(
            naturalLanguageCriteria = "Sustained wind ≥ 20 km/h for 2+ hours, temperature ≥ 10°C",
            minWindSpeedKmh = 20.0,
            sustainedWindHours = 2,
            minTempC = 10.0
        )
    }
}
```

### `AppSettings` DB table (key-value store)

```sql
CREATE TABLE AppSettings (
    key   TEXT NOT NULL PRIMARY KEY,
    value TEXT NOT NULL
);
```

Filter stored under key `days_of_interest_filter` as serialized JSON.

---

## Files to Create

### `composeApp/src/commonMain/kotlin/core/DaysOfInterestFilter.kt`
- `DaysOfInterestFilter` data class (serializable)
- `fun DaysOfInterestFilter.matches(day: DayWeatherSummary): Boolean` extension

### `composeApp/src/commonMain/kotlin/settings/SettingsViewModel.kt`
- Loads saved filter from DB on init
- Exposes `StateFlow<DaysOfInterestFilter?>` for current filter
- Exposes `StateFlow<SettingsUiState>` for AI parsing progress (Idle / Parsing / Success / Error)
- `fun saveFilter(criteria: String)` — calls AI to parse, stores result

### `composeApp/src/commonMain/kotlin/core/AppSettingsRepository.kt`
- Wraps `AppDatabase` to read/write `AppSettings` table
- `suspend fun getString(key: String): String?`
- `suspend fun setString(key: String, value: String)`

### `composeApp/src/commonMain/kotlin/ai/DaysOfInterestParser.kt`
- Single function: `suspend fun parseCriteria(criteria: String, client: AnthropicClient): DaysOfInterestFilter`
- Calls Claude API (no tools, single turn) with a structured extraction prompt
- Parses JSON response into `DaysOfInterestFilter`

---

## Files to Modify

### `AppDatabase.sq`
- Add `AppSettings` table + `getAppSetting` / `setAppSetting` queries

### `SettingsView.kt`
- Add "Days of Interest" section below API key sections
- Text field for criteria input
- Save button (triggers AI parse)
- Loading/error/success state feedback
- Show current active criteria (if any)

### `MonthlyStatisticsViewModel.kt`
- Inject `AppSettingsRepository`
- Load `DaysOfInterestFilter` on init (fall back to `DEFAULT` if none saved)
- Replace `KiteSpotterConfig` references with `filter.matches(day)`

### `WeatherCards.kt`
- Accept `DaysOfInterestFilter` as parameter (passed from VM via state)
- Replace hardcoded `KiteSpotterConfig` check with `filter.matches(daySummary)`

### `DI.kt`
- Register `AppSettingsRepository` as singleton
- Register `SettingsViewModel` with `viewModelOf`

### `strings.xml` + `AppStrings.kt`
- New strings: section title, field label, placeholder, save button, parsing state messages

---

## AI Parsing Prompt Design

**System prompt** (sent to Claude, no tools):
```
You are a structured data extractor. The user will describe criteria for a "day of interest"
in natural language. Extract the criteria into JSON with these optional fields:

{
  "minTempC": number | null,
  "maxTempC": number | null,
  "minWindSpeedKmh": number | null,
  "maxWindSpeedKmh": number | null,
  "windDirections": string[] | null,   // compass points: N, NE, E, SE, S, SW, W, NW
  "sustainedWindHours": number | null,
  "daylightOnly": boolean | null,
  "noRain": boolean | null,
  "maxCloudCoverPct": number | null
}

Convert all wind speeds to km/h. Convert knots: 1 kn = 1.852 km/h.
Reply with ONLY the JSON object, no explanation.
```

**User message**: the criteria text verbatim.

---

## Evaluation Logic (`matches()`)

```kotlin
fun DaysOfInterestFilter.matches(day: DayWeatherSummary): Boolean {
    if (minTempC != null && (day.avgTemp ?: Double.MIN_VALUE) < minTempC) return false
    if (maxTempC != null && (day.avgTemp ?: Double.MAX_VALUE) > maxTempC) return false
    if (noRain == true && (day.precipitation ?: 0.0) > 0.0) return false
    // Wind direction check — winddir is degrees, convert to compass point
    if (windDirections != null && day.windDirection != null) {
        val compassPoint = degreesToCompass(day.windDirection)
        if (compassPoint !in windDirections) return false
    }
    // Sustained wind check uses existing rolling-window logic (in VM)
    if (minWindSpeedKmh != null && (day.sustainedWindSpeed ?: 0.0) < minWindSpeedKmh) return false
    if (maxWindSpeedKmh != null && (day.sustainedWindSpeed ?: 0.0) > maxWindSpeedKmh) return false
    return true
}
```

The `sustainedWindHours` is used in the existing rolling-window calculation in the VMs (currently reads from `KiteSpotterConfig.SUSTAINED_WIND_WINDOW_HOURS`).

---

## Out of Scope (this issue)

- Daylight-hours filtering (sunrise/sunset data is available in DB but requires per-hour evaluation — defer)
- Multiple named filters / filter presets
- German translations for new strings (covered by KIM-57 Phase 2)

---

## Implementation Order

1. `AppDatabase.sq` — add AppSettings table
2. `AppSettingsRepository.kt` — new
3. `DaysOfInterestFilter.kt` — new data model + `matches()`
4. `DaysOfInterestParser.kt` — AI parsing call
5. `SettingsViewModel.kt` — new VM
6. `DI.kt` — register new components
7. `MonthlyStatisticsViewModel.kt` — swap KiteSpotterConfig
8. `WeatherCards.kt` — swap KiteSpotterConfig
9. `SettingsView.kt` — add UI section
10. `strings.xml` + `AppStrings.kt` — new strings
11. Verify Android build
