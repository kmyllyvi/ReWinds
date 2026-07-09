# KIM-293: Compose semantic UI tests for critical flows (Android)

**Status:** Done · **Priority:** High · **Labels:** spec-ready
**Created:** 2026-06-15T07:16:59.149Z · **Completed:** 2026-06-15T16:13:03.420Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android
**Parent:** KIM-289 — [EPIC] UI Testing Strategy & Automation
**Related:** related: KIM-294 — Maestro setup + cross-platform E2E smoke suite (Android + iOS); related: KIM-290 — Add testTag / contentDescription coverage to commonMain screens; related: KIM-291 — Define 5-10 critical user journeys for UI test automation; related: KIM-292 — Navigation testing strategy: fakeable Navigator/Router for tests

## Description

## Spec

Layer 1 of the UI testing strategy ([KIM-289](https://linear.app/kimmo-m/issue/KIM-289/epic-ui-testing-strategy-and-automation)): fast, deterministic Compose UI tests covering a subset of the P0 critical journeys (from [KIM-291](https://linear.app/kimmo-m/issue/KIM-291/define-5-10-critical-user-journeys-for-ui-test-automation)), running via `connectedAndroidTest` against an emulator/device. Tests inject fakes (repositories, `FakeNavigator` from [KIM-292](https://linear.app/kimmo-m/issue/KIM-292/navigation-testing-strategy-fakeable-navigatorrouter-for-tests)) via the existing Koin test-module pattern — no real network/DB. `ChatLayoutTest.kt` is migrated from text selectors to `TestTags` ([KIM-290](https://linear.app/kimmo-m/issue/KIM-290/add-testtag-contentdescription-coverage-to-commonmain-screens)) and kept as a reference example alongside the new tests.

CI wiring (adding an emulator-backed instrumented-test job to the PR pipeline) is **not** in scope for this ticket — see Notes. The "run on every PR" part of the original title is deferred to a follow-up ticket.

## Acceptance criteria

- [ ] A Compose test exists for J2 (add a place via search): typing in `HOME_SEARCH_FIELD`, tapping a suggestion, and asserting a new `PlaceRow` appears, using injected fake repository data (no real network)
- [ ] A Compose test exists for J3 (open a saved place's summary): seeding a place via fake repo, tapping a `PlaceRow`, and asserting `FakeNavigator` recorded navigation to `PlaceSummaryRoute`
- [ ] A Compose test exists for J6 (chat send/receive): typing into `CHAT_INPUT_FIELD`, tapping `CHAT_SEND_BUTTON`, and asserting a user message bubble then an assistant message bubble appear, using a fake AI/chat repository (no real network)
- [ ] A Compose test exists for J7 (chat blocked without Anthropic key): tapping send with no key configured (fake config), asserting the "API Key Not Configured" dialog appears, and tapping "Go to Settings" asserts `FakeNavigator` recorded navigation to Settings
- [ ] A Compose test exists for J8 (switch chat sessions): seeding two sessions via fake repo, opening `ChatSessionSwitcher` via `CHAT_SESSION_SWITCHER_BUTTON`, tapping a non-active `SessionRow`, and asserting the message list now shows the selected session's messages
- [ ] A Compose test exists for J10 (configure Anthropic API key in Settings): tapping the Anthropic `SettingsKeyRow`, entering a key in `ApiKeyDialog`, tapping Save, and asserting the row now shows the "Configured" chip
- [ ] `ChatLayoutTest.kt` is migrated to use `TestTags` constants (`onNodeWithTag`) instead of `onNodeWithText`, and continues to pass
- [ ] All new tests pass under `./gradlew :composeApp:connectedAndroidTest` (or the equivalent instrumented test task) on an emulator
- [ ] All new tests use only injected fakes (fake repositories, `FakeNavigator`) — no real network or database access, and pass deterministically when run in isolation or as part of the full suite (order-independent)
- [ ] A new section is added to `docs/` (or `composeApp/src/androidInstrumentedTest/README.md` if no such doc exists) describing the Compose-UI-test pattern: how to seed fakes via Koin, how to use `FakeNavigator`, and how to reference `TestTags`
- [ ] J1, J4, J5, J9, J11 (and CI wiring for "every PR") are explicitly noted as out of scope/deferred in the PR description — not silently dropped
- [ ] No new tests required beyond the Compose UI tests above for this ticket's own infra (the tests themselves are the new test coverage)

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit for all non-trivial logic (ViewModel, Repository, pure functions, bug fixes). No "tests later". Exempt only: pure UI styling, config/doc-only changes — see AC for this ticket's exemption status.
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations

## Notes

**Scope decisions made by PO (split from the original ticket):**

* Reduced from "all 8 P0 journeys + wired into PR pipeline" to 5 journeys (J2, J3, J6, J7, J8) plus the `ChatLayoutTest` migration and pattern doc — fits a 1-3hr session. J1/J4/J5/J9/J11 are covered (or will be) by [KIM-294](https://linear.app/kimmo-m/issue/KIM-294/maestro-setup-cross-platform-e2e-smoke-suite-android-ios)'s Maestro suite at the E2E layer; Compose coverage for those can be a follow-up ticket if Kimmo wants it.
* CI wiring ("run on every PR") split into a separate follow-up ticket — the current `.github/workflows/ci.yml` has no emulator/instrumented-test job, and adding one (emulator boot, caching, timing) is itself a coherent 1-3hr unit of work that depends on these tests existing first. Not yet created in Linear; flag to Kimmo at Gate 1 whether to create it now or after this ticket lands.
* J6/J7 require faking the Anthropic AI repository/response — assumed an existing fake or mockable `AiRepository`/`ChatRepository` interface is usable; if no fake exists yet, the developer should add a minimal one scoped to these tests (small addition, still fits session). If it turns out to be larger, defer J7 to the follow-up ticket and note it in the PR.

**Reviewers needed:** code-reviewer (always), qa-test-agent (logic-heavy — these are the canonical example tests for the whole epic, worth a QA pass on flakiness/determinism). No ux-ui-reviewer — no UI changes, tests only.

Priority: High (retained from epic triage — core Layer 1 deliverable, unblocks epic completion). Size: proposed at 1 session (5 journeys + migration + doc).

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-15T16:13:06.513Z

PR #32 merged into develop. Marcy's review passed (AC/DoD met, production seam refactors — `AiConversationRepository`, `ApiKeyChecker`, `commonTestFixtures` move — are clean and MV*-compliant, no duplicate DI bindings). 8/8 instrumented tests pass. Marking as Done.

Open item for @kimmo.myllyviita@gmail.com (Gate 1, not resolved by agents): whether to create a CI-wiring follow-up ticket now for an emulator-backed instrumented-test job (`.github/workflows/ci.yml` currently has none), or defer it.

### kimmo.myllyviita@gmail.com — 2026-06-15T16:12:18.854Z

**Code review (Marcy) — PASS ✅**

Reviewed PR #32 against KIM-293 AC/DoD and ARCHITECTURE-RULES.md (MV*, DI). All acceptance criteria and DoD items met; no Critical/Major issues found.

**Verified locally (branch `kim-293-compose-ui-tests` vs `develop`):**
- `:composeApp:testDebugUnitTest` → green (includes the updated `ChatViewModelKoinGraphTest` exercising the new interface binding)
- `:composeApp:compileDebugAndroidTestKotlin` → green (instrumented sources type-check; signatures/TestTags all resolve)
- `:composeApp:buildAndroidOnly` → green, lint clean (run `--no-configuration-cache`; the pre-existing config-cache violation in the wrapper task is unrelated)
- Instrumented run (8/8 on Pixel_9 API 36) taken on Randy's report — needs an emulator, not re-run here.

**AC coverage:**
- J2/J3 (`HomeJourneyTest`), J6/J7/J8 (`ChatJourneyTest`), J10 (`SettingsJourneyTest`) — all present, drive real composables via `TestTags`, assert navigation via `FakeNavigator`, use only in-memory fakes. J8 also asserts the *absence* of the prior session's message (strong).
- `ChatLayoutTest` migrated text→tag selectors, kept as reference, compiles.
- Pattern doc at `androidInstrumentedTest/README.md` covers Koin/direct-construction, fakes, TestTags, FakeNavigator.
- Out-of-scope (J1/J4/J5/J9/J11 + CI wiring) explicitly noted in PR description, not dropped.
- Tests are order-independent: the only shared global mutable state (`ApiKeyManager`/`WeatherApiKeyManager` in J10) is reset in `@Before`/`@After`.

**Production seam refactor (the high-risk part) — clean & MV*-compliant:**
- `AiConversationRepository` interface extraction: `ChatViewModel` now depends on the abstraction; `AiRepository` is the sole impl; DI binds `single<AiConversationRepository>`. No stray concrete-type refs remain.
- `ApiKeyChecker` (fun interface) + `PlatformApiKeyChecker` default, injected into `ChatViewModel`/`SettingsViewModel`. Logic stays in the VMs — no leakage into views.
- DI bindings correct: `SettingsViewModel` switched `viewModelOf`→explicit `viewModel { }` lambda for the same documented reason as `ChatViewModel`'s `ioDispatcher` (defaulted ctor arg must not be constructor-reflected). No duplicate/conflicting bindings.
- `FakeNavigator` moved `commonTest`→`commonTestFixtures` (clean R100 rename, single file on disk); shared into both `commonTest` and Android `androidTest` source sets. `commonTestFixtures` is a sibling dir, not nested under `commonTest`, so no double-compilation; the two source sets produce distinct artifacts so no duplicate-class clash.
- Two testTags added to `AppHeader` title / `ChatMessageBubble` — presentational only.

**For Kimmo (not resolved by review):** Randy's open Gate-1 question stands — whether to create the CI-wiring follow-up ticket (emulator-backed instrumented job; current `ci.yml` has none) now or after this lands. Your call.

**Honest limit:** same model family as the developer — I reliably catch spec-drift, MV*/DI violations, and obvious defects, but not deep design flaws. The seam abstractions look right to me; given this is the canonical example test infra for the whole epic, a quick human eyeball before merge is worth it.

Leaving issue state as-is per the KIM-292 precedent (merge next, then move to Done). Issue remains In Progress; removing `in-review`.

### kimmo.myllyviita@gmail.com — 2026-06-15T13:39:59.229Z

**Dev handover — ready for review**

- **Branch:** `kim-293-compose-ui-tests`
- **PR:** https://github.com/kmyllyvi/ReWinds/pull/32 (Closes KIM-293)
- **Build:** PASS — `buildAndroidOnly` green (run with `--no-configuration-cache`; the wrapper task has a pre-existing config-cache violation unrelated to this change). Lint clean, no new violations.
- **Tests:** PASS
  - `:composeApp:connectedDebugAndroidTest` → **8/8** on Pixel_9 API 36 (6 journeys + 2 ChatLayoutTest), stable across repeated reruns (order-independent / deterministic).
  - `:composeApp:testDebugUnitTest` → green (existing suite, incl. updated `ChatViewModelKoinGraphTest`).

**Summary**
Compose semantic UI tests for **J2, J3, J6, J7, J8, J10** under `androidInstrumentedTest/kotlin/uitest/`, driven by in-memory fakes (no network/DB). `ChatLayoutTest` migrated from `onNodeWithText` → `TestTags`/`onNodeWithTag`. Pattern doc at `composeApp/src/androidInstrumentedTest/README.md`.

Each AC item is met:
- J2/J3 via `HomeJourneyTest`, J6/J7/J8 via `ChatJourneyTest`, J10 via `SettingsJourneyTest`.
- J7 asserts the "API Key Not Configured" dialog + `FakeNavigator` recording `NavigationCall.Settings`.
- J3 asserts `FakeNavigator` recorded navigation to the place summary; J8 asserts the message list swaps to the selected session's messages; J10 asserts the Anthropic row chip flips to "Configured".
- Tests inject only fakes + `FakeNavigator`; deterministic in isolation and in the full suite.

**Deviations / decisions (please sanity-check at review):**
1. **Injection style:** screens are driven by constructing the real ViewModel from fakes and passing it as the `vm` parameter, rather than `koinViewModel()` + a started Koin graph. Both "inject fakes", but direct construction avoids a global Koin singleton and guarantees the order-independence AC. The pattern doc notes the Koin-module variant is equivalent.
2. **Testability seams added in production (MV*-clean):**
   - Extracted `AiConversationRepository` interface (AiRepository implements it); ChatViewModel depends on the interface so chat is fakeable without the Anthropic network. DI binds it as the interface. (Spec explicitly allowed adding a minimal fake/seam for J6/J7.)
   - Added `ApiKeyChecker` fun interface + `PlatformApiKeyChecker` default, injected into ChatViewModel & SettingsViewModel, so the key present/absent branch (J7/J10) is deterministic instead of depending on local `gradle.properties`/Keychain/`BuildConfig`. `SettingsViewModel` Koin binding switched `viewModelOf` → explicit `viewModel { }` lambda so the defaulted param isn't reflection-resolved.
3. **FakeNavigator relocated** from `commonTest` to a new `src/commonTestFixtures/kotlin` dir wired into both `commonTest` and the Android `androidTest` source sets — instrumented tests can't see `commonTest`, so a shared fixtures dir was needed to keep one navigator double with no drift.
4. **Scope (per ticket Notes):** the ticket title says 5 journeys but the AC lists 6 (adds J10) — implemented all 6. **Out of scope, deferred (not dropped):** J1/J4/J5/J9/J11 Compose coverage (E2E via KIM-294 Maestro), and CI "run on every PR" wiring — no emulator job exists in `ci.yml` yet. **Gate-1 question for @kimmo_m:** create the CI-wiring follow-up ticket now, or after this lands?

Reviewers per ticket: code-reviewer + qa-test-agent (determinism/flakiness pass). No UI changes → no ux-ui review.

### kimmo.myllyviita@gmail.com — 2026-06-15T13:08:37.341Z

Specced and moved to Todo for Randy.

Key context:
- testTag coverage (KIM-290), journeys (KIM-291), and `FakeNavigator` + example test (KIM-292) are all done/merged — use `composeApp/src/commonTest/kotlin/core/FakeNavigator.kt` + `FakeNavigatorExampleTest.kt` as the pattern reference, and `core/TestTags.kt` for selectors.
- Scoped down to 5 journeys (J2, J3, J6, J7, J8) + ChatLayoutTest migration + pattern doc — see Notes for why J1/J4/J5/J9/J11 and CI wiring ("every PR") are deferred to a follow-up ticket (not created yet, flagging to Kimmo separately).
- No existing emulator job in `.github/workflows/ci.yml` — these tests run via `connectedAndroidTest` locally/on-demand for now.
- If J6/J7 fakes for the Anthropic/chat repo turn out bigger than expected, defer J7 and note it in the PR rather than over-running the session.

