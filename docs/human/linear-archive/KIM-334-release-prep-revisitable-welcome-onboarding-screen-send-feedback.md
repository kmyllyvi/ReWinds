# KIM-334: Release prep: revisitable welcome/onboarding screen + Send Feedback

**Status:** Done · **Priority:** No priority · **Labels:** _none_
**Created:** 2026-07-02T09:30:28.664Z · **Completed:** 2026-07-02T09:49:54.762Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-334/release-prep-revisitable-welcomeonboarding-screen-send-feedback

## Description

## Spec

Two small TestFlight release-prep features, both approved by Kimmo (skip Gate 1 per his instruction).

**1. Revisitable welcome/onboarding screen.** On first-ever launch, show an informational welcome screen explaining that ReWinds is a historical weather tracking app for wind sports (NOT a live forecast app), before the existing VC-API-key gate. Persist "seen" via AppSettingsStore key `has_seen_onboarding_v1`. Add a Settings → About row ("What is ReWinds?") to re-open it on demand.

**2. Send Feedback (email).** Settings → About row that opens the device email client via `mailto:` to [apps@goaheadand.dev](<mailto:apps@goaheadand.dev>) with a prefilled subject + body (app version + platform). New `expect/actual` email-opening fn in core, implemented for Android (ACTION_SENDTO) and iOS (openURL with percent-encoded mailto).

## Acceptance criteria

- [ ] Welcome screen shows automatically on first launch, before the VC-key gate; dismissed with a "Get Started" button
- [ ] "Seen" state persists under `has_seen_onboarding_v1` via AppSettingsStore; not shown again on subsequent launches
- [ ] Settings → About has a "What is ReWinds?" row that re-opens the same welcome content with a "Done" button, without re-triggering first-launch logic
- [ ] New WelcomeViewModel (StateFlow, all logic) + pure-render WelcomeView; wired into DI; new NavRoute for Settings navigation
- [ ] Settings → About has a "Send Feedback" row that opens the email client via mailto to [apps@goaheadand.dev](<mailto:apps@goaheadand.dev>) with prefilled subject + body
- [ ] Email open uses expect/actual: Android ACTION_SENDTO, iOS openURL with percent-encoded mailto
- [ ] New AppStrings fields added to both English and German with real German translations
- [ ] No MV* violations; new logic unit-tested in the same commit

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit for all non-trivial logic
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations
- [ ] Passes CI coverage gates

## Notes

Release-prep bundle for the first iOS TestFlight beta. Reviewers: code-reviewer (+ ux-ui-reviewer for the welcome screen if time).

## Comments

### kimmo.myllyviita@gmail.com — 2026-07-02T09:47:18.330Z

**Dev handoff (Randy)**

- **Branch:** `kimmomyllyviita/kim-334-welcome-onboarding-send-feedback`
- **PR:** https://github.com/kmyllyvi/ReWinds/pull/67
- **Build:** pass (`:composeApp:compileDebugKotlinAndroid`)
- **Tests:** pass (`:composeApp:testDebugUnitTest` green)
- **CI:** green — Android Tests, iOS Unit Tests (Kotlin/Native), Android Instrumented all success. The iOS Native job confirms the iosMain `sendEmail` actual compiles/links.
- **New tests written:** yes — `commonTest/kotlin/onboarding/WelcomeViewModelTest.kt`, `commonTest/kotlin/core/MailtoBuilderTest.kt`, and feedback cases added to `commonTest/kotlin/settings/SettingsViewModelTest.kt`.

**Summary**
1. **Welcome guide** — `WelcomeViewModel` owns first-run visibility + persistence under `has_seen_onboarding_v1`; shown in `Navigation()` ahead of the VC-key gate, dismissed with "Get Started". Revisitable from Settings → About → "What is ReWinds?" via new `WelcomeRoute` (renders in the Settings tab with a "Done" button; no first-run side effects). `WelcomeView` is pure-render, wired into DI.
2. **Send Feedback** — new `expect fun sendEmail()`; Android `ACTION_SENDTO` (email apps only, subject/body as extras), iOS `openURL` with a percent-encoded mailto from the shared KMP-safe `MailtoBuilder`. `SettingsViewModel.onSendFeedbackClicked` assembles the body (app version + platform) and launches to apps@goaheadand.dev. Email sender + platform label injected for testability; body assembly is a pure companion fn.

New `AppStrings` fields added to English + German with real translations.

**Deviations**
- iOS uses the pre-iOS-10 `openURL(url)` (deprecated but functional) per the spec's "openURL" wording — compiles warning-free-enough and passed the Kotlin/Native CI job. If a warning-clean build is preferred, can switch to `openURL:options:completionHandler:` in a follow-up.
- Both features bundled in one PR per instruction (single TestFlight milestone).

**Manual test cases:** appended 3 rows to `docs/human/sections/test-checklist.html` (first-run welcome + persistence, revisit-from-Settings, feedback email launch on Android/iOS).

