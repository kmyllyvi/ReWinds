# KIM-258: Weather station data: persist multiple stations, backfill existing places, manual refresh

**Status:** Done · **Priority:** High · **Labels:** in-review, spec-ready, Feature
**Created:** 2026-06-02T06:25:24.304Z · **Completed:** 2026-06-02T18:58:23.358Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-258/weather-station-data-persist-multiple-stations-backfill-existing
**Related:** related: KIM-245 — First-run onboarding: Visual Crossing API key setup; blocks: KIM-259 — Map: show actual weather station markers, distinct from saved place, with refresh action

## Description

## Spec

The app currently stores only one station coordinate pair (the highest-useCount station) on the `WeatherResponse` row. Older/imported places have no station data at all because `&include=stations` was only added to `addPlaceFromSearch`. This ticket fixes the data layer so that: (a) all stations returned by the API are persisted per place, (b) places that are missing station data can be backfilled on demand, and (c) a manual refresh can re-fetch station data at any time. The map UI ticket (KIM-TBD) that renders multiple markers depends on this being in place first.

## Data handling strategy (plan)

**Where station data lives**
The existing `WeatherResponse` table carries `stationLatitude` / `stationLongitude` for a single primary station. Replace this with a new `WeatherStation` table (`resolvedAddress FK`, `stationId TEXT`, `name TEXT`, `latitude REAL NOT NULL`, `longitude REAL NOT NULL`, `distance REAL`, `quality INTEGER`, `useCount INTEGER`, `contribution REAL`). A SQLDelight migration increments the schema version and drops the two old columns.

**When stations are fetched**
Stations are fetched as part of `addPlaceFromSearch` (already uses `&include=stations`) and during the new backfill/refresh call. They are NOT re-fetched on every weather day download — station geometry is stable, so one fetch per place is enough unless the user explicitly refreshes.

**Backfill path**
On app init (or when the station list screen/map is first opened for a place), `PlaceSummaryViewModel` checks the new `WeatherStation` table for the place. If the result is empty, it triggers a lightweight dedicated API call — `verifyPlaceAndGetStations()` (already exists in `WeatherRepositoryImpl` but is currently unexposed) — which requests `last0days&include=stations`. The resulting station list is persisted. This call is made once automatically, and again whenever the user requests a manual refresh.

**Manual refresh**
`PlaceSummaryViewModel` exposes a `refreshStations()` method. It sets a `isRefreshingStations: Boolean` state flag to true, calls the repository to re-fetch, persists the result, then sets the flag back to false. The repository method for this must be added to the `WeatherRepository` interface so it is mockable in tests.

**Missing / empty / error cases**

* API returns empty `stations` map: persist zero rows; ViewModel exposes `StationsState.Empty`; map shows place-centre marker with a "No station data" note.
* API returns partial list (some stations missing lat/lon): persist only stations that have non-null lat and lon; silently skip the rest.
* Network error during backfill/refresh: ViewModel exposes `StationsState.Error(message)`; no previously-persisted stations are overwritten; user sees an inline error with a retry option.
* Stale data (previously persisted, now re-fetched): full replace — delete existing rows for the place and insert the new list within a single transaction.

`DataMapping` **changes**
`DataMapping.toWeatherResponse()` currently synthesises a fake single-entry stations map from the stored column pair. Once the new table exists, `getSavedPlaceFull()` joins `WeatherStation` rows and returns the real list. The synthetic reconstruction is removed.

## Acceptance criteria

- [ ] A new `WeatherStation` SQLDelight table exists with columns: `resolvedAddress` (FK → WeatherResponse, ON DELETE CASCADE), `stationId`, `name`, `latitude`, `longitude`, `distance`, `quality`, `useCount`, `contribution`; a migration drops `stationLatitude` and `stationLongitude` from `WeatherResponse`
- [ ] `DataMapping.fromWeatherResponse` no longer writes `stationLatitude` / `stationLongitude` to the `WeatherResponse` row
- [ ] `DataMapping.toWeatherResponse` no longer synthesises a fake single-entry stations map; instead it assembles stations from the joined `WeatherStation` rows
- [ ] `WeatherRepository` interface exposes `suspend fun fetchAndPersistStations(place: String): StationsResult` (sealed: Success(stations), Empty, Error)
- [ ] `WeatherRepositoryImpl.fetchAndPersistStations` calls Visual Crossing with `last0days&include=stations`, parses the `stations` map, filters out entries with null lat/lon, and upserts (delete-then-insert in one transaction) the `WeatherStation` rows for that place
- [ ] When `PlaceSummaryViewModel` loads a place and `WeatherStation` rows for it are absent, it automatically calls `fetchAndPersistStations` once before emitting the Success state
- [ ] `PlaceSummaryViewModel` exposes `isRefreshingStations: StateFlow<Boolean>` and `refreshStations()` method; calling `refreshStations()` re-fetches and persists stations regardless of whether rows already exist
- [ ] `WeatherSummaryUiState.Success` includes `stations: List<StationDisplayData>` (data class with at minimum `name`, `latitude`, `longitude`); the View reads this from state and does not query the repository directly
- [ ] When `fetchAndPersistStations` returns `Empty`, the ViewModel sets `stations` to an empty list (no crash, no infinite retry)
- [ ] When `fetchAndPersistStations` returns `Error`, previously-persisted station rows for the place are unchanged; the ViewModel exposes an error message the View can show
- [ ] `PlaceSummaryViewModel` unit tests cover: (a) auto-backfill fires when station table is empty for a place, (b) `refreshStations()` replaces existing stations, (c) empty-result case, (d) error case leaves prior stations intact

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`); new ViewModel logic has unit tests per AC above
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md) — no repository calls in Composables
- [ ] No new lint violations
- [ ] SQLDelight migration compiles cleanly and the schema version is incremented
- [ ] Manual smoke test on Android: add a new place → station rows appear in DB; open an existing place with no station data → auto-backfill fires and rows are written; refresh button re-fetches and overwrites rows

## Notes

Priority: High — this is the data foundation that the map marker UI (follow-on ticket) depends on. Milestone A scope (Kimmo confirms).
Proposed size: M (2–3 hours). Schema migration + repository interface change + ViewModel auto-backfill + unit tests.
Assumption: `verifyPlaceAndGetStations` in `WeatherRepositoryImpl` is the correct call shape; the ticket can rename/repurpose it as `fetchAndPersistStations` and expose it on the interface.
Assumption: `StationDisplayData` is a new lightweight UI model defined in `place/` package — not `core.Station` — to keep the layer separation clean.
Note: `PlaceSummaryView` currently holds `var showMapModal by remember { mutableStateOf(false) }` — a MV\* violation. This should be moved to the ViewModel as part of this ticket or the map-UI ticket; flag to developer to clean up whichever ticket picks it up first.
Reviewers needed: code-reviewer, qa-test-agent (data migration + four new test cases).

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-02T11:20:17.451Z

**Developer handover — KIM-258**

**Branch:** `kim-258-weather-station-data`
**PR:** https://github.com/kmyllyvi/ReWinds/pull/8
**Build:** pass (`./gradlew :composeApp:compileDebugKotlinAndroid` — BUILD SUCCESSFUL)
**Tests:** pass (`./gradlew :composeApp:testDebugUnitTest` — 251 tests, 0 failures)

**Summary of changes:**

1. **WeatherStation table** — new SQLDelight table (multi-row per place) with migration file `4.sqm` that recreates WeatherResponse without the old `stationLatitude`/`stationLongitude` columns.

2. **Repository layer** — `WeatherRepository` interface now exposes `fetchAndPersistStations(place): StationsResult` (sealed: Success/Empty/Error) and `getPersistedStations(place): List<Station>`. Impl calls `last0days&include=stations` with atomic delete-then-insert.

3. **ViewModel auto-backfill** — `PlaceSummaryViewModel.loadWeatherData()` checks the station table on every load; if empty it triggers `fetchAndPersistStations` once. `refreshStations()` and `isRefreshingStations: StateFlow<Boolean>` are exposed for user-triggered refresh.

4. **UI state** — `WeatherSummaryUiState.Success` carries `stations: List<StationDisplayData>` and `stationsError: String?`. View reads from state only.

5. **MV* fix** — `showMapModal` moved from `remember { mutableStateOf(false) }` in the View to `isMapModalVisible: StateFlow<Boolean>` on the ViewModel (flagged in ticket notes).

6. **Test coverage** — `PlaceSummaryViewModelStationTest` (4 new cases per AC), `DataMappingKIM149Test` rewritten for new architecture. Pre-existing `HomeViewModel` search exception leak fixed (wrapped `searchForLocations` call in try-catch to prevent `UncaughtExceptionsBeforeTest` cross-contamination).

**Deviations from spec:**
- None from the core AC. `HomeViewModel.kt` contains KIM-245's `isWeatherKeyConfigured` field as a side-effect of the overlapping working tree; it is harmless and will be cleaned up when KIM-245 is landed.
- iOS build not attempted by agent per CLAUDE.md; no `iosMain` changes were made.

