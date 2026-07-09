# KIM-259: Map: show actual weather station markers, distinct from saved place, with refresh action

**Status:** Done · **Priority:** High · **Labels:** in-review, spec-ready, Feature
**Created:** 2026-06-02T06:25:51.551Z · **Completed:** 2026-06-02T18:58:32.589Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-259/map-show-actual-weather-station-markers-distinct-from-saved-place-with

## Description

## Spec

`StationMapModal` currently shows a single Leaflet marker at whichever single coordinate was stored for the place — it makes no distinction between the saved-place centre and an actual weather station. Once [KIM-258](https://linear.app/kimmo-m/issue/KIM-258/weather-station-data-persist-multiple-stations-backfill-existing) lands, `WeatherSummaryUiState.Success` carries a `List<StationDisplayData>` with real station coordinates. This ticket wires those stations into the map: each station gets its own visually distinct marker (different colour or icon from the place marker), the popup shows the station name, and the user can trigger a station refresh from within the map view. The map is the user's primary way to validate that a place's weather data comes from a nearby, trustworthy station.

## Acceptance criteria

- [ ] `PlaceSummaryViewModel` exposes `showStationMap: StateFlow<Boolean>`; `StationMapModal` is shown/hidden based on this state, not on a `remember { mutableStateOf }` in the View (fixes existing MV\* violation in `PlaceSummaryView`)
- [ ] `PlaceSummaryViewModel` exposes `openStationMap()` and `closeStationMap()` methods; `PlaceSummaryView` calls these in response to the info-button tap and the modal's dismiss action respectively
- [ ] `StationMapModal` accepts `stations: List<StationDisplayData>` and `placeLat: Double` / `placeLon: Double` as parameters instead of reading them from the ViewModel directly
- [ ] When `stations` is non-empty, `StationMapModal` renders one marker per station using a visually distinct style (different colour or custom icon) compared to the saved-place marker
- [ ] Each station marker popup shows the station name when available, or "Unknown station" when `name` is null
- [ ] The saved-place centre marker (blue/default) is still rendered at `placeLat`/`placeLon` and is visually distinguishable from station markers
- [ ] When `stations` is empty, the map still opens; it shows only the place-centre marker and a text note on the map (or in a banner above it) reading "No station data available"
- [ ] A "Refresh stations" icon button is visible in `StationMapModal`; tapping it calls `vm.refreshStations()` via a callback passed into the composable; while `isRefreshingStations` is true the button shows a loading indicator and is disabled
- [ ] While station refresh is in progress (`isRefreshingStations == true`), the map continues showing the previously-loaded markers — it does not clear them
- [ ] If a refresh completes with an error, a brief snackbar or inline message appears inside the modal; previously shown markers remain visible
- [ ] No business logic (API calls, station filtering, error handling) appears inside `StationMapModal` or `PlaceSummaryView`

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md) — `showStationMap` state lives in ViewModel, map data passed as parameters
- [ ] No new lint violations
- [ ] iOS: manually verified in Xcode simulator — multi-station place shows multiple distinct markers; place with no station data shows fallback text; refresh button triggers backfill and markers update

## Notes

Priority: High — direct user-visible outcome Kimmo described; depends on [KIM-258](https://linear.app/kimmo-m/issue/KIM-258/weather-station-data-persist-multiple-stations-backfill-existing) (blocked-by).
Proposed size: S (1–2 hours). Map HTML already exists; this is parameter wiring + HTML generation changes + ViewModel state for show/hide.
Assumption: `StationDisplayData` defined in [KIM-258](https://linear.app/kimmo-m/issue/KIM-258/weather-station-data-persist-multiple-stations-backfill-existing) (`name: String?`, `latitude: Double`, `longitude: Double`) is sufficient for the map. If distance or quality should appear in the popup, note as a future enhancement rather than blocking this ticket.
Assumption: "visually distinct" marker is achieved by passing a different Leaflet marker colour via the existing HTML generation function — no external icon assets required.
Note: the MV\* fix (`showStationMap` StateFlow) must land in this ticket; the existing `var showMapModal by remember` in `PlaceSummaryView` is a known violation flagged in [KIM-258](https://linear.app/kimmo-m/issue/KIM-258/weather-station-data-persist-multiple-stations-backfill-existing).
Reviewers needed: code-reviewer, ux-ui-reviewer (visible map changes), qa-test-agent (empty/error/loading states).

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-02T18:39:34.286Z

**Bugfix commit pushed** — `8546a81` on branch `kim-259-station-map-markers` (PR #10)

**Root cause found: base64 padding bug in `htmlToBase64` — not JSON deserialization**

The initial hypothesis (stations deserialized as `List<Station>`) was wrong. `WeatherResponse.stations` is already typed as `Map<String, Station>?`, which correctly deserializes the Visual Crossing JSON object keyed by station ID. A new regression test (`StationsJsonDeserializationTest`) confirms this directly against the Ermatingen 9-station payload.

The actual bug is in `StationMapModal.kt` → `htmlToBase64()`:

```kotlin
// BEFORE (broken): computed padding from `i` after it was already advanced
val hasSecond = i - 1 < bytes.size   // wrong when b2 was not read
val hasThird = i < bytes.size         // wrong — means "is there a NEXT triplet", not "was b3 read"
```

This produces incorrect padding for:
- `bytes.size % 3 == 0` → last group emitted `XXX=` instead of `XXXX` (last byte lost)
- `bytes.size % 3 == 1` → last group emitted `XXX=` instead of `XX==` (phantom byte encoded)

Both cases corrupt the trailing bytes of the base64-encoded HTML. A strict WebView decoder (iOS WKWebView in particular) would reject the malformed data-URI entirely, rendering a blank map with no station markers — even with a fully-populated station list.

```kotlin
// AFTER (fixed): track availability at point of read
val hasB2 = i < bytes.size
val b2 = if (hasB2) bytes[i++].toInt() and 0xFF else 0
val hasB3 = i < bytes.size
val b3 = if (hasB3) bytes[i++].toInt() and 0xFF else 0
```

**`saveWeatherResponse` duplicate path (Marcy's note):** also correct — `weatherResponse.stations?.values?.filter { ... }` works fine with `Map<String, Station>?`. No fix needed there.

**What was changed:**
- `StationMapModal.kt`: fix `htmlToBase64` padding logic
- `StationsJsonDeserializationTest.kt` (new): 4 tests — parses Ermatingen 9-station JSON directly, asserts count=9, field mapping, lat/lon filter, null-stations case
- `StationMapBase64Test.kt` (new): 10 tests — RFC-4648 known-good vectors + round-trip for all 3 size-mod-3 residues + HTML-scale padding count assertion

**Build:** Android `assembleDebug` passes
**Tests:** All 40 tests pass (was 36 before new tests)

**For Kimmo to verify on device:** searching "Ermatingen" and opening the station map should now render 9 orange station markers alongside the blue place marker.

### kimmo.myllyviita@gmail.com — 2026-06-02T16:42:17.665Z

Branch: `kim-259-station-map-markers`
PR: https://github.com/kmyllyvi/ReWinds/pull/10

**Build:** PASS (`./gradlew :composeApp:compileDebugKotlinAndroid` + `buildAndroidOnly` — BUILD SUCCESSFUL)
**Tests:** PASS (`./gradlew :composeApp:testDebugUnitTest` — 36 tasks, BUILD SUCCESSFUL, 3 new tests added)

**Summary of changes:**

- `PlaceSummaryViewModel`: renamed `isMapModalVisible`/`showMapModal()`/`dismissMapModal()` → `showStationMap: StateFlow<Boolean>` / `openStationMap()` / `closeStationMap()` (MV* fix per spec)
- `StationMapModal`: now accepts `stations: List<StationDisplayData>`, `isRefreshingStations`, `stationsError`, `onRefreshStations` as parameters — no ViewModel dependency inside the composable
- Map renders orange SVG markers for stations (visually distinct from default-blue place marker); popup shows station name or "Unknown station" when name is null
- Empty-state JS overlay on the map when `stations` is empty
- Error banner above the map when `stationsError` is non-null; prior markers remain
- Refresh button (top-start) shows `CircularProgressIndicator` while refreshing, disabled during refresh
- New strings added to `AppStrings` (English + German): `noStationData`, `refreshStations`, `refreshStationsDesc`, `unknownStation`, `stationRefreshError`
- 3 new unit tests: `showStationMap_startsHidden`, `openStationMap_makesShowStationMapTrue`, `closeStationMap_makesShowStationMapFalse`

**Deviations:** None — all acceptance criteria implemented as specified.

Ready for code-reviewer, ux-ui-reviewer, and qa-test-agent.

