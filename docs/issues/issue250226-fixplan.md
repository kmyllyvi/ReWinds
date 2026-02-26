# Fix Plan: Places Always Map Correctly to API Calls

## Issue Reference
`issue250226.md` — Weather data loaded for wrong place when downloading a month.

## Root Cause

**Asymmetry** in how the weather API location is resolved:

| Operation | API input | Resolution |
|---|---|---|
| `addPlaceFromSearch` (first fetch) | `"60.3172%2C24.9633"` | Lat/lon — precise, unambiguous |
| `getDaysRange` (subsequent downloads) | `"Helsinki Airport"` | Text string — Visual Crossing re-geocodes |

When `getDaysRange` passes the place name string to Visual Crossing, the API does its own text geocoding and can match a wrong location.

**Secondary consequence**: When fetched with a wrong location, the API returns a different `resolvedAddress`. Since `saveWeatherResponse` uses `resolvedAddress` as the DB primary key, a *new duplicate place record* is created in the DB (rather than adding days to the existing record). This is why the user saw "a place was added" rather than the correct place being updated.

The correct lat/lon are **already stored in the DB** from the initial add — they just aren't used for subsequent downloads.

## File to Modify

`composeApp/src/commonMain/kotlin/core/WeatherRepository.kt`

## Changes

### 1. Add private helper `resolveLocationString`

Add after `calculateDateGaps` (~line 121), before `getDaysRange`:

```kotlin
private fun resolveLocationString(place: String, existingData: WeatherResponse?): String {
    if (existingData == null) return place
    val lat = existingData.latitude
    val lon = existingData.longitude
    if (lat == 0.0 && lon == 0.0) return place
    return "$lat%2C$lon"
}
```

Uses the same `"lat%2Clon"` format already established in `addPlaceFromSearch` (line 251).

### 2. Fix single-day fetch path in `getDaysRange` (~lines 133–138)

**Before:**
```kotlin
} else {
    Log.d("Fetching single day from network: $place, $fromDate")
    val networkResponse = fetchWeatherFromNetwork(place, fromDate, null)
    database.saveWeatherResponse(networkResponse)
    return networkResponse
}
```

**After:**
```kotlin
} else {
    Log.d("Fetching single day from network: $place, $fromDate")
    val locationString = resolveLocationString(place, existingPlaceData)
    val networkResponse = fetchWeatherFromNetwork(locationString, fromDate, null)
    val correctedResponse = networkResponse.copy(resolvedAddress = place, address = place)
    database.saveWeatherResponse(correctedResponse)
    return correctedResponse
}
```

### 3. Fix date-range gap fetch path in `getDaysRange` (~lines 188–202)

Compute `locationString` once before the gap loop, then correct `resolvedAddress` before each save.

**Before:**
```kotlin
val dateGaps = calculateDateGaps(missingDates)
Log.d(...)
for (gap in dateGaps) {
    try {
        val gapResponse = fetchWeatherFromNetwork(place, gap.startDate, gap.endDate)
        database.saveWeatherResponse(gapResponse)
    } catch (e: Exception) { ... }
}
```

**After:**
```kotlin
val dateGaps = calculateDateGaps(missingDates)
val locationString = resolveLocationString(place, existingPlaceData)
Log.d(...)
for (gap in dateGaps) {
    try {
        val gapResponse = fetchWeatherFromNetwork(locationString, gap.startDate, gap.endDate)
        val correctedGapResponse = gapResponse.copy(resolvedAddress = place, address = place)
        database.saveWeatherResponse(correctedGapResponse)
    } catch (e: Exception) { ... }
}
```

## Why This Works

- `resolveLocationString` extracts stored lat/lon from the existing DB record and builds the coordinate string (e.g., `"60.3172%2C24.9633"`), falling back to the place name only if no valid coordinates exist
- `.copy(resolvedAddress = place, address = place)` ensures the fetched data is saved under the existing DB key — `saveWeatherResponse`'s merge logic then correctly identifies it as an existing place and only adds new days (no duplicates)
- No interface changes required — fix is purely internal to `WeatherRepositoryImpl`

## Verification

1. Add a place (e.g., "Helsinki Airport")
2. Navigate to PlaceSummaryView
3. Tap a grey (missing) month → confirm download → tap "Download"
4. Verify data is for the correct place (not a wrong geocoded match)
5. Verify no duplicate place appeared on the home screen
6. Run existing tests: `./gradlew :composeApp:testDebugUnitTest`
