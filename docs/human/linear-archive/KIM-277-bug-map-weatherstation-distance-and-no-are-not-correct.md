# KIM-277: Bug: map weatherstation distance and no are not correct

**Status:** Done · **Priority:** No priority · **Labels:** in-review
**Created:** 2026-06-06T12:45:46.751Z · **Completed:** 2026-06-07T08:10:05.498Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-277/bug-map-weatherstation-distance-and-no-are-not-correct

## Description

for example, Konstanz says 10 weather stations, closest 2177km away!

Helsinki: 4 stations, closest 16360km away 🙂

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-06T13:08:28.971Z

Investigated and fixed.

**Root cause:** Visual Crossing reports station `distance` in **metres**, but `PlaceSummaryViewModel.toMapSummary()` treated it as kilometres and only rounded for display (a stale comment even claimed "distances are already in km"). So a real ~2177 m Konstanz station rendered as "2177 km"; Helsinki's 16360 m as "16360 km". The station **count** was actually correct — the absurd distances just made the whole panel look broken.

**Fix:** Convert metres → km (÷1000) in `toMapSummary()` before formatting to one decimal. Raw distance stays in metres through the model/storage layers (no migration).

**Tests:** Existing summary tests used km-scale fixtures (e.g. `distance = 2.34`) that hid the bug — updated to metre-scale values, plus a new regression test using a real API value (Konstanz 9705 m → "9.7" km).

- Branch: `kim-277-station-distance-meters-to-km`
- PR: https://github.com/kmyllyvi/ReWinds/pull/20
- Build: Android compile OK. `buildAndroidOnly` fails on a pre-existing configuration-cache error (`Task.project` at execution time) unrelated to this change.
- Tests: PASS (`./gradlew :composeApp:testDebugUnitTest`, full suite re-run)
- Deviations: none. iOS not built via Gradle per project convention — verify manually in Xcode (display-only change, no platform-specific code).

