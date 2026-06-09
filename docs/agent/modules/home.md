# Home Module

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. -->

**Last updated:** 2026-06-09 (bootstrap)
**Status:** Active

---

## Purpose

The entry screen of the Places tab. Displays the list of saved places with day-count summaries, provides location search to add new places, and surfaces error states related to API key configuration.

---

## Responsibilities

- Loading saved place names and their stored-day counts via a lightweight GROUP BY query (no full day/hour load on the home screen).
- Debouncing location search input (500 ms) and displaying Open-Meteo geocoding results.
- Adding a new place from a search result (triggers Visual Crossing fetch + DB persist).
- Navigating to `PlaceSummaryView` on place selection.
- Delete confirmation flow for saved places.
- Displaying `AlertBanner` items and a Visual Crossing key error state (`VcKeyErrorType.MISSING | INVALID`) with a Settings CTA.
- Skeleton loading rows while data loads.
- Debug menu with database export/import controls (hidden behind a toggle).
- Refreshing the Visual Crossing key status when the screen becomes active (so the onboarding nudge hides immediately after the user saves a key in Settings).

---

## Dependencies

### Internal
- `core.WeatherRepository` — `getSavedPlaceNames()`, `getPlaceDayCounts()`, `searchForLocations()`, `addPlaceFromSearch()`, `deletePlace()`.
- `core.Database` — used directly only for `cleanupForecastDays()` (currently disabled).
- `core.DatabaseExportImport` — export/import operations triggered from the debug menu.
- `core.WeatherApiKeyManager` — `hasValidKey()` checked on init and on return from Settings.
- `core.NetworkException` — distinguishes 401/403 errors for the `VcKeyErrorType` banner.

---

## Key interfaces

### HomeViewModel public API
```kotlin
val uiState: StateFlow<HomeUiState>
val searchText: StateFlow<String>
val navigationEvent: Flow<NavigationEvent>    // NavigationEvent.ToPlaceSummary

fun refreshWeatherKeyState()
fun onSearchTextChange(text: String)
fun onSearchResultSelected(place: GeoSearchResult)
fun onSavedPlaceSelected(placeName: String)
fun onErrorDismissed()
fun onVcKeyErrorDismissed()
fun onDeleteRequest(placeName: String)
fun onDeleteCancelled()
fun onDeleteConfirmed()
fun toggleDebugMenu()
fun exportDatabase()
fun listBackups()
fun onImportFilePathChange(path: String)
fun importDatabase()
```

### HomeUiState (data class)
| Field | Type | Notes |
|---|---|---|
| `placeDisplayData` | `List<PlaceDisplayData>` | Name, subtitle ("N days stored"), status dot |
| `searchResults` | `List<GeoSearchResult>` | Live geocoding results |
| `isSearching` | `Boolean` | Spinner while search in flight |
| `error` | `String?` | Generic dismissible error |
| `vcKeyError` | `VcKeyErrorType?` | MISSING or INVALID; drives Settings CTA banner |
| `showDeleteConfirmation` | `Boolean` | Delete dialog visibility |
| `placeToDelete` | `String?` | Staged place name |
| `showDebugMenu` | `Boolean` | Debug panel visibility |
| `debugMessage` | `String` | Debug panel output text |
| `importFilePath` | `String` | User-typed import path |
| `isWeatherKeyConfigured` | `Boolean` | Drives onboarding nudge visibility |
| `alertBanners` | `List<AlertBanner>` | Dismissible banners above place list |
| `isLoading` | `Boolean` | Skeleton row visibility |

### PlaceDisplayData
```kotlin
data class PlaceDisplayData(name: String, subtitle: String, status: PlaceStatus)
// PlaceStatus: NORMAL | WARNING | ERROR
```

---

## Known constraints

- Place rows with zero stored days receive `PlaceStatus.WARNING`; there is no `ERROR` status assigned currently.
- `refreshWeatherKeyState()` must be called by the View when returning from the Settings tab to ensure the nudge banner disappears promptly. This is not automatic.
- The debug menu is always compiled in; it is hidden by a toggle, not by a build flag.

---

## Decisions log

- See `/docs/agent/decisions/` for records created after the bootstrap phase.
