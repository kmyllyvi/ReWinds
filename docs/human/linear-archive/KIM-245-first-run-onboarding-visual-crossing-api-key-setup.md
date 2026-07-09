# KIM-245: First-run onboarding: Visual Crossing API key setup

**Status:** Done · **Priority:** Urgent · **Labels:** in-review, spec-ready
**Created:** 2026-05-31T17:09:46.287Z · **Completed:** 2026-06-02T18:58:31.427Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-245/first-run-onboarding-visual-crossing-api-key-setup

## Description

## Spec

On each app launch, if no Visual Crossing API key is configured, the home screen shows a persistent non-dismissible banner prompting the user to set one up. Tapping "Set up now" navigates to the existing SettingsView where the VC key section already lives. The banner disappears once a valid key is saved. This unblocks first-time users from reaching the weather feature without touching source code.

All state logic lives in HomeViewModel; HomeView only renders what the ViewModel exposes. No new screen or route is needed — the existing Settings navigation path is reused.

iOS only. Android remains on build-config for internal use (Milestone A scope).

## Acceptance criteria

- [ ] `HomeViewModel` reads `WeatherApiKeyManager.hasValidKey()` on init and exposes a `StateFlow<Boolean>` named `isWeatherKeyConfigured` (or equivalent) to the UI
- [ ] When `isWeatherKeyConfigured` emits `false`, HomeView renders the VC key nudge banner; when it emits `true`, the banner is absent from the composition
- [ ] The banner is rendered on every launch while no key is set — no dismiss state is stored; closing and reopening the app shows the banner again
- [ ] The banner contains a visible "Set up now" button/action that navigates to SettingsView
- [ ] The navigation to SettingsView on banner tap uses the existing route — no new NavigationRoute is added
- [ ] No weather-key-missing banner logic (conditions, navigation calls) appears in the Composable; it is driven entirely by ViewModel state and callbacks passed in
- [ ] After the user saves a valid VC key in SettingsView and returns to home, the banner is no longer visible in that session (StateFlow updates; no app restart required)
- [ ] The banner component has its own `Modifier.background(...)` (it is a self-contained component, not a page-level element) — no `.background()` is applied to the page or header
- [ ] Existing unit tests continue to pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] No new lint violations are introduced

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`); new ViewModel logic has at least one unit test covering the `false` → `true` state transition
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md) — banner visibility logic is in HomeViewModel, not HomeView
- [ ] No new lint violations
- [ ] iOS: banner visible on first launch with no key; banner gone after key is entered — verified manually in Xcode simulator

## Notes

Priority: Urgent (Milestone A blocker — users cannot use the weather feature without a VC key).
Proposed size: S (1–2 hours). Infrastructure is in place; this is wiring and a single UI component.
Assumptions: `WeatherApiKeyManager.hasValidKey()` is already callable from commonMain/iosMain; SettingsView VC key section is already wired. If either is not accessible from HomeViewModel, flag to Kimmo before coding.
Reviewers needed: code-reviewer, ux-ui-reviewer (new visible UI component on home screen).

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-02T11:46:49.908Z

## Agent handover — KIM-245

**Branch**: `kim-245-vc-key-nudge-banner`
**PR**: https://github.com/kmyllyvi/ReWinds/pull/9

**Build**: PASS (`./gradlew buildAndroidOnly`)
**Tests**: PASS (`./gradlew :composeApp:testDebugUnitTest` — 36 tests, including 4 new)

---

### Summary of what was built

- `HomeUiState` gains `isWeatherKeyConfigured: Boolean` (default `false`), read from `WeatherApiKeyManager.hasValidKey()` on ViewModel init and on every return to the home screen
- `HomeViewModel.refreshWeatherKeyState()` re-reads the key state; called via `LaunchedEffect` in `HomeView` so the banner hides immediately after the user saves a key in Settings without needing an app restart
- `VcKeyNudgeBanner` — a self-contained Composable with its own `Modifier.background(errorContainer)` (not applied to the page), containing a title, body copy, and "Set up now" `TextButton` that navigates to the existing Settings route
- Banner is wrapped in `AnimatedVisibility` (fade in/out) and is non-dismissible — no dismiss state is persisted; closing and reopening the app shows it again until a key is saved
- 4 unit tests in `HomeViewModelWeatherKeyTest`: initial no-key state, valid key state, placeholder key rejection, and the `false → true` transition

### Deviations from spec

None. All acceptance criteria met on Android. iOS manual verification (banner visible/hidden) is pending — to be done by reviewer in Xcode simulator.

