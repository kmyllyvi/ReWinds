# KIM-294: Maestro setup + cross-platform E2E smoke suite (Android + iOS)

**Status:** Done · **Priority:** High · **Labels:** spec-ready
**Created:** 2026-06-15T07:17:17.841Z · **Completed:** 2026-06-15T12:45:17.104Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-294/maestro-setup-cross-platform-e2e-smoke-suite-android-ios
**Parent:** KIM-289 — [EPIC] UI Testing Strategy & Automation
**Related:** related: KIM-287 — Bug: chats don't switch; related: KIM-290 — Add testTag / contentDescription coverage to commonMain screens; related: KIM-291 — Define 5-10 critical user journeys for UI test automation

## Description

## Spec

Layer 2 of the UI testing strategy: a black-box E2E smoke suite using Maestro (YAML, accessibility-tree driven) that drives the built Android app (emulator) and iOS app (simulator) through the P0 critical journeys defined in [KIM-291](https://linear.app/kimmo-m/issue/KIM-291/define-5-10-critical-user-journeys-for-ui-test-automation). This is also the **primary iOS path** ([KIM-295](https://linear.app/kimmo-m/issue/KIM-295/decide-ios-ui-test-approach-maestro-only-vs-native-compose-ui-test)) — it avoids standing up a native iOS Compose UI test target. Maestro selectors use the `id:` (testTag/contentDescription) values from [KIM-290](https://linear.app/kimmo-m/issue/KIM-290/add-testtag-contentdescription-coverage-to-commonmain-screens)'s `core/TestTags.kt`.

## Depends on

* [KIM-290](https://linear.app/kimmo-m/issue/KIM-290/add-testtag-contentdescription-coverage-to-commonmain-screens) (testTags) — implemented on branch `kim-290-testtag-coverage` (commit 5d4e93f), not yet merged. Randy should rebase/merge onto this branch before starting, or wait for merge to develop.
* [KIM-291](https://linear.app/kimmo-m/issue/KIM-291/define-5-10-critical-user-journeys-for-ui-test-automation) (critical journeys) — done, `spec-ready`/Todo. 8 P0 journeys (J1-J8) are the smoke set this ticket must cover; the 3 P1 journeys (J9-J11) are nice-to-have.

## Scope

* Install/pin Maestro CLI; add a `.maestro/` flows directory; document version pinning and local run instructions (against an Android emulator and an iOS simulator).
* Author Maestro YAML flows for the 8 P0 journeys from [KIM-291](https://linear.app/kimmo-m/issue/KIM-291/define-5-10-critical-user-journeys-for-ui-test-automation), using `TestTags` constants as `id:` selectors:
  * J1 — empty state / first launch (`HOME_SEARCH_FIELD`, VC key nudge banner, empty place list)
  * J2 — add a place via search (`HOME_SEARCH_FIELD`, `HOME_SEARCH_SUGGESTION`, `HOME_PLACE_ROW`)
  * J3 — open a saved place's summary (`HOME_PLACE_ROW` → `PLACE_YEAR_SELECTOR`, `PLACE_MONTH_GRID`)
  * J4 — view monthly statistics for a FULL month (`PLACE_MONTH_CELL` → `MONTH_STAT_CARD_GRID`, `MONTH_DAY_LIST`)
  * J5 — download missing days for a NO_DATA month (`PLACE_MONTH_CELL` → `DownloadMissingDaysDialog`)
  * J6 — ask AI chat a weather question (`TAB_CHAT`, `CHAT_INPUT_FIELD`, `CHAT_SEND_BUTTON`, `CHAT_MESSAGE_LIST`)
  * J7 — chat blocked when Anthropic key missing (`CHAT_SEND_BUTTON` → API key dialog → Settings nav)
  * J8 — switch between chat sessions (`CHAT_SESSION_SWITCHER_BUTTON`, `CHAT_SESSION_LIST`, `CHAT_SESSION_LIST_ITEM`)
* Note any platform-specific forks (e.g. permission dialogs, keyboard behaviour differences) inline in the relevant flow files.
* Wire into a nightly / pre-release CI job (NOT every PR — these run against a built app + emulator/simulator, too slow/costly for per-PR gating).
* Document local run instructions for both emulator and simulator in `.maestro/README.md`.

## Acceptance criteria

- [ ] Maestro CLI version pinned (documented in `.maestro/README.md`); `.maestro/` flows directory committed
- [ ] All 8 P0 journeys (J1-J8 from [KIM-291](https://linear.app/kimmo-m/issue/KIM-291/define-5-10-critical-user-journeys-for-ui-test-automation)) implemented as Maestro flows, each using `TestTags` constants (via `id:` selectors) — no ad-hoc text/coordinate-based selectors for elements that have a `TestTags` constant
- [ ] Flows pass against an Android emulator
- [ ] Flows pass against an iOS simulator, or platform-specific forks are documented per-flow with the reason
- [ ] A nightly/pre-release CI job runs the suite and reports pass/fail results
- [ ] Local run instructions documented in `.maestro/README.md` for both emulator and simulator
- [ ] If any `TestTags` constant needed by a journey doesn't exist yet, this is called out explicitly (don't invent ad-hoc selectors silently)

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit for all non-trivial logic (ViewModel, Repository, pure functions, bug fixes). No "tests later". Exempt only: pure UI styling, config/doc-only changes — state the reason explicitly.
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations
- [ ] No new tests required beyond the Maestro flows themselves — this ticket adds test infrastructure/config (`.maestro/` YAML, CI job, docs), not app logic

## Notes

Priority set by Seppo (High), retained. Reviewers needed: code-reviewer [+ qa-test-agent, since this is QA infra].

[KIM-295](https://linear.app/kimmo-m/issue/KIM-295/decide-ios-ui-test-approach-maestro-only-vs-native-compose-ui-test) (iOS UI test approach decision) defaults to "Maestro-only for iOS" — this ticket's iOS simulator results are the evidence for that decision.

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-15T12:22:22.071Z

**Dev → Review handoff**

- **Branch:** `kim-294-maestro-e2e-suite`
- **PR:** https://github.com/kmyllyvi/ReWinds/pull/30 (Closes KIM-294)
- **Build:** pass — `:composeApp:assembleDebug` green
- **Tests:** pass — `:composeApp:testDebugUnitTest` green
- **New tests written:** no — config/doc-only ticket (test *infrastructure*, not app logic); covered by the AC exemption "No new tests required beyond the Maestro flows themselves". No app/Gradle code changed.

**Summary**

Added `.maestro/` E2E smoke suite (Layer 2 / KIM-289):
- 8 Maestro YAML flows for P0 journeys J1–J8 (KIM-291), each using `TestTags` constants as `id:` selectors. One flow per journey + suite `config.yaml`.
- Maestro CLI pinned to **1.39.0**; `.maestro/README.md` documents pinning + local run instructions for both Android emulator and iOS simulator. Cross-platform via `${APP_ID}` (Android `com.km.rewinds`; iOS team-suffixed bundle id).
- Nightly/pre-release CI job `.github/workflows/nightly-e2e.yml` (cron + `workflow_dispatch`, JUnit report artifact) — deliberately not per-PR.
- Platform forks documented inline + in README: `hideKeyboard` (iOS keyboard overlap; no-op Android), English-locale assumption for text fallbacks.

**Selector gaps — called out explicitly per AC (no silent ad-hoc selectors)**

Three journeys touch elements that have **no `TestTags` constant** on `develop`. Each uses an explicit, commented text/index fallback, documented in the flow and the README "Selector gaps" section:
1. **Month-cell state (J4 FULL / J5 NO_DATA)** — `PLACE_MONTH_CELL` is non-indexed and state-agnostic; nothing distinguishes FULL vs NO_DATA by id. Flows tap `index: 0` + rely on a seeded precondition.
2. **`DownloadMissingDaysDialog` (J5)** — `PlaceSummaryView.kt:181` has no testTag on title/Download/Cancel → localized text fallback.
3. **Chat API-key-missing `AlertDialog` (J7)** — `ChatView.kt:203` has no testTag on title/"Go to Settings" → localized text fallback; post-nav assertion uses `SETTINGS_ANTHROPIC_KEY_ROW` (locale-independent).

Recommended follow-up (KIM-290 territory, out of scope here): add `PLACE_MONTH_CELL_FULL`/`_NO_DATA`, `DOWNLOAD_DIALOG_*`, `CHAT_API_KEY_DIALOG_*` constants and replace the text fallbacks with `id:` selectors.

**Deviations / notes**

- Per the spec update, J6/J8 use `CHAT_PLACE_TAG_PILL` (KIM-287/PR#27 removed the old chip-row constants) — confirmed against current `core/TestTags.kt`.
- **Flows not yet executed against a live emulator/simulator** in this session (no running device + provisioned VC/Anthropic keys available). The nightly CI job is the intended execution path; AC items "flows pass against emulator/simulator" need a first green run there. iOS CI is behind an opt-in `vars.ENABLE_IOS_E2E` flag until simulator provisioning/signing is wired up (feeds the KIM-295 decision).
- Pre-existing unrelated issue: `./gradlew buildAndroidOnly` fails the configuration-cache check (`Task.project` at execution time) — not caused by this PR (no Gradle changes). Verified via canonical `assembleDebug` + `testDebugUnitTest` instead, both green.

### kimmo.myllyviita@gmail.com — 2026-06-15T09:30:06.453Z

Spec-ready, moved to Todo — ready for Randy.

One real sequencing dependency: this depends on KIM-290's `core/TestTags.kt`, which is implemented and committed on branch `kim-290-testtag-coverage` (commit `5d4e93f`) but not yet merged to develop. Randy should either rebase this work onto that branch or wait for it to merge before starting, so the `id:` selectors reference real constants.

AC/DoD now reference the actual `TestTags` constants and the 8 P0 journeys (J1-J8) from KIM-291.

### kimmo.myllyviita@gmail.com — 2026-06-15T09:27:55.680Z

**Blocked before speccing — dependency mismatch (needs-human)**

I was asked to bring this to spec-ready + Todo on the premise that KIM-290 ("TestTag/contentDescription coverage" with a completed `TestTags` constants object) and KIM-291 (critical journeys) are both done.

Checked both:

- **KIM-291**: genuinely done — status Todo, `spec-ready`, 11 journeys (8 P0 + 3 P1) fully documented. ✅ Good to reference in this spec.
- **KIM-290**: still status **Todo**, not Completed. I searched the whole `commonMain` tree for `testTag`/`TestTag` — there are **zero** matches, and no `TestTags.kt` file exists anywhere in the repo. The constants object KIM-290 is supposed to produce has not been implemented yet.

KIM-294's own dependency line says it explicitly: *"KIM-290 (testTag / accessibility ids) — Maestro `id:` selectors rely on them."* Writing Maestro flows with `id:` selectors against a `TestTags` object that doesn't exist isn't feasible — Randy would either have to invent the tags himself (duplicating/conflicting with KIM-290's eventual work) or write brittle text-based selectors that the rest of the epic explicitly wants to avoid.

**What I'd recommend instead of guessing:**
1. KIM-290 ships first (it's already spec-ready in Todo — just needs to be picked up), giving us real `TestTags` constants to reference.
2. Then I (Shirley) re-spec KIM-294 referencing the actual constant names, with AC tied to KIM-291's 11 journeys.

I don't want to either (a) invent a `TestTags` object in this spec that may not match what KIM-290 actually produces, or (b) move KIM-294 to Todo ahead of a hard prerequisite that isn't met — that's a sequencing/scope call, not a minor wording decision.

Holding KIM-294 in Backlog without `spec-ready` for now, adding `needs-human` and assigning Kimmo. Once KIM-290 lands (or you tell me to proceed with placeholder tag names pending KIM-290), I'll do the full spec-ready pass on KIM-294 referencing concrete `TestTags` constants and the KIM-291 journey list.

— Shirley (PO)

