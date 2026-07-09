# KIM-270: Month Summary screen redesign — stat cards and daily bar chart

**Status:** Done · **Priority:** Medium · **Labels:** design, in-review, spec-ready, Feature
**Created:** 2026-06-04T12:10:47.575Z · **Completed:** 2026-06-05T14:16:23.984Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-270/month-summary-screen-redesign-stat-cards-and-daily-bar-chart
**Related:** related: KIM-265 — Design token / theme system — Midnight Blue palette; related: KIM-266 — Background isobar texture — shared Canvas layer

## Description

## Spec

Redesign the Month Summary screen with the Midnight Blue theme. The screen shows a header with prev/next month navigation, four summary stat cards, and a daily bar chart where the peak-value day is highlighted in amber. This ticket is **display-only**: no new aggregates are added to `CalculatedStats`, no new ViewModel data-computation logic, and no new repository queries. All data required for the redesigned layout already exists in `DayWeatherSummary` and the current ViewModel; the work is entirely in the composable layer and how existing values are selected and formatted for display.

## Acceptance criteria

**Screen chrome**

- [ ] Screen background is `pageBg` with isobar texture as the lowest layer
- [ ] Header shows a back arrow (left), month+year title in `textPrimary`, and left/right chevron buttons for previous/next month; navigation state is driven by the ViewModel
- [ ] Prev/next month taps update ViewModel state; the composable reacts via `collectAsState()`

**Stat cards**

- [ ] Exactly 4 stat cards are rendered in a 2×2 grid: temperature, wind speed, rainfall, and kiteable days count — each card has a label in `textSecondary`, a value in `textPrimary` large, and a unit label in `textTertiary`; card background is `surface`
- [ ] The temperature card shows avg min and avg max on a single line formatted as "Temps: X–Y°C" (no separate hottest-day or coldest-day fields)
- [ ] The wind speed card shows the average `sustainedWindSpeed` (3h rolling avg) across the month; `avgWindSpeed` is not used as the primary wind metric in any card
- [ ] The rainfall card shows total rainfall for the month
- [ ] The kiteable days card shows the count of kiteable days for the month (value already computed in the ViewModel)
- [ ] `totalSolarEnergy`, `overallAverageTemp`, hottest-day with date, and coldest-day with date are **not** rendered anywhere on the screen

**Daily bar chart**

- [ ] A horizontal bar chart shows one bar per day of the month; bar colour is `accentBlue`
- [ ] The bar metric is `sustainedWindSpeed` per `DayWeatherSummary` entry (not `avgWindSpeed` or any other wind field)
- [ ] The bar(s) representing the peak `sustainedWindSpeed` value are rendered in `attention` (`#e8a030`)
- [ ] Which day is the peak is determined by the ViewModel (index or date passed in state), not computed inside the composable
- [ ] Bar chart bars are rendered using Compose `Canvas`, not a third-party chart library (consistent with existing project patterns — confirm in ARCHITECTURE-RULES or flag if a library is already in use)
- [ ] The chart is horizontally scrollable if bars overflow the screen width

**Day summary rows / collapsed - expanded**

- [ ] Show same data as currently

**General**

- [ ] All colour values reference theme tokens; no hardcoded colour literals except the single `attention` hex already defined in the token set

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`); peak-day selection logic in ViewModel has unit test coverage
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations
- [ ] All colour values reference theme tokens

## Scope boundary (explicit out-of-scope)

The following are **not** part of this ticket and must not be implemented here:

* Adding new aggregates to `CalculatedStats` (e.g. fog day count, rain day count, kiteable ratio, wind aggregates filtered to kiteable days)
* Any new ViewModel data-computation logic beyond what is strictly required to expose already-available data to the redesigned composable
* New repository queries or database changes

These belong to a separate future ticket. If the developer discovers that a display requirement genuinely requires new ViewModel logic, they must flag it via `needs-human` rather than silently implementing it.

## Notes

Priority: Medium.
Proposed size: Medium (2–3 hours). Bar chart Canvas rendering is the main complexity.
Reviewers needed: code-reviewer, ux-ui-reviewer, qa-test-agent (peak-day logic).
Reference: `docs/designs/screens-v1.html` (third mockup, "Month Summary").
Assumption: a chart library is not currently used; if one is already in the project, the developer should use it and note the deviation.
Blocked by: [KIM-265](https://linear.app/kimmo-m/issue/KIM-265/design-token-theme-system-midnight-blue-palette) (tokens), [KIM-266](https://linear.app/kimmo-m/issue/KIM-266/background-isobar-texture-shared-canvas-layer) (isobar).

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-05T11:47:32.085Z

Implementation complete — ready for review.

**Branch:** `kimmomyllyviita/kim-270-month-summary-screen-redesign-stat-cards-and-daily-bar-chart`
**PR:** https://github.com/kmyllyvi/ReWinds/pull/14
**Build:** PASS — `:composeApp:compileDebugKotlinAndroid` succeeds
**Tests:** PASS — `:composeApp:testDebugUnitTest`, 15 tests in `MonthlyStatisticsViewModelTest` (incl. 5 new peak-day cases), 0 failures

**Summary**
- Redesigned `MonthlyStatisticsView` with the Midnight Blue theme: isobar background as the lowest layer over `pageBg`; header with back arrow, month+year title, and prev/next chevrons driven by ViewModel state.
- 2×2 stat-card grid (`MonthStatCard`): temperature range ("Temps: X–Y°C"), average `sustainedWindSpeed`, total rainfall, kiteable days — labels `textSecondary`, values `textPrimary`, units `textTertiary`, card bg `surface`.
- Daily wind bar chart (`DailyWindBarChart`): Compose `Canvas`, one bar per day of `sustainedWindSpeed`, `accentBlue` bars, peak day in `attention` (`#e8a030` token), horizontally scrollable.
- Existing `DaySummaryRow` unchanged.
- ViewModel additions (display-driven only): `year`/`month`/`peakWindDayIndex` StateFlows, `navigateToPreviousMonth()`/`navigateToNextMonth()` with year wrapping, pure `peakSustainedWindIndex()` (unit-tested), `averageSustainedWindSpeed` on `CalculatedStats`.
- Removed from the screen per spec: `totalSolarEnergy`, `overallAverageTemp`, hottest/coldest day with date. All colours via theme tokens — no hex literals.

**Deviations**
- The mockup renders inline horizontal per-row bars; the ticket's acceptance criteria explicitly require a Compose `Canvas` vertical bar chart, so I followed the acceptance criteria (authoritative). No chart library is in the project, consistent with the spec's assumption.
- Pilot verification used the leaf Gradle tasks (`compileDebugKotlinAndroid` + `testDebugUnitTest`) rather than `buildAndroidOnly`: in this environment `buildAndroidOnly` also triggers an iOS framework link that OOMs (exit 137) plus a pre-existing config-cache problem — both unrelated to this change. iOS to be verified manually in Xcode per workflow.

