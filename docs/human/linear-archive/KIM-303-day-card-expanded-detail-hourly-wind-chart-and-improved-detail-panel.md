# KIM-303: Day card expanded detail: hourly wind chart and improved detail panel

**Status:** Done · **Priority:** High · **Labels:** in-review, spec-ready, Feature
**Created:** 2026-06-16T08:30:05.494Z · **Completed:** 2026-06-18T07:03:50.695Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-303/day-card-expanded-detail-hourly-wind-chart-and-improved-detail-panel

## Description

## Spec

When a user taps a day row in Monthly Statistics, a `ModalBottomSheet` opens showing an hourly wind chart for that day. The chart is a **line chart** (not a bar chart) displaying two series — wind speed and wind gust — plus a per-hour wind direction row below the x-axis, for the 12-hour window 09:00–21:00 in the location's local time (derived from the stored `tzoffset` on `WeatherResponse`). The current inline expanded content (description, min/max temp, wind direction, rainfall, sunrise/sunset, solar energy, fog warning) is removed entirely and replaced by the modal sheet. The feature ships on both Android and iOS.

Hourly data is already stored in the `Hour` DB table (`windspeed`, `windgust`, `winddir`, `datetime`) and the query `getHoursForDay` exists. The `WeatherResponse` carries `tzoffset: Double?` (UTC offset in hours), which is sufficient to filter 09:00–21:00 local time without any third-party timezone library. `DayWeatherSummary` does not currently carry hourly rows — a new on-demand load path is needed in `MonthlyStatisticsViewModel`.

**Design reference:** Windfinder hourly panel (see UX comment screenshot attached to this issue).

### Design decisions (from Mr.T, UX review)

**Colours (Midnight Blue dark theme)**

* Wind speed line: `#8ECFF0` — `accentBlue` token
* Wind gust line: `#E8A030` — `attention` token (not red; red = error semantics)
* Chart background: transparent — `ModalBottomSheet` surface `#0D1B2E` shows through
* Grid lines: `#1A3050` — `border` token
* Axis labels: `#7AB8D8` — `textSecondary` token, `labelSmall` type scale
* Legend: two items below the chart, filled circle swatches (8dp diameter), `textPrimary` `#DDEEF8`

**Chart type:** dual-series line chart (speed + gusts). No bars.

**Wind direction row:** a small rotating arrow icon placed below the x-axis time labels. The arrow is rotated by the raw bearing value in degrees via `Modifier.rotate(degrees)`, which is more precise than snapping to compass-text buckets. One arrow per available hour. Arrow: 12dp visual size, 18dp touch area, tint `#4A7A9B` (`textTertiary` token). Each arrow carries `contentDescription = "Wind direction: ${degreesToCompass(degrees)}"`.

**Beaufort bands:** out of scope for v1. No background fill on the chart. See Notes for v2 candidate.

## Acceptance criteria

**Data layer**

- [ ] `MonthlyStatisticsViewModel` exposes a `selectedDayHours: StateFlow<List<HourlyWindPoint>>` (or equivalent named type) that is populated when a day is selected and cleared when the sheet is dismissed. `HourlyWindPoint` holds at minimum: local-time label (string, e.g. "09:00"), `windspeed: Double?`, `windgust: Double?`, `winddir: Double?`.
- [ ] `MonthlyStatisticsViewModel` exposes a `selectedDay: StateFlow<DayWeatherSummary?>` that drives sheet visibility: non-null means open, null means closed.
- [ ] When a day is selected, the ViewModel loads hourly rows for that day from the DB via `getHoursForDay`, applies the 09:00–21:00 local-time filter using `WeatherResponse.tzoffset`, and emits exactly the matching hours (0–12 rows) into `selectedDayHours`. If `tzoffset` is null, treat it as 0.0 (UTC).
- [ ] The 09:00–21:00 filter is implemented in the ViewModel (or a pure function called by the ViewModel), not in a Composable.
- [ ] `DayWeatherSummary` is not extended with raw hourly rows; hourly data is fetched on demand per selected day, not pre-loaded for all days.
- [ ] A unit test covers the 09:00–21:00 local-time filter function: given a list of `Hour` rows with varied `datetimeEpoch` values and a known `tzoffset`, only the in-window hours are returned.
- [ ] A unit test covers `selectedDay` state: calling `selectDay(summary)` emits the summary; calling `dismissDaySheet()` emits null.

**Interaction**

- [ ] Tapping a `DaySummaryRow` card (anywhere on the card, not just the expand icon) calls a ViewModel method (e.g. `selectDay(summary)`) — no mutable state created inside the Composable for this purpose.
- [ ] The expand/collapse `remember { mutableStateOf(false) }` inside `DaySummaryRow` is removed; expand is no longer inline — the card tap always opens the modal.
- [ ] The `ExpandMore`/`ExpandLess` icon button is removed from `DaySummaryRow`.
- [ ] Dismissing the sheet (swipe down or outside tap) calls `dismissDaySheet()` on the ViewModel.

**Modal sheet**

- [ ] The modal is implemented using `ModalBottomSheet` with `rememberModalBottomSheetState`, matching the established pattern in `StationMapModal` and `ChatSessionSwitcher`.
- [ ] The sheet header shows a single `titleMedium` date label (e.g. "Wed 11.") at 16dp top padding. The default `ModalBottomSheet` drag handle is used. No custom title bar.
- [ ] The sheet contains a dual-series line chart of hourly wind speed and wind gust (km/h) for the 12-hour window, with hour labels on the x-axis (e.g. "09", "10", … "21").
- [ ] The wind speed line is rendered in `accentBlue` (`#8ECFF0`) and the wind gust line in `attention` (`#E8A030`).
- [ ] A two-item legend row is shown below the chart: filled circle swatches (8dp), labels in `textPrimary` (`#DDEEF8`).
- [ ] Grid lines use the `border` token (`#1A3050`). Axis labels use `textSecondary` (`#7AB8D8`), `labelSmall` type scale. Chart background is transparent.
- [ ] Wind direction is shown per hour as a rotating arrow icon placed **below** the x-axis time labels (not above the chart). The icon is rotated by the raw bearing degrees via `Modifier.rotate(degrees)`. Visual size 12dp, touch area 18dp, tint `textTertiary` (`#4A7A9B`). Each arrow has `contentDescription = "Wind direction: ${degreesToCompass(degrees)}"`.
- [ ] The chart Canvas block carries `Modifier.semantics { contentDescription = "Hourly wind chart for ${date}. Speed range ${minSpeed}–${maxSpeed} km/h, gusts up to ${maxGust} km/h." }`.
- [ ] If fewer than 12 hourly rows exist in the window (partial data), the chart renders only the available hours with no interpolation, no fabricated data, and no crash. The x-axis still spans 09:00–21:00 but line segments are drawn only between consecutive available data points.
- [ ] If zero hourly rows exist for the selected day, the sheet shows a centred label "No hourly data available for this day" in `textTertiary` with the date as a subtitle in `textSecondary`. No empty chart frame is rendered. The sheet still opens.
- [ ] While hourly data is being fetched asynchronously, a `CircularProgressIndicator` (tint: `accentBlue`) is shown centred in the chart area. No skeleton layout.
- [ ] There is no tap-to-see-value overlay in v1. The chart is read-only; no tooltip or hit-testing.
- [ ] The new hourly chart Composable is a separate file from `DaySummaryRow` and from `DailyWindBarChart`; it does not reuse `DailyWindBarChart` (which is a monthly aggregate chart).
- [ ] All number formatting in the chart Composable uses `formatDecimal` from `core.utils.FormatUtils` — no `String.format` or `"%.1f".format(...)` calls.

**Platform**

- [ ] The sheet opens and dismisses correctly on both Android (emulator) and iOS (simulator in Xcode). No platform-specific code is required beyond what `ModalBottomSheet` already handles in CMP.

*MV* compliance*

- [ ] No `remember { mutableStateOf(...) }` is introduced in any Composable for sheet-open state or selected-day state.
- [ ] The hourly filter logic (09:00–21:00 window, `tzoffset` application) has no implementation in any Composable.

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit for all non-trivial logic (ViewModel, Repository, pure functions, bug fixes). No "tests later". Exempt only: pure UI styling, config/doc-only changes — see AC for this ticket's exemption status.
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations

## Notes

Priority: High (set by Shirley; Kimmo reranks freely).

Size: M. One data-layer change (on-demand hourly load + filter), one new Composable (hourly chart), one ViewModel state addition, one MV* fix in `DaySummaryRow`. Fits one focused developer session; no further split recommended.

Reviewers needed: code-reviewer (mandatory); ux-ui-reviewer (new chart component — visual review needed); qa-test-agent (data filter logic is non-trivial).

**Beaufort background bands: out of scope for v1.** V2 candidate: three zones (0–15, 15–25, 25+ km/h) using `accentBlue` and `attention` tokens at 8% opacity.

**Technical notes for Randy:**

* `tzoffset` is a `Double?` on `WeatherResponse` — already returned by `WeatherRepository.getSavedDataFor`. The ViewModel already holds the full `WeatherResponse`; pass `tzoffset` through to the filter function.
* The `Hour.datetime` field is a local-time string (e.g. `"09:00:00"`). The `Hour.datetimeEpoch` field is a Unix timestamp. Either can be used for the 09:00–21:00 filter; `datetimeEpoch + (tzoffset * 3600)` gives local epoch seconds, from which the local hour is `(localEpoch / 3600) % 24`. Use whichever is most testable.
* `DailyWindBarChart` in `place/components/` is monthly aggregate — do not extend it.
* `ModalBottomSheet` precedent: see `place/StationMapModal.kt` and `ai/ChatSessionSwitcher.kt`.
* `degreesToCompass` helper already exists in `commonMain` — use it for direction arrow `contentDescription` strings (not as the visual label; the visual is the rotated arrow).
* `DaySummaryRow` currently violates MV* with `var expanded by remember { mutableStateOf(false) }`. Removing inline expand and replacing with the modal tap handler resolves this violation as a side-effect of this ticket.

**Assumption:** `WeatherRepository.getSavedDataFor` returns hourly rows already attached to each `Day` (the `Day.hours` field is populated from the DB). If that is not the case (hours are null for days loaded from the summary path), Randy should extend the repository to load hours on demand via a separate `getHoursForDay(dayId)` call, triggered by `selectDay`.

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-16T11:12:05.219Z

**Randy handoff — KIM-303**

- **Branch:** `kimmomyllyviita/kim-303-day-card-expanded-detail-hourly-wind-chart-and-improved`
- **PR:** https://github.com/kmyllyvi/ReWinds/pull/39 (→ `develop`)
- **Build:** PASS — Android compiles + assembles; lint clean (0 issues). The `buildAndroidOnly` convenience task surfaces a pre-existing Gradle config-cache warning (`Task.project` at execution time) unrelated to this change; `:composeApp:test`/`:lint`/`:assembleRelease` all ran green.
- **Tests:** PASS — `HourlyWindWindowTest` (8) + `MonthlyStatisticsViewModelSelectDayTest` (3), 0 failures. Full `testDebugUnitTest` suite green.

**Summary**
- Inline expand/collapse content in `DaySummaryRow` removed (incl. the `ExpandMore`/`ExpandLess` icon and `var expanded by remember { mutableStateOf(false) }`) — resolves the MV* violation. The whole card now calls `vm.selectDay(summary)`.
- New `ModalBottomSheet` (`DayDetailSheet`) with a `titleMedium` date header (16dp top padding, default drag handle) showing a dual-series line chart (`HourlyWindChart`): wind speed in `accentBlue`, gusts in `attention`, transparent canvas, `border` grid lines, `textSecondary` `labelSmall` axis labels, per-hour rotating direction arrows below the x-axis (`textTertiary`, 12dp visual / 18dp touch, `Modifier.rotate(degrees)`, compass `contentDescription`), two-item legend with 8dp circle swatches, and the required chart-Canvas semantics string. Loading (accentBlue spinner) / empty ("No hourly data…") / chart states handled. No `DailyWindBarChart` reuse.
- `MonthlyStatisticsViewModel` gains `selectedDay`, `selectedDayHours`, `isLoadingHours` StateFlows and `selectDay`/`dismissDaySheet`. The 09:00–21:00 window is a pure `tzoffset`-aware function (`hourlyWindWindow`) — no Composable logic. Hours load on demand from the already-loaded `WeatherResponse` (`Day.hours`); `DayWeatherSummary` is not extended.

**Deviations / decisions**
1. **Ticket was in Backlog**, not Todo/Planned/In Progress. It carries `spec-ready` and a feature branch was pre-named, and I was dispatched explicitly with the full spec, so I proceeded and am moving it to In Progress now. Flagging per the out-of-turn rule.
2. **VM ctor type change:** `AppSettingsRepository` → `AppSettingsStore` (the interface it already only used `getString` from). DI already binds the concrete impl as `AppSettingsStore`, so wiring is unchanged; this makes the VM unit-testable without a live DB (needed for the `selectDay` state test).
3. **Repository unchanged:** the spec's primary assumption held — `getSavedPlaceFull` populates `Day.hours` — so no `getHoursForDay(dayId)` on-demand path was added.
4. **Dead strings left in place:** the now-unused `DaySummaryRow` strings (`expand`, `collapse`, `dayMinTemp`, etc.) were intentionally not removed to keep the diff scoped; flagging for a possible cleanup.
5. **iOS** verified manually-in-Xcode per workflow (no Gradle iOS build run); no platform-specific code added.

Adding `in-review`.

### kimmo.myllyviita@gmail.com — 2026-06-16T08:48:02.189Z

example chart from windfinder

![Screenshot 2026-06-16 at 10.47.56.png](https://uploads.linear.app/22579512-5822-406d-914b-3c8e817d1106/71389a26-20d8-4fc5-aa93-a01f66aeb80b/227de81e-5dd9-4d96-8b76-74257f5ba6a0)

