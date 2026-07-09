# KIM-329: Day card shows gust wind instead of average top wind

**Status:** Done · **Priority:** No priority · **Labels:** Bug
**Created:** 2026-07-01T06:57:34.850Z · **Completed:** 2026-07-01T07:29:37.341Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-329/day-card-shows-gust-wind-instead-of-average-top-wind

## Description

On the monthly summary, the day card uses the gust wind value.

**Expected:** In the collapsed/closed state, the day card should show the **average** top wind, not the gust wind.

*Source: voice memo bug report ("Rewind, gust instead of avg").*

## Comments

### kimmo.myllyviita@gmail.com — 2026-07-01T07:29:44.665Z

Marcy (code-reviewer): PASS — collapsed day row now shows the day's average top wind (sustainedWindSpeed, peak of rolling 3h-window averages), not the gust (windgust). Field choice moved to the testable DayWeatherSummary.collapsedRowWindSpeed accessor, so the composable stays pure render (MV* clean). New DayWeatherSummaryTest covers both branches incl. the sustained-null-with-gust-present case (regression guard against falling back to gust); FormatUtilsTest updated for the formatGust→formatWindSpeed rename. testDebugUnitTest verified green locally. No Critical/Major findings. Removed in-review, set to Completed.

### kimmo.myllyviita@gmail.com — 2026-07-01T07:22:10.466Z

**Dev handoff (Randy)**

- **Branch:** `kimmomyllyviita/kim-329-day-card-average-top-wind`
- **PR:** https://github.com/kmyllyvi/ReWinds/pull/60
- **Build:** pass (`./gradlew buildAndroidOnly`)
- **Tests:** pass (`./gradlew :composeApp:testDebugUnitTest`)
- **New tests written:** yes — `composeApp/src/commonTest/kotlin/place/DayWeatherSummaryTest.kt` (collapsed-row wind = sustained, not gust; null handling); updated `FormatUtilsTest` for the `formatGust` → `formatWindSpeed` rename.

**Summary:** The collapsed day summary row showed the momentary gust (`windgust` → `maxWindSpeed`). It now shows the day's average top wind (`sustainedWindSpeed`) via a new testable `DayWeatherSummary.collapsedRowWindSpeed` accessor; the composable just renders it. Renamed the generic km/h formatter `formatGust` → `formatWindSpeed`.

**Deviations / notes:** Confirmed in code that "closed"/collapsed state = `DaySummaryRow`; the "open" state is `DayDetailSheet` (hourly chart), unchanged. "Average top wind" is interpreted as `sustainedWindSpeed` — the highest rolling-average over the sustained-wind window — the same value the daily chart and day-of-interest matching already use, so the row is now consistent with them. This issue was in Backlog (no `spec-ready`) but was explicitly dispatched by Kimmo; moving to In Progress + in-review.

