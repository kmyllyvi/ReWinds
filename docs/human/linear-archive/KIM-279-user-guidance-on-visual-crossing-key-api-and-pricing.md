# KIM-279: User guidance on Visual crossing key, API and pricing

**Status:** Done · **Priority:** High · **Labels:** spec-ready, Feature
**Created:** 2026-06-09T09:55:49.296Z · **Completed:** 2026-07-07T19:29:21.058Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-279/user-guidance-on-visual-crossing-key-api-and-pricing

## Description

## Spec

New users of ReWinds (BYOK model, Milestone A) will never have heard of Visual Crossing. When they land on the app for the first time, or when they tap into the Visual Crossing API Key entry in Settings, there is currently no explanation of what Visual Crossing is, how to obtain a free key, or what the free tier allows. This ticket adds that guidance in-app so users can self-serve without leaving the app to Google it.

The scope is **copy/content only** — no new screens, no new ViewModels, no new navigation routes. The work touches:

1. The `visualCrossingKeyDescription` and `visualCrossingApiUrl` strings that appear inside the existing `ApiKeyDialog` in Settings.
2. The `vcKeyNudgeBody` string inside `VcKeyNudgeBanner` on the Home screen.
3. Both EN and DE localisations in `AppStrings.kt`.

Guidance to convey: Visual Crossing provides historical and forecast weather data; the free tier is 1 000 records/day (roughly 2–3 years of daily data per query); a free account and key can be created at [visualcrossing.com/weather-api](<http://visualcrossing.com/weather-api>); no credit card is required for the free tier.

## Acceptance criteria

- [ ] `visualCrossingKeyDescription` (EN) explains in 2–4 sentences: what Visual Crossing is, that a free account is available, the free-tier daily record limit (1 000 records/day), and that no credit card is required.
- [ ] `visualCrossingKeyDescription` (DE) contains the same information as the EN version, translated.
- [ ] `visualCrossingApiUrl` (EN) text is updated to read as a clear call-to-action (e.g. "Create a free account at: [https://www.visualcrossing.com/weather-api](<https://www.visualcrossing.com/weather-api>)") rather than the current neutral "Get your API key from: …".
- [ ] `visualCrossingApiUrl` (DE) is updated to match, translated.
- [ ] `vcKeyNudgeBody` (EN) is updated to include one sentence of context explaining why a Visual Crossing key is needed (weather data source), in addition to the existing call-to-action copy.
- [ ] `vcKeyNudgeBody` (DE) matches, translated.
- [ ] All six changed strings compile without error in `buildAndroidOnly`.
- [ ] No new Composables, ViewModels, or navigation routes are introduced by this change.
- [ ] No tests are required — this ticket changes only string literals with no logic; exempt from the "new tests required" DoD item.

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit for all non-trivial logic (ViewModel, Repository, pure functions, bug fixes). No "tests later". Exempt only: pure UI styling, config/doc-only changes — see AC for this ticket's exemption status.
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations

## Notes

Priority set to High — this is Milestone A (BYOK public launch). A user who does not understand Visual Crossing will not configure the app; the free tier detail ("1 000 records/day, no card") is the key conversion nudge. Size: XS — string-only change, one file (`AppStrings.kt`), under an hour.

Reviewers needed: code-reviewer (mandatory). ux-ui-reviewer recommended — copy is user-facing and tone matters for a BYOK onboarding moment.

**Assumption**: free-tier limit of 1 000 records/day is correct as of June 2026. If Kimmo knows it has changed, update the number before Randy implements. The direct sign-up URL used (`/weather-api`) should be verified live before the copy ships — link it in the PR description.

**Out of scope for this ticket**: a deep-link from the nudge banner directly to the VC sign-up page (would require platform-level URL launching); a dedicated onboarding screen; pricing table UI.

## Comments

### kimmo.myllyviita@gmail.com — 2026-07-07T19:29:25.759Z

Review passed and PR #69 merged into develop (merge commit 55d0b1c). All 9 acceptance criteria and all DoD items verified against the diff: EN+DE parity on all six strings, CTA phrasing with the /weather-api URL, "weather data source" context sentence in both nudge banners, and the free-tier + no-card + 1,000-records/day facts in the description. String-literal-only change — no MV*/KMP violations, no new Composables/ViewModels/routes; tests exemption is valid and stated in the PR. DE unicode escapes decode to the correct German characters. CI run 28892499042 was green on the exact head SHA (00af20e) at merge time. Manual UX/wrapping check logged to the async test checklist (docs/human/sections/test-checklist.html) for Kimmo. Moving to Completed and removing in-review.

### kimmo.myllyviita@gmail.com — 2026-07-07T19:28:05.609Z

**Dev handoff — Randy**

- **Branch**: `kimmomyllyviita/kim-279-user-guidance-on-visual-crossing-key-api-and-pricing`
- **PR**: https://github.com/kmyllyvi/ReWinds/pull/69 (Closes KIM-279)
- **Build**: PASS — `./gradlew buildAndroidOnly` (lint included, no new violations)
- **Tests**: PASS — `./gradlew :composeApp:testDebugUnitTest`
- **CI**: PASS — run 28892499042 (green, incl. iOS Kotlin/Native unit tests)

**Summary**: Copy/content-only change to `AppStrings.kt`. Rewrote six strings (3 EN + 3 DE) so BYOK users understand Visual Crossing and how to get a free key:
- `visualCrossingKeyDescription` — explains VC as the weather data source, free account, 1,000 records/day free tier, no credit card.
- `visualCrossingApiUrl` — now a CTA: "Create a free account at: https://www.visualcrossing.com/weather-api".
- `vcKeyNudgeBody` (Home banner) — added a sentence on why the key is needed (weather data source).

**Assumptions verified live (2026-07-07)**:
- Free tier = **1,000 records/day** — confirmed on visualcrossing.com/weather-api (product copy + FAQ).
- `/weather-api` sign-up URL — HTTP 200, live. Used per AC's literal example. (Page's own CTA button targets `/sign-up/`, also live; kept `/weather-api` as specified.)
- No credit card for free tier — confirmed.

**Deviations**: none. No new tests (string-only change; explicit AC exemption).

**Manual test cases**: appended 1 row to `docs/human/sections/test-checklist.html` — UX/copy + text-wrapping check for the API-key dialog and Home nudge, EN + DE (Automate? = No, pure localized copy/tone).

Handing off to **Marcy (code-reviewer)** in-session; **Mr.T (ux-ui-reviewer)** requested additionally since this is user-facing onboarding copy.

