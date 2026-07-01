---
name: display-layer-conversion
description: When the API stores data in one unit (km/h) and the UI must show another, conversion happens at display time only — no re-fetch, no schema change. Spec pattern for this class of ticket.
metadata:
  type: feedback
---

Display-layer unit conversion pattern (established KIM-330):

- Visual Crossing is always queried with `unitGroup=metric`; all DB values are metric (km/h, °C). Do not touch the network or DB layers.
- Conversion belongs in a pure top-level function in `FormatUtils.kt` (KMP-safe, no `String.format`), or in a ViewModel method. Never in a Composable.
- The user preference is persisted via `AppSettingsStore` (key/value string store backed by SQLDelight). Load on ViewModel `init`; default to the most-common unit on missing/unrecognised key.
- Hide settings rows whose effect is not yet implemented rather than showing dead controls — a missing row is better UX than a control that does nothing.
- For multi-screen unit labels: the ViewModel (or a shared helper) resolves the label string; Composable parameters already exist for this (e.g. `HourlyWindChart.unitLabel`).

**Why:** Dead controls erode trust. The app stores everything in metric; conversion is purely cosmetic.

**How to apply:** For any "unit preference not wired up" bug, scope to display-time conversion only. Confirm in Notes at Gate 1 whether filter thresholds (stored in km/h, e.g. `minWindSpeedKmh`) should also convert — that is usually a follow-on ticket.
