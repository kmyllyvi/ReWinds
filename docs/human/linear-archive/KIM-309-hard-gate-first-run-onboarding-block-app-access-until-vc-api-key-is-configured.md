# KIM-309: Hard-gate first-run onboarding: block app access until VC API key is configured

**Status:** Done · **Priority:** Urgent · **Labels:** spec-ready, Feature
**Created:** 2026-06-18T08:13:31.532Z · **Completed:** 2026-06-18T08:37:21.899Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-309/hard-gate-first-run-onboarding-block-app-access-until-vc-api-key-is
**Related:** related: KIM-245 — First-run onboarding: Visual Crossing API key setup; related: KIM-252 — First-run onboarding: Claude (Anthropic) API key setup; related: KIM-279 — User guidance on Visual crossing key, API and pricing

## Description

## Spec

Without a Visual Crossing API key the weather feature is entirely broken — it is not a degraded experience, it is a zero-value app. The current soft banner (shipped in [KIM-245](https://linear.app/kimmo-m/issue/KIM-245/first-run-onboarding-visual-crossing-api-key-setup), Done) sits on the Home screen but the user can scroll past it and reach a non-functional app. This ticket replaces that soft gate with a full-screen blocking onboarding screen that is shown on every launch until a valid VC key is saved. The user has exactly one CTA — "Configure now" — which routes them into the existing VC key entry in Settings. There is no dismiss, skip, or quit affordance; the only way out is the OS home gesture. Once a valid key is saved and the user returns to the app, the gate is gone for that session and for all subsequent launches.

The [KIM-245](https://linear.app/kimmo-m/issue/KIM-245/first-run-onboarding-visual-crossing-api-key-setup) `VcKeyNudgeBanner` component is removed in this same commit — keeping both would create two competing entry points with inconsistent messaging. The new screen becomes the single first-run surface for VC key onboarding. Copy on the new screen reuses or builds on the strings updated in [KIM-279](https://linear.app/kimmo-m/issue/KIM-279/user-guidance-on-visual-crossing-key-api-and-pricing) (`visualCrossingKeyDescription`, `visualCrossingApiUrl`, `vcKeyNudgeBody`) wherever they fit — do not invent new copy where those strings already serve.

Scope: iOS (Milestone A). Android continues to use build-config; the gate logic is written in commonMain but the gate screen is only shown on iOS where `WeatherApiKeyManager.hasValidKey()` returns false. This is consistent with [KIM-245](https://linear.app/kimmo-m/issue/KIM-245/first-run-onboarding-visual-crossing-api-key-setup) and [KIM-252](https://linear.app/kimmo-m/issue/KIM-252/first-run-onboarding-claude-anthropic-api-key-setup).

## Acceptance criteria

- [ ] A new full-screen Composable `VcKeyOnboardingScreen` (or equivalent) exists in a dedicated file; it contains no business logic — it renders only what the ViewModel exposes
- [ ] A ViewModel (new `VcKeyOnboardingViewModel` or extended `HomeViewModel`) exposes a `StateFlow<Boolean>` named `isWeatherKeyConfigured` (reusing or forwarding the signal already on `HomeViewModel`); no duplicate reads of `WeatherApiKeyManager.hasValidKey()` are introduced — the existing StateFlow is the single source of truth
- [ ] On app launch, if `isWeatherKeyConfigured` emits `false`, the onboarding screen is shown full-screen before the Home screen is reachable; the Home screen is not in the composition while the gate is active
- [ ] On app launch, if `isWeatherKeyConfigured` emits `true`, the onboarding screen is skipped entirely and the Home screen loads normally
- [ ] The onboarding screen contains exactly one primary CTA ("Configure now" or equivalent label from [KIM-279](https://linear.app/kimmo-m/issue/KIM-279/user-guidance-on-visual-crossing-key-api-and-pricing) strings); tapping it navigates to the existing VC key entry in Settings via the existing navigation route
- [ ] The onboarding screen has no dismiss button, no skip button, no quit/exit button, and no back-navigation gesture that returns to a functional app state — the gate cannot be bypassed
- [ ] After the user saves a valid VC key in Settings and navigates back, `isWeatherKeyConfigured` updates to `true` in the same session (no restart required) and the onboarding screen is no longer shown
- [ ] The [KIM-245](https://linear.app/kimmo-m/issue/KIM-245/first-run-onboarding-visual-crossing-api-key-setup) `VcKeyNudgeBanner` component is removed from `HomeView` and its associated rendering logic is removed from `HomeViewModel` (or clearly replaced) in the same commit — no two competing VC key prompt surfaces exist simultaneously
- [ ] The copy on the onboarding screen (headline, body, CTA label) is sourced from `AppStrings.kt` — no hardcoded English strings in the Composable
- [ ] The onboarding screen does not apply a `.background()` to the root-level page container; it blends with the system background per the project UI guidelines
- [ ] All gating/navigation logic (when to show the screen, when to dismiss it, routing to Settings) lives in the ViewModel — the Composable contains no `if`/`when` on key-configuration state beyond collecting from the StateFlow
- [ ] At least one unit test covers the `isWeatherKeyConfigured` false → true transition for the ViewModel responsible for gate state
- [ ] No new MV* violations are introduced (see ARCHITECTURE-RULES.md)

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit for all non-trivial logic (ViewModel, Repository, pure functions, bug fixes). No "tests later". Exempt only: pure UI styling, config/doc-only changes — see AC for this ticket's exemption status.
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations

## Notes

Priority: Urgent — this is a Milestone A hard blocker. A public BYOK app that allows users to reach a broken weather feature will generate bad reviews and App Store rejection risk.

Proposed size: S (2–3 hours). The key infrastructure (`isWeatherKeyConfigured` StateFlow, Settings navigation route, VC key entry in Settings) is already in place from [KIM-245](https://linear.app/kimmo-m/issue/KIM-245/first-run-onboarding-visual-crossing-api-key-setup). Work is: new full-screen Composable + route wiring + removal of [KIM-245](https://linear.app/kimmo-m/issue/KIM-245/first-run-onboarding-visual-crossing-api-key-setup) banner + one ViewModel test. No new data layer or API work.

[KIM-279](https://linear.app/kimmo-m/issue/KIM-279/user-guidance-on-visual-crossing-key-api-and-pricing) dependency: this screen should reuse the `visualCrossingKeyDescription`, `visualCrossingApiUrl`, and `vcKeyNudgeBody` strings that [KIM-279](https://linear.app/kimmo-m/issue/KIM-279/user-guidance-on-visual-crossing-key-api-and-pricing) enriches. If [KIM-279](https://linear.app/kimmo-m/issue/KIM-279/user-guidance-on-visual-crossing-key-api-and-pricing) ships first, Randy picks up those improved strings automatically. If this ticket ships first, the screen uses the current (thinner) string values; [KIM-279](https://linear.app/kimmo-m/issue/KIM-279/user-guidance-on-visual-crossing-key-api-and-pricing) will improve them in a follow-up commit. Either order is acceptable — no hard dependency. Link [KIM-279](https://linear.app/kimmo-m/issue/KIM-279/user-guidance-on-visual-crossing-key-api-and-pricing) as related.

[KIM-245](https://linear.app/kimmo-m/issue/KIM-245/first-run-onboarding-visual-crossing-api-key-setup) banner removal decision (made by PO): the [KIM-245](https://linear.app/kimmo-m/issue/KIM-245/first-run-onboarding-visual-crossing-api-key-setup) `VcKeyNudgeBanner` on Home is removed in this commit. Rationale — two competing VC key prompt surfaces (a full-screen gate AND a home banner) would be confusing and redundant. The hard gate is strictly more effective. Kimmo should confirm this removal at Gate 1 if he wants to revisit.

[KIM-252](https://linear.app/kimmo-m/issue/KIM-252/first-run-onboarding-claude-anthropic-api-key-setup) (Claude key soft banner): out of scope here. Kimmo may later want the Anthropic key to follow the same hard-gate pattern — that is a separate decision.

No quit/exit button on the onboarding screen. Programmatic app termination (`exitProcess`, `Activity.finish()`) violates Apple HIG and risks App Review rejection. Android could technically support it but behaviour is kept consistent across platforms. The user exits via the OS home gesture.

Reviewers needed: code-reviewer (mandatory), ux-ui-reviewer (new user-facing full-screen), qa-test-agent (gate state logic and session-persistence behaviour).

Assumption: `WeatherApiKeyManager.hasValidKey()` is accessible from commonMain/iosMain. This was true at [KIM-245](https://linear.app/kimmo-m/issue/KIM-245/first-run-onboarding-visual-crossing-api-key-setup) ship time — verify before coding if any refactor has touched that class since [KIM-245](https://linear.app/kimmo-m/issue/KIM-245/first-run-onboarding-visual-crossing-api-key-setup) merged.

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-18T13:56:25.258Z

Follow-up commit pushed to PR #45 (branch `kim--309-hard-gate-vc-key-onboarding`).

**Commit:** `82b4b38`

**Mr.T (UX) must-fix items applied** on `VcKeyOnboardingScreen.kt`:
- Accessibility (WCAG 1.1.1): hero `CloudOff` icon now has a meaningful `contentDescription` (reuses `vcKeyNudgeTitle`) instead of `null`.
- Removed the non-interactive raw VC API URL label (and its orphaned 8.dp spacer) from the gate screen. The URL still lives in the Settings ApiKeyDialog the CTA routes to; the `visualCrossingApiUrl` string is untouched.

**Seppo (QA) tests committed** (were green in working tree, now in version control):
- `WeatherApiKeyManagerTest.kt` (new) — validity boundaries + flow emissions, 9 tests.
- `VcKeyOnboardingViewModelTest.kt` (extended) — true→false gate-return + whitespace-key cases.

**Verification:**
- `./gradlew buildAndroidOnly` → BUILD SUCCESSFUL.
- `./gradlew :composeApp:testDebugUnitTest` → 508 tests, 0 failures, 0 errors.

**Still pending before merge:** iOS Keychain restart-persistence needs manual Xcode verification per Seppo's checklist (not coverable by the Android pilot build).

### kimmo.myllyviita@gmail.com — 2026-06-18T08:37:17.533Z

**Marcy (code-reviewer) — verdict: approve-with-nits ✅**

Reviewed the full PR #45 diff against all 13 acceptance criteria, the DoD, and the MV* rules. Ran the onboarding tests and the full Android unit suite — both green.

**All criteria + DoD met. No blocking issues.** Removing `in-review` and moving to Completed.

Key verifications:
- Hard gate shown full-screen when no valid VC key; tab surface never composed behind it; skipped when a key exists. ✅
- Single "Configure now" CTA → existing Settings VC entry; no dismiss/skip/quit/back; gate not bypassable (SettingsView has no back arrow). ✅
- Reactive clear chain traced end-to-end: VC save → `WeatherApiKeyManager.setApiKey` → `hasValidKeyFlow` emits → gate clears in-session. Sound, and more robust than back-nav refresh. ✅
- KIM-245 banner + `HomeViewModel`/`HomeUiState` plumbing fully removed; obsolete test deleted (its coverage re-homed in `VcKeyOnboardingViewModelTest`); Maestro J1 updated to assert the gate and `assertNotVisible` home search. ✅
- MV* clean; copy from `AppStrings.kt`; no `.background()` on gate root; false→true transition test present. ✅

Declared deviations both acceptable: (1) CTA reuses `vcKeyNudgeAction` ("Set up now") — AC permits an equivalent KIM-279 string; (2) reactive clear instead of back-nav refresh — an improvement.

Non-blocking nits (for follow-up / other reviewers):
- CTA lands on the *full* Settings screen and does not auto-open the VC key dialog (extra tap; full settings reachable while gated). Matches the old banner's behavior and satisfies the AC — flagging for Mr.T's UX call.
- `OnboardingGate` builds a throwaway `NavigatorImpl` to satisfy SettingsView's contract; works today, but a future `navigateBack` in SettingsView could affect the gate invariant. Consider a dedicated minimal gate Navigator later.

Honest limit: I share Randy's model family, so I reliably catch spec-drift / AC / MV* but not deep UX/design. Per the spec this ticket still needs **Mr.T (ux-ui-reviewer)** for the new full-screen surface and **Seppo (qa-test-agent)** for iOS session-persistence behavior before merge. PO note: Kimmo to confirm the KIM-245 banner removal at Gate 1.

### kimmo.myllyviita@gmail.com — 2026-06-18T08:33:22.820Z

**Randy (developer) handoff**

- **Branch:** `kim--309-hard-gate-vc-key-onboarding`
- **PR:** https://github.com/kmyllyvi/ReWinds/pull/45 (Closes KIM-309)
- **Build:** PASS — `./gradlew buildAndroidOnly`
- **Tests:** PASS — `./gradlew :composeApp:testDebugUnitTest` (new `VcKeyOnboardingViewModelTest`, 4 tests, incl. false → true gate transition)

**Summary**
- New full-screen blocking gate `VcKeyOnboardingScreen` (presentational only) + `VcKeyOnboardingViewModel` owning the single source of truth `isWeatherKeyConfigured: StateFlow<Boolean>`.
- Added a reactive `WeatherApiKeyManager.hasValidKeyFlow`; the VM forwards it so the gate clears in the same session the instant a valid key is saved — no restart, no back button needed.
- Gate wired into `Navigation()` (Router.kt): when not configured the Home/tab surface is not composed; the only path is the "Configure now" CTA → existing Settings VC key entry. No dismiss/skip/quit/back.
- Removed the KIM-245 `VcKeyNudgeBanner` from `HomeView` and its `isWeatherKeyConfigured` / `refreshWeatherKeyState` logic from `HomeViewModel` (PO-confirmed supersession). Deleted the now-obsolete `HomeViewModelWeatherKeyTest`; updated J1 Maestro flow to assert the gate.
- Copy sourced entirely from `AppStrings.kt` (reused KIM-279 VC strings); no `.background()` on the gate root container.

**hasValidKey reachability check (spec's one assumption):** confirmed — `WeatherApiKeyManager.hasValidKey()` is in `commonMain/core` and reachable from the ViewModel layer, unchanged since KIM-245.

**Banner removal:** confirmed removed in this same commit.

**Deviations**
- CTA label reuses `vcKeyNudgeAction` ("Set up now" / "Jetzt einrichten") rather than inventing a literal "Configure now" string — the spec explicitly allows "or equivalent label from KIM-279 strings" and forbids inventing copy.
- The gate clears via reactive observation of the key manager rather than via a back-navigation refresh, which is why no back affordance is needed (and the gate stays unbypassable). This is stronger than the spec's "navigate back" wording but satisfies the same AC (same-session clear, no restart).

Handing off to Marcy (code-reviewer) in-session.

