# KIM-302: Improve day breakdown and card

**Status:** Done · **Priority:** High · **Labels:** in-review, spec-ready, Feature
**Created:** 2026-06-16T08:26:42.075Z · **Completed:** 2026-06-16T17:26:37.469Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-302/improve-day-breakdown-and-card
**Related:** blocks: KIM-303 — Day card expanded detail: hourly wind chart and improved detail panel

## Description

## Spec

The collapsed day card in the Monthly Statistics view shows `YYYY-MM-DD`, `avgTemp°C`, and `avgWindSpeed km/h`. This ticket upgrades the header row to show friendlier, more useful data: a short weekday+day label (`Mon 13.`), a temperature range (`min–max °C`), and the peak gust speed (`windgust km/h`). No layout changes to the expanded section — that is scoped in KIM-303.

The data is already present in `DayWeatherSummary` (`date`, `minTemp`, `maxTemp`, `maxWindSpeed`/gust). This is a pure display-layer change: a formatting helper in the ViewModel or a utility function, plus a Composable update.

## Acceptance criteria

- [ ] The collapsed card header shows a short day label formatted as `"Mon 13."` (three-letter weekday abbreviation, day-of-month, trailing period) derived from the `DayWeatherSummary.date` string (`YYYY-MM-DD`). When `date` is null or unparseable the label falls back to the raw date string (or `--` if that is also null).
- [ ] The collapsed card header shows temperature as `"min–max °C"` (e.g. `"12–19 °C"`), using `minTemp` and `maxTemp`. If either value is null it is rendered as `--` for that slot (e.g. `"--–19 °C"`).
- [ ] The collapsed card header shows the peak gust speed using `DayWeatherSummary.maxWindSpeed` formatted as a whole number followed by `"km/h"` (e.g. `"32 km/h"`). If `maxWindSpeed` is null, the field shows `"-- km/h"`.
- [ ] The collapsed card header no longer shows `avgTemp` or `avgWindSpeed` as primary fields (those remain visible only in the expanded section).
- [ ] The short-day label formatter (`"YYYY-MM-DD"` → `"Mon 13."`) lives in a ViewModel method or a `core/FormatUtils.kt` function — not inline in the Composable.
- [ ] The new formatter uses only KMP-safe APIs (no `String.format`, no JVM-only date classes — use string splitting and a weekday-offset calculation or a lookup; see `ARCHITECTURE-RULES.md`).
- [ ] The expanded section content is unchanged by this ticket (all existing fields still render correctly when the card is expanded).
- [ ] No new lint violations introduced.
- [ ] No tests required for the `remember { mutableStateOf(false) }` expand/collapse state in `DaySummaryRow` — this is UI-local toggle state with no business logic, exempt per ARCHITECTURE-RULES.md. New tests ARE required for the day-label and temperature-range formatting functions if they are extracted to a non-Composable utility or ViewModel.

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit for all non-trivial logic (ViewModel, Repository, pure functions, bug fixes). No "tests later". Exempt only: pure UI styling, config/doc-only changes — see AC for this ticket's exemption status.
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations

## Notes

Priority: High — this is a visible UX polish on a core screen that Kimmo already rated High; ranking confirmed.
Size estimate: S (1 session, roughly 1–2 hours). Pure display-layer change; all source data already in `DayWeatherSummary`.

Reviewers needed: code-reviewer; ux-ui-reviewer (Compose UI change on a user-facing card).

Scope split: The expanded card detail upgrade (hourly wind chart) is a separate, larger piece of work tracked in KIM-303. That ticket is parked at needs-human until Kimmo decides on wind chart scope.

Files expected to change:

* `composeApp/src/commonMain/kotlin/place/components/DaySummaryRow.kt` — collapsed header row
* `composeApp/src/commonMain/kotlin/core/FormatUtils.kt` (or a new utility) — day label formatter
* A new test file for the formatter if extracted to a utility function

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-16T08:52:50.553Z

Handover — ready for review

**Branch:** `kimmomyllyviita/kim-302-improve-day-breakdown-and-card`
**PR:** https://github.com/kmyllyvi/ReWinds/pull/38 (Closes KIM-302)
**Build:** PASS (compilation) — `:composeApp:assembleDebug` BUILD SUCCESSFUL. Note: `buildAndroidOnly` aggregator task fails on a pre-existing configuration-cache problem (reads `Task.project` at execution time) unrelated to this change; flagged as a follow-up.
**Tests:** PASS — `:composeApp:testDebugUnitTest` green, including 9 new formatter tests.

**Summary**
- Collapsed `DaySummaryRow` header now shows: short day label (`Mon 13.`), temperature range (`min–max °C`), peak gust (`32 km/h`).
- Removed `avgTemp` / `avgWindSpeed` from the collapsed header; they remain in the expanded section, which is otherwise unchanged.
- Three KMP-safe formatters added to `core/FormatUtils.kt`: `shortDayLabel`, `formatTemperatureRange`, `formatGust`. Weekday via Sakamoto integer math — no JVM date classes, no `String.format`.
- New tests in `FormatUtilsTest.kt`: known weekdays, leap day, year boundary, leading-zero day, unparseable/null fallbacks, temp-range missing slots, gust rounding/null.

**AC coverage:** day label with raw/`--` fallbacks ✓, temp range with `--` per-slot ✓, gust whole-number + `-- km/h` ✓, avg fields removed from collapsed header ✓, formatter in FormatUtils (not inline) ✓, KMP-safe ✓, expanded section unchanged ✓, formatter tests added ✓.

**Deviations:** none on scope. The only caveat is the pre-existing `buildAndroidOnly` config-cache failure (independent of this PR).

