# Weather Data Models

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. -->

**Last updated:** 2026-06-09 (bootstrap)
**Status:** Active

---

## Overview

Core serializable data models shared between the API layer (Visual Crossing), the database layer (SQLDelight), and the AI tool layer. All models are defined in `core/WeatherData.kt`.

---

## Models

### WeatherResponse
Top-level container returned by Visual Crossing and reconstructed from DB.

```kotlin
data class WeatherResponse(
    val resolvedAddress: String,             // canonical place key used throughout the app
    val address: String? = null,
    val queryCost: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val tzoffset: Double? = null,
    val days: List<Day>? = null,
    val stations: Map<String, Station>? = null   // ID → Station; only from add-place calls
)
```

### Day
One calendar day of weather data. Mirrors the Visual Crossing day object with `include=hours`.

```kotlin
data class Day(
    val datetime: String,           // "YYYY-MM-DD" — primary key for upsert logic
    val datetimeEpoch: Long?,
    val tempmax: Double?,           // °C
    val tempmin: Double?,           // °C
    val temp: Double?,              // °C daily average
    val feelslikemax / feelslikemin / feelslike: Double?,
    val dew: Double?,               // °C
    val humidity: Double?,          // %
    val precip: Double?,            // mm
    val precipprob: Double?,        // %
    val precipcover: Double?,       // %
    val preciptype: List<String>?,  // e.g. ["rain"], stored via listOfStringAdapter
    val snow: Double?,              // mm
    val snowdepth: Double?,         // mm
    val windgust: Double?,          // m/s
    val windspeed: Double?,         // m/s (daily average)
    val winddir: Double?,           // degrees
    val pressure: Double?,          // hPa
    val cloudcover: Double?,        // %
    val visibility: Double?,        // km
    val solarradiation: Double?,    // W/m²
    val solarenergy: Double?,       // MJ/m²
    val uvindex: Double?,
    val sunrise: String?,
    val sunriseEpoch: Long?,
    val sunset: String?,
    val sunsetEpoch: Long?,
    val moonphase: Double?,         // 0.0–1.0
    val conditions: String?,
    val description: String?,
    val icon: String?,
    val stations: List<String>?,    // list of station IDs (not the full Station objects)
    val source: String?,
    val hours: List<Hour>? = null,
    val normal: NormalStats? = null
)
```

**Unit note:** All wind values (`windspeed`, `windgust`) are stored and returned in **m/s** as received from Visual Crossing. The AI layer, the MetricMapper, and all display components that show knots convert via `× 1.944`.

### Hour
Hourly sub-record for a day. Same wind/temperature/precipitation fields as `Day` minus day-level aggregates.

```kotlin
data class Hour(
    val datetime: String,       // "HH:MM:SS"
    val datetimeEpoch: Long?,
    val temp / feelslike / humidity / dew: Double?,
    val precip / precipprob: Double?,
    val snow / snowdepth: Double?,
    val preciptype: List<String>?,
    val windgust / windspeed / winddir: Double?,
    val pressure / visibility / cloudcover: Double?,
    val solarradiation / solarenergy / uvindex: Double?,
    val conditions / icon / source: String?,
    val stations: List<String>?
)
```

### Station
A physical weather measurement station near a place.

```kotlin
data class Station(
    val id: String?,
    val name: String?,
    val distance: Double?,    // metres (as returned by Visual Crossing)
    val latitude: Double?,
    val longitude: Double?,
    val useCount: Int?,
    val quality: Int?,
    val contribution: Double?
)
```
Stations with null `latitude` or `longitude` are filtered out before persistence.

### NormalStats
Optional statistical normals returned when `include=stats` is used (not currently in the default query).

```kotlin
data class NormalStats(
    val tempmax / tempmin / feelslike / precip / humidity / snowdepth /
    windspeed / windgust / winddir / cloudcover: List<Double?>?
)
```

---

## GeoSearchResult (from Open-Meteo geocoding)

```kotlin
data class GeoSearchResult(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val region: String? = null       // @SerialName("admin1")
)
```

---

## DataAvailabilityStatus (enum)

```kotlin
enum class DataAvailabilityStatus { Available, Partial, Missing }
```
Returned by `WeatherRepository.checkDataAvailability()`. Used by the AI permission-gate in `WeatherTools.handleGetWeatherMetrics`.

---

## DayWeatherSummary (place/DayWeatherSummary.kt)

Derived display model produced by `PlaceSummaryViewModel` and `MonthlyStatisticsViewModel` from `Day`. Not persisted.

```kotlin
data class DayWeatherSummary(
    val date: String?,
    val description: String?,
    val maxTemp / minTemp / avgTemp: Double?,
    val avgWindSpeed: Double?,         // m/s (from Day.windspeed)
    val maxWindSpeed: Double?,         // m/s (from Day.windgust)
    val sustainedWindSpeed: Double?,   // m/s rolling average from hourly data
    val solarenergy: Double?,
    val isFoggy: Boolean,
    val foggyHours: Int,
    val precipitation: Double? = null,
    val windDirection: Double? = null,
    val sunrise / sunset: String? = null,
    val isMatch: Boolean = false       // true when DaysOfInterestFilter.matches()
)
```
