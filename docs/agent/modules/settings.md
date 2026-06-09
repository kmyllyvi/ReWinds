# Settings Module

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. -->

**Last updated:** 2026-06-09 (bootstrap)
**Status:** Active

---

## Purpose

The Settings tab screen where users manage API keys (Anthropic, Visual Crossing) and configure the days-of-interest filter used to highlight matching days in Monthly Statistics. Also handles language preference.

---

## Responsibilities

- Displaying and saving the Anthropic API key (iOS: via Keychain bridge; Android: UI only, not persisted beyond session — BuildConfig is the Android source of truth).
- Displaying and saving the Visual Crossing API key (iOS: via Keychain bridge).
- Accepting natural-language days-of-interest criteria, parsing them to a structured `DaysOfInterestFilter` via `DaysOfInterestParser` + Anthropic AI call, and persisting the result as JSON in `AppSettingsRepository`.
- Showing parse states: Idle → Parsing → Success (with filter preview) → Error.

---

## Dependencies

### Internal
- `core.AppSettingsRepository` — stores/reads `days_of_interest_filter` JSON.
- `ai.AnthropicClient` — used by `DaysOfInterestParser` to parse natural-language criteria.
- `ai.DaysOfInterestParser` — single-turn Claude call that converts freetext to `DaysOfInterestFilter`.
- `core.KeychainBridge` / `core.WeatherKeychainBridge` — invoked by the View's key save/delete actions via platform `actual` functions.
- `core.ApiKeyManager` / `core.WeatherApiKeyManager` — in-memory stores updated immediately when a key is saved.

---

## Key interfaces

### SettingsViewModel public API
```kotlin
val doiState: StateFlow<DaysOfInterestUiState>
val currentFilter: StateFlow<DaysOfInterestFilter?>

fun saveFilter(criteria: String)
fun resetDoiState()

companion object {
    const val FILTER_KEY = "days_of_interest_filter"  // shared with MonthlyStatisticsViewModel
}
```

### DaysOfInterestUiState (sealed)
`Idle` | `Parsing` | `Success(filter: DaysOfInterestFilter)` | `Error(message: String)`

### DaysOfInterestFilter (serializable data class, `core/DaysOfInterestFilter.kt`)
```kotlin
data class DaysOfInterestFilter(
    val naturalLanguageCriteria: String,
    val minTempC: Double? = null,
    val maxTempC: Double? = null,
    val minWindSpeedKmh: Double? = null,
    val maxWindSpeedKmh: Double? = null,
    val windDirections: List<String>? = null,
    val sustainedWindHours: Int? = null,
    val daylightOnly: Boolean? = null,
    val noRain: Boolean? = null,
    val maxCloudCoverPct: Double? = null
)
```
Default: sustained wind ≥ 20 km/h for 2+ hours, temperature ≥ 10°C.

---

## Known constraints

- Parsing the days-of-interest criteria consumes Anthropic API credits. There is no caching of parse results beyond what is saved to `AppSettingsRepository`.
- The Visual Crossing API key is only persistable on iOS via Keychain. On Android the key comes from `BuildConfig` and cannot be changed at runtime through the Settings UI.
- `saveFilter` silently ignores blank input; no validation beyond blank check is performed before the AI parse call.

---

## Decisions log

- See `/docs/agent/decisions/` for records created after the bootstrap phase.
