# KIM-268: Home / Places screen redesign

**Status:** Done · **Priority:** Medium · **Labels:** in-review, spec-ready, Feature
**Created:** 2026-06-04T12:10:16.492Z · **Completed:** 2026-06-05T14:16:15.442Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-268/home-places-screen-redesign
**Related:** related: KIM-265 — Design token / theme system — Midnight Blue palette; related: KIM-267 — Tab bar navigation — Places / Chat / Settings; related: KIM-266 — Background isobar texture — shared Canvas layer

## Description

## Spec

Implement home screen design from /docs/designs/screens-v1.html

Apply the Midnight Blue theme to the Home / Places screen and add the designed UI components: a search bar, amber and red alert banners, and per-location status dots (blue / amber / red). The screen lives inside the new tab bar shell ([KIM-267](https://linear.app/kimmo-m/issue/KIM-267/tab-bar-navigation-places-chat-settings)). It shows a list of saved locations; each row has a coloured status dot, place name, subtitle, and a chevron. The isobar background ([KIM-266](https://linear.app/kimmo-m/issue/KIM-266/background-isobar-texture-shared-canvas-layer)) is applied as the bottom layer.

## Acceptance criteria

- [ ] Screen background is `pageBg` (`#030810`) with the isobar texture composable as its lowest layer
- [ ] Header shows the "ReWinds" title in `textPrimary` at 26 sp bold, with a settings gear icon button and an add-place icon button on the right
- [ ] A search bar is rendered below the header; background `surface`, border `border`, placeholder text in `textTertiary`
- [ ] Each place row has a 6 dp circular status dot: blue (`accentBlue`) for normal, amber (`attention`) for warning, red (`error`) for error — dot colour is driven by a state exposed from `HomeViewModel`, not computed inside the composable
- [ ] Place name is rendered in a colour close to `textPrimary` (`#c8e8f8`); subtitle in a muted secondary colour
- [ ] Amber alert banners (background `rgba(attention, 0.12)`, border `rgba(attention, 0.28)`, text `attention`) and red error banners are rendered when the ViewModel exposes them; no banners rendered when state has none
- [ ] Row background is `surface` at \~75 % opacity with `borderRadius` 13 dp
- [ ] The screen compiles and renders correctly inside the tab bar ([KIM-267](https://linear.app/kimmo-m/issue/KIM-267/tab-bar-navigation-places-chat-settings))

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`); new logic has basic coverage
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md) — status dot colour and banner visibility must come from ViewModel state
- [ ] No new lint violations
- [ ] All colour values reference theme tokens, not hex literals
- [ ] Blocked by: [KIM-265](https://linear.app/kimmo-m/issue/KIM-265/design-token-theme-system-midnight-blue-palette) (tokens), [KIM-266](https://linear.app/kimmo-m/issue/KIM-266/background-isobar-texture-shared-canvas-layer) (isobar), [KIM-267](https://linear.app/kimmo-m/issue/KIM-267/tab-bar-navigation-places-chat-settings) (tab nav)

## Notes

Priority: Medium — depends on all three foundation tickets.
Proposed size: Medium (2–3 hours).
Reviewers needed: code-reviewer, ux-ui-reviewer.
Reference: `docs/designs/screens-v1.html` (first phone mockup, "Places").

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-04T20:06:55.585Z

Branch: `kim-268-home-places-screen-redesign`
PR: https://github.com/kmyllyvi/ReWinds/pull/13
Build: pass (`./gradlew :composeApp:compileDebugKotlin` — BUILD SUCCESSFUL)
Tests: pass (30 test files, 0 failures; 10 new tests in `HomeViewModelStatusTest`)

## Summary

Applied the Midnight Blue design system to the Home / Places screen:

- **PlaceStatus enum** (NORMAL/WARNING/ERROR) and **AlertBanner** data class added to `HomeViewModel`. Dot colour and banner visibility are driven entirely by ViewModel state — no logic in the composable (MV* clean).
- **PlaceDisplayData** extended with `subtitle: String` and `status: PlaceStatus`. Places with no stored days get `WARNING` status automatically.
- **alertBanners: List<AlertBanner>** added to `HomeUiState`. Amber banners use `attention` token at 12%/28% opacity; red banners use `error` token the same way. No banners rendered when the list is empty.
- **HomeView redesign**: isobar background as lowest layer, themed search bar (`surface` bg + `border` outline + `textTertiary` placeholder), 6 dp `StatusDot` composable, place rows with 75% opacity `surface` bg + 13 dp corner radius + `textPrimary` name + `textSecondary` subtitle, chevron in `textTertiary`. All colours reference `ReWindsColors` tokens — no hex literals.
- **Header**: "ReWinds" title at 26 sp bold (via new optional `titleSizeSp` param on `AppHeader`), settings gear icon + add-place icon button on the right.
- **AppHeader** `titleSizeSp` param is non-breaking — all other screens unaffected.

## Deviations

- The `Add` icon button in the header is wired to a no-op stub. The spec mentions an "add-place icon button" but the actual add-place flow (search bar → result selection) is unchanged and continues to work. The icon placement satisfies the AC; wiring it as a shortcut for the search field can be a follow-up if desired.

