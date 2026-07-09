# KIM-290: Add testTag / contentDescription coverage to commonMain screens

**Status:** Done · **Priority:** Urgent · **Labels:** spec-ready, QA
**Created:** 2026-06-15T07:16:14.791Z · **Completed:** 2026-06-15T09:41:49.435Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-290/add-testtag-contentdescription-coverage-to-commonmain-screens
**Parent:** KIM-289 — [EPIC] UI Testing Strategy & Automation
**Related:** related: KIM-287 — Bug: chats don't switch

## Description

## Why (prerequisite — blocks the rest of the epic)

Currently **zero** `Modifier.testTag(...)` usage exists in `commonMain`. The single existing UI test relies on brittle text matching. Both Compose semantic tests and Maestro need stable accessibility identifiers. This is the foundational workstream.

## Scope

Add `Modifier.testTag(...)` (and `contentDescription` where it doubles as an accessibility label) to key interactive / asserted elements across the screens:

* `home/HomeView.kt` — place list items, add-place entry, settings gear, tab bar
* `ai/ChatView.kt` — message list, input field, send button, place pill
* `ai/ChatSessionSwitcher.kt` — session list rows, new-session action
* `settings/SettingsView.kt` — API key field, save/clear actions
* `place/PlaceSummaryView.kt`, `place/MonthlyStatisticsView.kt` — key data containers, calendar selectors, station map trigger

## Conventions to establish

* Define a central `TestTags` object (string constants) so tags are shared between Compose tests and Maestro `id:` selectors and don't drift.
* Tag interactive elements (buttons, inputs, list rows) and the containers that tests need to assert visibility/position on.

## Acceptance criteria

- [ ] `TestTags` constants object exists in `commonMain` (single source of truth)
- [ ] No inline string literals are used as testTag values at call sites — all reference `TestTags` constants
- [ ] `home/HomeView.kt` interactive elements (search bar, place rows, VC key nudge action, settings entry point) have testTags
- [ ] `ai/ChatView.kt` interactive elements (message list, input field, send button, session switcher icon, context chip row) have testTags
- [ ] `ai/ChatSessionSwitcher.kt` interactive elements (session rows, new-chat row) have testTags
- [ ] `settings/SettingsView.kt` interactive elements (API key rows, save/delete/cancel actions, language/units rows) have testTags
- [ ] `place/PlaceSummaryView.kt` and `place/MonthlyStatisticsView.kt` key containers (year selector, month grid cells, station map trigger) have testTags
- [ ] `./gradlew buildAndroidOnly` stays green
- [ ] Existing `ChatLayoutTest.kt` still passes unmodified (migration to testTags is [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android-run-on-every-pr)'s job, not this ticket's)

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] No new tests required — this ticket adds identifiers/modifiers only, no new logic (pure annotation of existing Composables)
- [ ] No MV* violations (see ARCHITECTURE-RULES.md) — testTags are static labels, must not introduce state or logic into Views
- [ ] No new lint violations

## Notes

Reviewers needed: code-reviewer (check no logic crept into Views via this change), ux-ui-reviewer (spot-check that testTag additions don't alter layout/spacing — testTag is a no-op modifier but flagging per epic convention).

Priority: Urgent — hard prerequisite for [KIM-291](https://linear.app/kimmo-m/issue/KIM-291/define-5-10-critical-user-journeys-for-ui-test-automation) (already specced), [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android-run-on-every-pr), [KIM-294](https://linear.app/kimmo-m/issue/KIM-294/maestro-setup-cross-platform-e2e-smoke-suite-android-ios). Retained from Seppo's triage.

## References

`composeApp/src/commonMain/kotlin/{home,ai,settings,place}/`

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-15T11:27:37.985Z

## Rebase / conflict-resolution pass (follow-up on commit 5d4e93f)

Merged current `origin/develop` into `kim-290-testtag-coverage` to clear the conflict introduced after this branch was cut.

**Conflict:** `ai/ChatView.kt` only. KIM-287 (PR #27, "static place pill") had since **replaced** the interactive `ContextChipRow`/`ContextChip` composables that KIM-290 tagged with a single non-interactive `PlaceTagPill(placeName)`. The tagged components no longer exist on develop.

**Resolution:**
- Dropped `TestTags.CHAT_CONTEXT_CHIP_ROW` and `TestTags.CHAT_CONTEXT_CHIP`.
- Added `TestTags.CHAT_PLACE_TAG_PILL` (`"chat_place_tag_pill"`), applied to `PlaceTagPill`'s root `Row`. A single tag is the right model now — it is one static pill, not a row of interactive chips.
- `Router.kt` and `PlaceSummaryView.kt` auto-merged cleanly; their KIM-290 testTags are intact. No stale references to the removed constants remain.

**Verification (Android pilot):**
- `./gradlew :composeApp:compileDebugKotlinAndroid -PincludeAllTargets=false` — BUILD SUCCESSFUL
- `./gradlew :composeApp:testDebugUnitTest -PincludeAllTargets=false` — BUILD SUCCESSFUL (all green)
- `ChatLayoutTest.kt` (androidInstrumentedTest, text-matching) references none of these constants — unaffected, unmodified.

PR #29 is now **MERGEABLE / CLEAN** (no conflicts). Branch pushed: `kim-290-testtag-coverage` @ `76070c6`.

