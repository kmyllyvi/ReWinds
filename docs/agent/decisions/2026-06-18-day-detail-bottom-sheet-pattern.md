# Day Detail ModalBottomSheet Pattern and DaySummaryRow MV* Fix

**Date:** 2026-06-18
**PR:** #39
**Ticket:** KIM-303
**Status:** Active

## Context

The `DaySummaryRow` composable held its own `expanded` state (`mutableStateOf`) and rendered an `AnimatedVisibility` block with additional day metrics inline. This violated the MV* rule (business/UI state must live in the ViewModel, not in composables). Additionally, the inline expand detail was limited in space and could not accommodate a richer hourly wind chart.

## Decision

- Remove all local state and the inline expand/collapse block from `DaySummaryRow`. The row becomes a stateless collapsed card with an `onClick` callback.
- Add `selectedDay`, `selectedDayHours`, and `isLoadingHours` `StateFlow`s to `MonthlyStatisticsViewModel` to own the selection state.
- Render the day detail as a `ModalBottomSheet` (`DayDetailSheet`) overlaid on the `MonthlyStatisticsView` composable, driven by `selectedDay` nullability.
- Resolve hourly wind points on demand from the already-loaded `WeatherResponse` (`loadedData`) via the pure `hourlyWindWindow()` function; no new repository method was added.

## Rationale

A `ModalBottomSheet` provides enough vertical space for the dual-series `HourlyWindChart` Canvas component. Ownership of selection state in the ViewModel means the sheet survives Compose recomposition and is dismissible from the ViewModel on month reload or download without the composable needing to coordinate. Moving state to the ViewModel also makes the sheet behaviour unit-testable without a Compose environment (see `MonthlyStatisticsViewModelSelectDayTest`).

Reusing `loadedData` (the `WeatherResponse` cached from the last `getSavedDataFor()` call) avoids a new repository method and a second DB round-trip. `Day.hours` is populated by the full-load path, so no schema change was needed.

## Consequences

- `DaySummaryRow` is now fully stateless and its signature requires an `onClick: () -> Unit` parameter. Any future caller must supply this.
- The ViewModel constructor type for settings was changed from `AppSettingsRepository` (concrete) to `AppSettingsStore` (bound interface), enabling unit tests with a fake store and no DB.
- The previously inline day metrics (wind direction, rainfall, sunrise/sunset, solar energy, fog) are no longer shown in the collapsed row. They are not currently shown in the detail sheet either — the sheet shows only the hourly wind chart. Future tickets may add them back to the sheet.
- The now-unused `AppStrings` keys (`expand`, `collapse`, `dayMinTemp`, etc.) were left in place in KIM-303 to keep the diff scoped; they are dead strings until explicitly removed.
- Beaufort background bands are deferred to a future ticket per the KIM-303 spec.
