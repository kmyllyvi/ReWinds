# Hard-Gate First-Run Onboarding for Visual Crossing API Key

**Date:** 2026-06-19
**PR:** #45
**Ticket:** KIM-309
**Status:** Active (supersedes KIM-245 soft banner)

---

## Context

The app requires a Visual Crossing (VC) API key to fetch weather data. Without it, every place-add attempt fails and the app is functionally unusable. KIM-245 addressed this with a non-dismissible `VcKeyNudgeBanner` rendered inside `HomeView` — a soft nudge that still allowed the user to reach (and be frustrated by) the broken Home surface. The banner relied on `HomeViewModel.refreshWeatherKeyState()` being called on screen entry to stay current, making the reactive state management split across two owners (HomeViewModel and WeatherApiKeyManager).

---

## Decision

Replace the soft banner with a full-screen hard gate implemented at the router level (`core.Router.Navigation()`). While `VcKeyOnboardingViewModel.isWeatherKeyConfigured` is false the `Navigation()` composable returns early after rendering `OnboardingGate`; the Home/tab surface (`AppTabs`) is never entered into the composition. The gate clears reactively via `WeatherApiKeyManager.hasValidKeyFlow` the moment a valid key is saved, without requiring a restart. There is no dismiss, skip, quit, or back affordance.

---

## Rationale

- **Correctness**: a user who has not configured a VC key cannot perform any meaningful action. Allowing them past the gate only generates error states and confusion.
- **Single ownership**: gate state is entirely owned by `VcKeyOnboardingViewModel` forwarding `WeatherApiKeyManager.hasValidKeyFlow`. `HomeViewModel` and `HomeUiState` no longer carry any VC key awareness; the Home module is simplified.
- **Reactive clearing**: `hasValidKeyFlow` (a `StateFlow<Boolean>` added to `WeatherApiKeyManager` in this PR) means the gate disappears in the same session without polling or manual refresh calls. The previous `refreshWeatherKeyState()` call-site discipline is eliminated.
- **Reuse**: the gate routes into the existing `SettingsView` VC key entry rather than duplicating key-entry UI. A gate-local `NavigatorImpl` wrapping a `gateSettingsStack` satisfies `SettingsView`'s `Navigator` contract without exposing the real tab stack.
- **Test isolation**: gate logic lives in a dedicated `VcKeyOnboardingViewModel` with its own test file (4 tests); no changes to `HomeViewModel` tests were needed beyond removing the now-deleted `HomeViewModelWeatherKeyTest`.

---

## Consequences

- **Easier**: the app never reaches a broken state for unconfigured users; Home module is simpler with no key-state concern.
- **Constrained**: on Android the VC key is typically set at build time via `BuildConfig`, so the gate is never reached in practice on Android. The gate is correct but essentially a no-op for Android users with a build-config key. iOS (Keychain-configured) is the meaningful runtime scenario.
- **Removed**: `HomeUiState.isWeatherKeyConfigured`, `HomeViewModel.refreshWeatherKeyState()`, `VcKeyNudgeBanner` composable, `TestTags.HOME_VC_KEY_NUDGE_ACTION`, and `HomeViewModelWeatherKeyTest` are all deleted.
- **Added**: `WeatherApiKeyManager.hasValidKeyFlow: StateFlow<Boolean>`, `onboarding.VcKeyOnboardingViewModel`, `onboarding.VcKeyOnboardingScreen`, `TestTags.ONBOARDING_VC_KEY_CONFIGURE`, `VcKeyOnboardingViewModelTest`.
- **Maestro J1 flow** updated: asserts `onboarding_vc_key_configure` is visible and `home_search_field` is not, reflecting the gate being the first thing a fresh-install user sees.
