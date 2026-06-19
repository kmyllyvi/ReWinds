# Home Module

**Last updated:** 2026-06-19 (PR #45 — KIM-309)
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

Note: VC key onboarding is no longer a Home responsibility. As of KIM-309 the hard gate lives in `core.Router` / `onboarding.VcKeyOnboardingViewModel` and blocks the Home surface entirely until a key is configured. The previous soft `VcKeyNudgeBanner` and `HomeViewModel.refreshWeatherKeyState()` / `HomeUiState.isWeatherKeyConfigured` have been removed.

---

## Dependencies

### Internal
- `core.WeatherRepository` — `getSavedPlaceNames()`, `getPlaceDayCounts()`, `searchForLocations()`, `addPlaceFromSearch()`, `deletePlace()`.
- `core.Database` — used directly only for `cleanupForecastDays()` (currently disabled).
- `core.DatabaseExportImport` — export/import operations triggered from the debug menu.
- `core.NetworkException` — distinguishes 401/403 errors for the `VcKeyErrorType` banner.

---

## Key interfaces

### HomeViewModel public API
```kotlin
val uiState: StateFlow<HomeUiState>
val searchText: StateFlow<String>
val navigationEvent: Flow<NavigationEvent>    // NavigationEvent.ToPlaceSummary

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

`refreshWeatherKeyState()` was removed in KIM-309. VC key state is now owned exclusively by `onboarding.VcKeyOnboardingViewModel`.

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
| `alertBanners` | `List<AlertBanner>` | Dismissible banners above place list |
| `isLoading` | `Boolean` | Skeleton row visibility |

`isWeatherKeyConfigured` was removed in KIM-309; it is no longer part of `HomeUiState`.

### PlaceDisplayData
```kotlin
data class PlaceDisplayData(name: String, subtitle: String, status: PlaceStatus)
// PlaceStatus: NORMAL | WARNING | ERROR
```

---

## Known constraints

- Place rows with zero stored days receive `PlaceStatus.WARNING`; there is no `ERROR` status assigned currently.
- The debug menu is always compiled in; it is hidden by a toggle, not by a build flag.
- `HomeView` is never in the composition while the VC key gate is active. The gate in `Navigation()` returns early and only renders `AppTabs` (which includes `HomeView`) once `VcKeyOnboardingViewModel.isWeatherKeyConfigured` is true.

---

## Decisions log

- `/docs/agent/decisions/2026-06-19-hard-gate-vc-key-onboarding.md` — why the soft banner was removed and replaced with a hard gate at the router level.
