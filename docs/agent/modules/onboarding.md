# Onboarding Module

**Last updated:** 2026-06-19 (PR #45 — KIM-309)
**Status:** Active

---

## Purpose

Provides a full-screen hard gate that blocks all access to the functional app until a valid Visual Crossing API key is configured. Shown on every launch while no key is present; clears reactively in the same session the moment a valid key is saved — no restart required.

This module supersedes the KIM-245 soft `VcKeyNudgeBanner` that was previously shown inside `HomeView`.

---

## Responsibilities

- Owning the single source of truth for whether a valid VC key is configured (`VcKeyOnboardingViewModel.isWeatherKeyConfigured: StateFlow<Boolean>`).
- Blocking the Home/tab surface from entering the composition while the gate is active.
- Routing the user to the existing Settings VC key entry via the "Configure now" CTA (`VcKeyOnboardingViewModel.showKeyEntry: StateFlow<Boolean>`).
- Providing a presentational full-screen screen (`VcKeyOnboardingScreen`) with no dismiss, skip, quit, or back affordance.

---

## Dependencies

### Internal
- `core.WeatherApiKeyManager` — `hasValidKeyFlow: StateFlow<Boolean>` is the upstream reactive source; the ViewModel forwards this flow via `stateIn(SharingStarted.Eagerly)`.
- `core.Router` — `Navigation()` composable hosts `OnboardingGate` and returns early (before rendering `AppTabs`) while the gate is active.
- `settings.SettingsView` — reused inside `OnboardingGate` for the key-entry surface; a gate-local `NavigatorImpl` wrapping a `gateSettingsStack` satisfies `SettingsView`'s `Navigator` contract without exposing the real tab stack.
- `core.LocalAppStrings` — `vcKeyNudgeTitle`, `vcKeyNudgeBody`, `vcKeyNudgeAction` strings consumed by `VcKeyOnboardingScreen` (previously banner-only, now reused by the gate).
- `ui.components.IsobarBackground` — decorative texture on the gate screen root; no `.background()` on the root container per UI guidelines.
- Koin — `VcKeyOnboardingViewModel` registered via `viewModelOf(::VcKeyOnboardingViewModel)` in `DI.kt`.

---

## Key interfaces

### VcKeyOnboardingViewModel
```kotlin
class VcKeyOnboardingViewModel : ViewModel() {
    /** True once a valid VC key is present. When false, the hard gate is active. */
    val isWeatherKeyConfigured: StateFlow<Boolean>

    /** True while the user is on the Settings key-entry surface reached from the gate CTA. */
    val showKeyEntry: StateFlow<Boolean>

    /** "Configure now" CTA — route into the existing Settings VC key entry. */
    fun onConfigureNowClicked()
}
```

`isWeatherKeyConfigured` is seeded with `WeatherApiKeyManager.hasValidKey()` at construction and stays live via `hasValidKeyFlow`.

### VcKeyOnboardingScreen
```kotlin
@Composable
fun VcKeyOnboardingScreen(onConfigureNow: () -> Unit)
```
Presentational only. Single CTA button tagged `TestTags.ONBOARDING_VC_KEY_CONFIGURE` (`"onboarding_vc_key_configure"`).

### OnboardingGate (private, in Router.kt)
```kotlin
@Composable
private fun OnboardingGate(vm: VcKeyOnboardingViewModel)
```
Switches between `VcKeyOnboardingScreen` and a gate-local `SettingsView` based on `vm.showKeyEntry`. The gate-local settings navigator uses a separate `gateSettingsStack` — it does not share the main settings tab stack.

---

## Test coverage

`commonTest/onboarding/VcKeyOnboardingViewModelTest.kt` — 4 tests covering:
- Gate active when no key is set.
- `onConfigureNowClicked()` sets `showKeyEntry = true`.
- Gate clears (false → true) when `WeatherApiKeyManager.setApiKey()` is called with a valid key.
- Gate remains active if the key fails the `hasValidKey()` check (blank or contains "placeholder").

---

## Known constraints

- The gate is implemented entirely in `commonMain`; it blocks the app on both iOS and Android. On Android, the VC key is typically pre-configured via `BuildConfig`, so the gate is not reached in practice on Android. iOS is the in-scope platform for this feature (as noted in the PR; Android uses build-config).
- There is no "quit" or "skip" affordance by design. The only exit from the gate is saving a valid key.
- The gate-local `NavigatorImpl` for `SettingsView` is created via `remember` inside `OnboardingGate`; it is not Koin-managed.

---

## Decisions log

- `/docs/agent/decisions/2026-06-19-hard-gate-vc-key-onboarding.md` — rationale for replacing the soft banner with a hard gate.
