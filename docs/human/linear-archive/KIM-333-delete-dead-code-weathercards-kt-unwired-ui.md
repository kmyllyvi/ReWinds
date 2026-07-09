# KIM-333: Delete dead code: WeatherCards.kt (unwired UI)

**Status:** Done · **Priority:** No priority · **Labels:** Improvement
**Created:** 2026-07-01T18:26:42.158Z · **Completed:** 2026-07-01T18:37:26.958Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-333/delete-dead-code-weathercardskt-unwired-ui
**Related:** related: KIM-330 — Settings: unit / wind-speed options don't do anything

## Description

`composeApp/src/commonMain/kotlin/place/components/WeatherCards.kt` is dead code. Its public composables `StoredDaysList` and `DayWeatherSummaryCard` (and the private `formatTemperature` / `InfoColumn` / `formatWindSpeed` helpers) have **no callers anywhere** in `composeApp/src` — verified by repo-wide grep during the [KIM-330](https://linear.app/kimmo-m/issue/KIM-330/settings-unit-wind-speed-options-dont-do-anything) review (Marcy) and re-verified 2026-07-01. Live wind readouts are rendered by `DaySummaryRow`, `MonthStatCard`, and `HourlyWindChart`.

**Action:** Delete `WeatherCards.kt` entirely. Fix any resulting compile breakage (there should be none, since nothing references it). Leave the `UnitSystem` enum, `SETTINGS_UNITS_ROW` testtag, and `settingsRowUnits` string alone — those are reserved for a future temperature-units ticket, out of scope here.

**Merge policy for this ticket (per Kimmo):** no code review needed — merge on green CI. This retires Marcy's `weathercards-dead-code` memo.

*Follow-up to* [KIM-330](https://linear.app/kimmo-m/issue/KIM-330/settings-unit-wind-speed-options-dont-do-anything)*.*

## Comments

### kimmo.myllyviita@gmail.com — 2026-07-01T18:37:33.180Z

Done — merged to `develop`.

- **Grep re-confirmation:** clean. `StoredDaysList` and `DayWeatherSummaryCard` (and the private `formatTemperature`/`InfoColumn`/`formatWindSpeed` helpers) appeared **only inside `WeatherCards.kt`** itself — zero external callers in `composeApp/src`.
- **Deleted:** `composeApp/src/commonMain/kotlin/place/components/WeatherCards.kt` (119 lines). No other changes needed — nothing referenced it, so no compile breakage.
- **Left in place (out of scope):** `UnitSystem` enum, `SETTINGS_UNITS_ROW` testtag, `settingsRowUnits` string.
- **Build/tests:** `./gradlew buildAndroidOnly` and `./gradlew :composeApp:testDebugUnitTest` both pass locally.
- **PR:** https://github.com/kmyllyvi/ReWinds/pull/66 — CI green (Android Tests, Android Instrumented/Compose semantic, iOS Unit Tests all pass). Merged (fast-forward) per the review-waived merge policy.
- **Manual test cases:** none (pure dead-code deletion, no user-visible effect).

Retires Marcy's `weathercards-dead-code` memo.

