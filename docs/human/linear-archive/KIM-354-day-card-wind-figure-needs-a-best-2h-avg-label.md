# KIM-354: Day-card wind figure needs a "best 2h avg" label

**Status:** Done · **Priority:** Medium · **Labels:** _none_
**Created:** 2026-07-07T09:16:37.614Z · **Completed:** 2026-07-07T09:31:01.091Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-354/day-card-wind-figure-needs-a-best-2h-avg-label

## Description

## Spec

Originated from an ad-hoc bug/UX report: users see the monthly day-list card show a wind figure (e.g. "15 knots") that looks inconsistent with the day-detail hourly wind graph (which shows raw hourly average up to ~29 kn and gusts up to ~55 kn for the same day). This is **not a calculation bug** — the card shows `DayWeatherSummary.collapsedRowWindSpeed` (= `sustainedWindSpeed`), the max of a 2h rolling average of hourly average wind (`KiteSpotterConfig.SUSTAINED_WIND_WINDOW_HOURS = 2`), whereas the hourly graph plots raw unsmoothed per-hour average + gust. Both are correct for what they represent, but the card gives no hint that its number is a smoothed/sustained figure rather than a peak, so it reads as wrong.

Decision (confirmed by PO): keep the underlying value and calculation as-is; add a short label near the card's wind figure making clear it is a sustained-wind number, not a peak gust. Label text: "best 2h avg".

## Acceptance criteria

- [ ] The collapsed day-summary row (`DaySummaryRow`) shows a small "best 2h avg" caption beneath/near the wind value when a wind value is present.
- [ ] The label is suppressed when there is no wind value (nothing to qualify).
- [ ] The underlying `collapsedRowWindSpeed` / `calculateMaxSustainedWindSpeed` value and calculation are unchanged.
- [ ] Label is localized (English + German) via `AppStrings`, matching the existing localization pattern.
- [ ] Label uses design-system tokens (`MaterialTheme.rewinds.*` / typography), no hardcoded colours.
- [ ] No new tests required for the label rendering itself (pure Composable render of a static localized caption, no new logic); the existing `DayWeatherSummaryTest` continues to guard the sustained-vs-gust semantics.

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations
- [ ] Passes CI coverage gates

## Notes

Ad-hoc bug report, no prior ticket. Reviewers needed: code-reviewer + ux-ui-reviewer (small UI/copy change).

## Comments

### kimmo.myllyviita@gmail.com — 2026-07-07T09:30:39.692Z

**Dev handoff → review**

- **Branch:** `kimmomyllyviita/kim-354-day-card-wind-figure-label`
- **PR:** https://github.com/kmyllyvi/ReWinds/pull/68
- **Build:** pass (`./gradlew buildAndroidOnly`)
- **Tests:** pass (`./gradlew :composeApp:testDebugUnitTest`)
- **New tests written:** no — the change adds no logic. It renders a static, localized caption in a pure Composable (MV*-compliant; suppression is a trivial null check on the existing value). The sustained-vs-gust semantics of the underlying value remain guarded by the existing `DayWeatherSummaryTest`. Per AC, no new test required for the label render.

**Summary:** The monthly day-list card shows `collapsedRowWindSpeed` (max of a 2h rolling average of hourly average wind), while the day-detail hourly graph plots raw per-hour average + gust — so the card figure read as inconsistent. Added a small "best 2h avg" caption beneath the wind value on `DaySummaryRow` (localized EN "best 2h avg" / DE "bester 2-Std.-Ø", `labelSmall` + `MaterialTheme.rewinds.textTertiary`), shown only when a wind value is present. Underlying value and calculation are unchanged.

Files:
- `composeApp/src/commonMain/kotlin/place/components/DaySummaryRow.kt`
- `composeApp/src/commonMain/kotlin/core/AppStrings.kt`
- `docs/human/sections/test-checklist.html`

**Deviations:** none. Originated from an ad-hoc bug report (no prior ticket); KIM-354 was created for it.

**Manual test cases:** appended 1 row to `docs/human/sections/test-checklist.html` (verify the caption appears beneath the wind value, subtle colour, suppressed when no wind, DE copy).

CI is green for the head commit. Handed to Marcy (code-reviewer) in-session.

