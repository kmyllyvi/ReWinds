# KIM-292: Navigation testing strategy: fakeable Navigator/Router for tests

**Status:** Done · **Priority:** High · **Labels:** spec-ready
**Created:** 2026-06-15T07:16:44.321Z · **Completed:** 2026-06-15T12:58:14.899Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-292/navigation-testing-strategy-fakeable-navigatorrouter-for-tests
**Parent:** KIM-289 — [EPIC] UI Testing Strategy & Automation
**Related:** related: KIM-294 — Maestro setup + cross-platform E2E smoke suite (Android + iOS); related: KIM-291 — Define 5-10 critical user journeys for UI test automation

## Description

## Spec

[KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android-run-on-every-pr) needs Compose tests that seed a starting screen and assert navigation outcomes (e.g. tapping a `PlaceRow` navigates to `PlaceSummaryRoute`, tapping back from `ChatRoute` root switches tabs). Today `Navigator` is already a clean interface (`core/Navigator.kt`) implemented by `NavigatorImpl` (plain constructor, takes a `SnapshotStateList<NavRoute>` — no Koin involved) and decorated by `TabRoutingNavigator` for cross-tab routing. No refactor is needed to make navigation injectable — it already is. This ticket adds a `FakeNavigator` test double and documents the seed-route + assert-navigation pattern with one worked example, so [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android-run-on-every-pr) can reuse it directly.

## Acceptance criteria

- [ ] A `FakeNavigator` class implementing `core.Navigator` exists in `composeApp/src/commonTest/kotlin/core/` (or a shared test-fixtures source set if one already exists), recording each navigation call (method name + arguments) in an inspectable list, e.g. `navigateToPlaceSummary("Helsinki")` appends a recorded `NavigationCall.PlaceSummary("Helsinki")`
- [ ] `FakeNavigator.canNavigateBack()` and `navigateBack()` are implemented with simple, overridable behaviour (e.g. a settable boolean / recorded back-stack depth) so tests can assert both "back was requested" and simulate "at root" vs "can pop"
- [ ] A short doc comment (KDoc on `FakeNavigator` or a markdown note in `docs/`) explains how a Compose test seeds a starting screen (pass `FakeNavigator` to the View under test, e.g. `PlaceSummaryView(route = ..., navigator = FakeNavigator())`) and asserts navigation by inspecting `FakeNavigator`'s recorded calls
- [ ] One example test in `composeApp/src/commonTest/kotlin/` demonstrates the pattern end-to-end: render a screen that takes a `Navigator` (e.g. `PlaceSummaryView` or `SettingsView`), trigger a navigation action, and assert the expected call was recorded on `FakeNavigator`
- [ ] No changes to `Navigator`, `NavigatorImpl`, `TabRoutingNavigator`, `Router`, `NavigationRoutes.kt`, or `TabNavigationViewModel` — they are already injectable as-is; this ticket is additive (new test double + example test only)
- [ ] No business logic moved into Composables as a side effect of adding the example test

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit for all non-trivial logic (ViewModel, Repository, pure functions, bug fixes). No "tests later". Exempt only: pure UI styling, config/doc-only changes — see AC for this ticket's exemption status.
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations

## Notes

**Scope re-assessment (post** [KIM-290](https://linear.app/kimmo-m/issue/KIM-290/add-testtag-contentdescription-coverage-to-commonmain-screens)**/291/294):** the original "Why/Scope" referenced a possible Koin-binding refactor for `Navigator`/`Router`/`TabRoutingNavigator`. On reviewing current `develop`, `Navigator` is already a plain interface with a trivial constructor-injected implementation (`NavigatorImpl(backStack: SnapshotStateList<NavRoute>)`), and `TabRoutingNavigator` decorates it via `Navigator by base`. Both are already test-fakeable without any production code changes. Scope was reduced accordingly — this is a single small ticket (test double + example + short doc), not a refactor. No split needed.

`Router.kt` exists but is currently an empty file (0 lines) — out of scope for this ticket; not referenced by current navigation flow (`Navigation()` in `TabRoutingNavigator.kt` constructs `NavigatorImpl` directly).

This ticket directly unblocks [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android-run-on-every-pr) (Compose semantic UI tests for the P0 journeys defined in [KIM-291](https://linear.app/kimmo-m/issue/KIM-291/define-5-10-critical-user-journeys-for-ui-test-automation)) — the example test should ideally cover a small slice of one P0 journey (e.g. J3's "tap PlaceRow → navigate to PlaceSummaryRoute" or J7's "Go to Settings" nav from the API-key dialog) to double as a head start on [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android-run-on-every-pr).

Priority: High (retained — direct prerequisite for [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android-run-on-every-pr)). Size: small, single session.

Reviewers needed: code-reviewer. qa-test-agent optional (test-infrastructure ticket, low logic risk). No UI changes — ux-ui-reviewer not needed.

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-15T12:58:17.065Z

PR #31 merged into develop. Marcy's review passed (no Critical/Major issues, AC/DoD met, pure additive test infrastructure). Marking as Done.

Minor follow-up noted by Marcy: the ticket's Notes describe `Router.kt` as empty (0 lines), but it now has 100+ lines (untouched by this PR, out of scope) — worth a future cleanup pass on ticket text, not a blocker.

### kimmo.myllyviita@gmail.com — 2026-06-15T12:57:30.794Z

**Code review (Marcy) — PR #31, commit 3a8ec71: PASS, ready to merge.**

Reviewed the actual diff (164 additions, 0 deletions, 2 files in `commonTest/kotlin/core/`), ran the tests, and cross-checked every factual claim against the codebase.

**AC — all met:**
- [x] `FakeNavigator` in `commonTest/kotlin/core/` recording calls as `NavigationCall` — implements `core.Navigator` with all 7 methods, signatures identical to the production interface.
- [x] Settable `canNavigateBack` flag + `navigateBack()` always records `NavigationCall.Back` — both "can pop" and "at root" branches drivable.
- [x] KDoc documents the seed-route + assert-navigation pattern.
- [x] End-to-end example test (`FakeNavigatorExampleTest`, 5 tests) covering J3 (Place→Chat with placeId) and J7 (Go to Settings), plus back/root-depth and call ordering. The J3 case mirrors real production: `PlaceSummaryView.kt:129` does call `navigator.navigateToChat(placeId = currentPlaceName)`.
- [x] No changes to Navigator / NavigatorImpl / TabRoutingNavigator / Router / NavigationRoutes / TabNavigationViewModel — git diff confirms zero production files touched.
- [x] No business logic moved into Composables (no Composables added).

**DoD — all met:**
- [x] Android unit tests pass: `:composeApp:testDebugUnitTest --tests core.FakeNavigatorExampleTest` → BUILD SUCCESSFUL, `tests="5" failures="0" errors="0"`.
- [x] Tests are the deliverable, present in the same commit.
- [x] No MV* violations (no ViewModel/View added; pure test infra).
- [x] No new lint surface.

**Accepted deviations (both sound, both disclosed by Randy):**
1. Example is a plain JVM unit test, not a Compose render — correct: `commonTest` has no Compose UI runtime here; instrumented render tests live in `androidInstrumentedTest`. The double exercises the real navigation contract and is directly reusable by KIM-293.
2. `buildAndroidOnly` config-cache failure (`Task.project` at execution time) — confirmed pre-existing and unrelated; the unit-test target builds green.

**Minor note (non-blocking, not a PR defect):** this ticket's Notes still say `Router.kt` is "an empty file (0 lines)" — it now has 100+ lines. `Router.kt` is untouched and out of scope, so the "no changes to Router" AC still holds; worth updating the ticket note at some point.

`in-review` removed. Per the KIM-294 precedent (marked Done after merge), leaving this In Progress — merge PR #31, then move to Completed. Low-risk test-only change; no extra human eyeball needed before merge.

### kimmo.myllyviita@gmail.com — 2026-06-15T12:51:49.743Z

Handover — KIM-292 implemented (additive, test-infrastructure only).

**Branch:** `kim-292-fakeable-navigator`
**PR:** https://github.com/kmyllyvi/ReWinds/pull/31 (Closes KIM-292)
**Build:** PASS — `./gradlew buildAndroidOnly` (with `--no-configuration-cache`; see Deviations)
**Tests:** PASS — `./gradlew :composeApp:testDebugUnitTest -PincludeAllTargets=false` green, no existing tests broken

**Summary**
- Added `FakeNavigator` in `composeApp/src/commonTest/kotlin/core/FakeNavigator.kt` — implements `core.Navigator`, records each call as an inspectable `NavigationCall` (`calls` / `lastCall`). `canNavigateBack` is a settable flag so tests can drive both "can pop" and "at root"; `navigateBack()` always records a `Back` so "back was requested" is assertable independently.
- KDoc on `FakeNavigator` documents the seed-route + assert-navigation pattern.
- Added `FakeNavigatorExampleTest` demonstrating the pattern end-to-end, covering a slice of KIM-291 J3 (Place → Chat carrying `placeId`, mirroring `PlaceSummaryView`) and J7 (Go to Settings), plus back/root-depth and call-ordering.

**AC coverage:** FakeNavigator in `commonTest/.../core/` ✓ · records calls ✓ · overridable back behaviour + simulatable root depth ✓ · doc on seed+assert pattern ✓ · one worked example test ✓ · no changes to Navigator/NavigatorImpl/TabRoutingNavigator/Router/NavigationRoutes/TabNavigationViewModel ✓ · no logic moved into Composables ✓.

**Deviations**
- The example test runs as a plain JVM unit test, not a Compose render. `commonTest` in this project has no Compose UI test runtime (the only rendering tests live in `androidInstrumentedTest`, which `testDebugUnitTest` does not run). The example therefore exercises the same navigation contract and the same `assertEquals(expectedCall, navigator.lastCall)` assertion KIM-293 will drive via real taps — keeping the DoD's `testDebugUnitTest` gate green. The `FakeNavigator` itself is fully usable from the Android Compose UI tests KIM-293 will add.
- `./gradlew buildAndroidOnly` has a pre-existing configuration-cache incompatibility (`Invocation of 'Task.project' by task ':composeApp:buildAndroidOnly' at execution time is unsupported`), unrelated to this change (test-only). Builds green with `--no-configuration-cache`. Flagging in case it's worth a separate fix.

### kimmo.myllyviita@gmail.com — 2026-06-15T12:46:22.183Z

Spec-ready and moved to Todo for Randy.

Scope was substantially reduced from the original Why/Scope after checking current `develop`: `Navigator`/`NavigatorImpl`/`TabRoutingNavigator` are already plain-Kotlin and fakeable (no Koin involved, `NavigatorImpl` takes a constructor-injected `SnapshotStateList<NavRoute>`). `Router.kt` is an empty file and unused — out of scope. No production refactor needed; this is now additive: a `FakeNavigator` test double + one example Compose test demonstrating the seed-route/assert-navigation pattern, ideally covering a slice of a KIM-291 P0 journey (J3 or J7) to give KIM-293 a head start.

