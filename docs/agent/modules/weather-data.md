# Weather Data Module

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. -->

**Last updated:** 2026-06-09 (bootstrap)
**Status:** Active

---

## Purpose

Owns all weather data: fetching from the Visual Crossing API, persisting to SQLDelight, gap-fill caching, and querying stored data. Also handles geocoding via Open-Meteo and weather station persistence for the map feature.

---

## Responsibilities

- Geocoding search using Open-Meteo (`searchForLocations`).
- Adding a new place: fetch station + place metadata from Visual Crossing, persist to DB.
- Gap-fill date-range queries: compare requested dates against DB, fetch only missing consecutive gaps from Visual Crossing, merge results.
- Session-lifetime in-memory cache (`savedDataCache: HashMap<String, WeatherResponse>`) keyed by resolved address; invalidated on every write.
- Checking data availability without fetching (`checkDataAvailability` → `DataAvailabilityStatus`).
- Preventing future/forecast data ingestion: all `toDate` values are truncated to yesterday.
- Weather station lifecycle: persist on add, auto-backfill from `PlaceSummaryViewModel`, user-triggered refresh, full-replace upsert semantics.
- Database export/import (expect/actual per platform: `DatabaseExportImport`).

---

## Dependencies

### Internal
- `core.Database` / `SqlDelightDatabase` — SQLDelight-backed persistence.
- `core.Networking` / `NetworkService` — Ktor HTTP client wrapper.
- `core.WeatherApiKeyManager` — runtime in-memory store for the Visual Crossing API key.
- `core.DataMapping` — maps between API models (`WeatherResponse`, `Day`, `Hour`, `Station`) and SQLDelight DB types.

### External
- **Visual Crossing** `https://weather.visualcrossing.com/...` — historical weather, hourly data, station metadata. Requires `unitGroup=metric&include=hours`.
- **Open-Meteo geocoding** `https://geocoding-api.open-meteo.com/v1/search` — no API key.

---

## Key interfaces

### WeatherRepository
```kotlin
suspend fun getSavedPlaceNames(): List<String>
suspend fun getPlaceDayCounts(): Map<String, Long>          // lightweight: GROUP BY, no day rows loaded
suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse?
suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse
suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse
suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse
suspend fun deletePlace(placeName: String)
suspend fun searchForLocations(query: String): List<GeoSearchResult>
suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse
suspend fun fetchAndPersistStations(place: String): StationsResult
suspend fun getPersistedStations(place: String): List<Station>
suspend fun checkDataAvailability(place: String, fromDate: String, toDate: String): DataAvailabilityStatus
```

### Database (internal interface over SqlDelightDatabase)
```kotlin
suspend fun getAllSavedPlaces(): List<String>
suspend fun getPlaceDayCounts(): Map<String, Long>
suspend fun getSavedPlaceFull(place: String): WeatherResponse?
suspend fun saveWeatherResponse(weatherResponse: WeatherResponse)
suspend fun getWeatherDataFor(placeName: String, date: String): WeatherResponse?
suspend fun deletePlace(placeName: String)
suspend fun cleanupForecastDays()
suspend fun upsertStations(place: String, stations: List<Station>)
suspend fun getStationsForPlace(place: String): List<Station>
```

### DataAvailabilityStatus (enum)
`Available` | `Partial` | `Missing`

### StationsResult (sealed)
`Success(stations: List<Station>)` | `Empty` | `Error(message: String)`

### DatabaseExportImport (expect class)
```kotlin
suspend fun exportDatabase(): Result<String>
suspend fun importDatabase(filePath: String): Result<String>
suspend fun listBackups(): Result<List<String>>
```

---

## Known constraints

- Visual Crossing responses use `unitGroup=metric`; all wind speeds are stored in m/s and converted to knots in the AI layer and display layer.
- The cache (`savedDataCache`) is Koin-singleton-scoped and lives for the app session. A place whose data is written via `saveWeatherResponse` has its cache entry removed; the next read will reload from DB.
- `getPreviousDays` always fetches from the network and overwrites (no DB-first check). Intended for real-time refresh; not used for the main month-download flow.
- Station distance values from Visual Crossing are in **metres**. The display layer divides by 1000 for km labels.
- `cleanupForecastDays` is defined but currently commented out in `HomeViewModel.init`. Forecast data may accumulate if it was written before this safeguard was added.
- `addPlaceFromSearch` uses `last0days` query (zero weather days) to retrieve station data at add-time. Day data is only fetched when the user explicitly downloads a date range.

---

## Decisions log

- See `/docs/agent/decisions/` for records created after the bootstrap phase.
