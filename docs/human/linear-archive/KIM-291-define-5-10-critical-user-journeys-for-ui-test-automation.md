# KIM-291: Define 5-10 critical user journeys for UI test automation

**Status:** Done · **Priority:** Urgent · **Labels:** spec-ready
**Created:** 2026-06-15T07:16:26.114Z · **Completed:** 2026-06-15T09:41:47.573Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-291/define-5-10-critical-user-journeys-for-ui-test-automation
**Parent:** KIM-289 — [EPIC] UI Testing Strategy & Automation
**Related:** related: KIM-290 — Add testTag / contentDescription coverage to commonMain screens

## Description

## Spec

Produce the canonical list of critical user journeys for ReWinds that [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android-run-on-every-pr) (Compose semantic flow tests) and [KIM-294](https://linear.app/kimmo-m/issue/KIM-294/maestro-setup-cross-platform-e2e-smoke-suite-android-ios) (Maestro E2E suite) will implement against. Each journey is written as a step-by-step flow with explicit preconditions, steps, and expected outcomes, grounded in the actual screens (`home/HomeView`, `ai/ChatView`, `ai/ChatSessionSwitcher`, `place/PlaceSummaryView`, `place/MonthlyStatisticsView`, `settings/SettingsView`). Journeys reference UI elements by description; [KIM-290](https://linear.app/kimmo-m/issue/KIM-290/add-testtag-contentdescription-coverage-to-commonmain-screens) (in progress, in parallel) will introduce the `TestTags` constants object — dev/QA wire concrete tag names to these descriptions when implementing [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android-run-on-every-pr)/294.

Journeys are split into **P0 (smoke — must be in both Compose and Maestro suites)** and **P1 (secondary — Maestro nice-to-have, Compose optional)**.

---

### P0 — Smoke journeys (8)

**J1 — First launch / empty state with no API keys configured**

* Preconditions: fresh install, no places saved, no Anthropic/Visual Crossing keys configured.
* Steps:
  1. Launch app → Home tab is shown.
  2. Observe the non-dismissible "VC key nudge" banner is visible (since `isWeatherKeyConfigured == false`).
  3. Observe the places list is empty (no `PlaceRow`s, no skeletons after load completes).
* Expected outcome: Home renders with header, search bar, empty list, and the VC key nudge banner. No crash, no error dialog.
* Screens/elements: `home/HomeView` — header, search bar (`PlacesSearchBar`), VC key nudge banner (`VcKeyNudgeBanner`, "set up now" action), empty places list.

**J2 — Add a place via search**

* Preconditions: app launched, Home tab active, network reachable.
* Steps:
  1. Tap the search field and type a place name (e.g. "Helsinki").
  2. Wait for `isSearching` indicator to appear then resolve to a suggestions list.
  3. Tap a suggestion from the results.
* Expected outcome: search field clears/loses focus, the new place appears as a `PlaceRow` in the list with a status dot and subtitle.
* Screens/elements: `home/HomeView` — `PlacesSearchBar` text field, suggestion list (`SuggestionCell`), resulting `PlaceRow`.

**J3 — Open a saved place's summary**

* Preconditions: at least one place saved (seed via fake repo for Compose test; real flow for Maestro).
* Steps:
  1. From Home, tap a `PlaceRow`.
  2. Wait for navigation to `PlaceSummaryView` (route `PlaceSummaryRoute`).
  3. Observe the loading state, then the success state.
* Expected outcome: header shows the place name; year selector strip and month grid render; tapping back returns to Home.
* Screens/elements: `home/HomeView` (`PlaceRow` tap), `place/PlaceSummaryView` — header with place name, back button, map icon button, chat button, `YearSelectorStrip`, `MonthGrid`.

**J4 — View monthly statistics for a downloaded month**

* Preconditions: place summary open, at least one month cell in `FULL` state (data downloaded).
* Steps:
  1. On `PlaceSummaryView`, tap a month cell whose state is `FULL` (not `NO_DATA`).
  2. Observe navigation to `MonthlyStatisticsView` (route `MonthlyStatisticsRoute`).
* Expected outcome: monthly statistics screen renders for the selected place/year/month without error.
* Screens/elements: `place/PlaceSummaryView` — `MonthGrid` / `MonthCell` (FULL state), `place/MonthlyStatisticsView`.

**J5 — Download missing days for a NO_DATA month**

* Preconditions: place summary open, at least one month cell in `NO_DATA` state.
* Steps:
  1. Tap a `NO_DATA` month cell.
  2. Observe the "Download Missing Days" dialog (`DownloadMissingDaysDialog`) appears with the correct month/year text.
  3. Tap the "Download" confirm action.
* Expected outcome: dialog dismisses; cell state updates away from `NO_DATA` once download completes (Compose test can assert via fake repo state change; Maestro asserts dialog dismissal and no crash).
* Screens/elements: `place/PlaceSummaryView` — `MonthCell` (NO_DATA), `DownloadMissingDaysDialog` (title, message, Download/Cancel buttons).

**J6 — Ask the AI chat a weather question and receive a response**

* Preconditions: Anthropic API key configured (fake/mocked for Compose; real or test key for Maestro), at least one place exists.
* Steps:
  1. Navigate to the Chat tab (`ChatView`).
  2. Type a question into the chat input field (e.g. "What's the wind forecast for Helsinki?").
  3. Tap the send button.
  4. Observe the loading indicator appears, then a new assistant message bubble is added to the message list.
* Expected outcome: user message bubble appears immediately; assistant message bubble appears after response; input field clears; send button disables while `isSendEnabled == false`.
* Screens/elements: `ai/ChatView` — `ChatInputArea` (text field, send `IconButton`), message `LazyColumn` (`ChatMessageBubble`), loading `CircularProgressIndicator`.

**J7 — Chat blocked when Anthropic API key is missing**

* Preconditions: no Anthropic API key configured.
* Steps:
  1. Navigate to Chat tab.
  2. Type a message and tap send.
  3. Observe the "API Key Not Configured" `AlertDialog` appears.
  4. Tap "Go to Settings".
* Expected outcome: dialog appears with title/message from `strings.apiKeyNotConfigured*`; tapping "Go to Settings" navigates to `SettingsView` and dismisses the dialog.
* Screens/elements: `ai/ChatView` — API key missing `AlertDialog` (Go to Settings / Dismiss buttons), navigation to `settings/SettingsView`.

**J8 — Switch between chat sessions**

* Preconditions: at least two chat sessions exist (one active, one inactive) — seed via fake `ChatRepository`/Koin for Compose, or create via "New chat" in Maestro.
* Steps:
  1. On `ChatView`, tap the session switcher icon in the header (`Icons.AutoMirrored.Filled.List`).
  2. Observe `ChatSessionSwitcher` bottom sheet opens, listing sessions with relative time + message count, active session marked with a check icon.
  3. Tap a non-active session row.
* Expected outcome: bottom sheet dismisses; `ChatView` now shows the messages belonging to the selected session; the selected session is marked active if the switcher is reopened.
* Screens/elements: `ai/ChatView` — session switcher header icon button, `ai/ChatSessionSwitcher` — `SessionRow` (title, relative time, message count, active check icon), `NewChatRow`.

---

### P1 — Secondary journeys (3)

**J9 — Start a new chat session**

* Preconditions: chat session switcher open.
* Steps:
  1. Open the session switcher.
  2. Tap "New chat" (`NewChatRow`).
* Expected outcome: bottom sheet dismisses; `ChatView` shows an empty message list and the new session becomes active (visible as the top/active entry when switcher reopened).
* Screens/elements: `ai/ChatSessionSwitcher` — `NewChatRow`.

**J10 — Configure the Anthropic API key in Settings**

* Preconditions: Settings open, Anthropic key not configured.
* Steps:
  1. Navigate to Settings tab.
  2. Tap the Anthropic API key row (`SettingsKeyRow`, shows "Not set" chip).
  3. Enter a key value in the dialog's text field and tap "Save".
* Expected outcome: dialog dismisses; the Anthropic key row now shows the "Configured" chip; returning to Home dismisses the VC-key-style nudge if it was the Anthropic key (note: VC key nudge is specifically for the Visual Crossing key — confirm only the relevant row updates).
* Screens/elements: `settings/SettingsView` — `SettingsKeyRow` (Anthropic), `ApiKeyDialog` (text field, Save/Delete/Cancel).

**J11 — Switch app language**

* Preconditions: Settings open.
* Steps:
  1. Tap the Language row in the General settings group.
  2. Observe the displayed value toggles between English and German.
* Expected outcome: language value updates immediately; a sampling of strings elsewhere (e.g. Home header title) reflects the new language on next render.
* Screens/elements: `settings/SettingsView` — General group, Language `SettingsValueRow`.

---

## Acceptance criteria

- [ ] 11 journeys documented above, each with preconditions, numbered steps, and an expected outcome
- [ ] Journeys are split into P0 (8, smoke — required in both Compose and Maestro suites) and P1 (3, secondary)
- [ ] Each journey lists the concrete screens/composables and UI elements it touches, described precisely enough to map to `TestTags` once [KIM-290](https://linear.app/kimmo-m/issue/KIM-290/add-testtag-contentdescription-coverage-to-commonmain-screens) lands
- [ ] This issue is linked from [KIM-289](https://linear.app/kimmo-m/issue/KIM-289/epic-ui-testing-strategy-and-automation) (epic) as the canonical journey list for [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android-run-on-every-pr) and [KIM-294](https://linear.app/kimmo-m/issue/KIM-294/maestro-setup-cross-platform-e2e-smoke-suite-android-ios) to implement against

## Definition of done

- [ ] N/A — this is a documentation/spec deliverable, not a code change. No build/test/lint impact.
- [ ] Reviewed against [KIM-290](https://linear.app/kimmo-m/issue/KIM-290/add-testtag-contentdescription-coverage-to-commonmain-screens)'s eventual `TestTags` object once that PR lands, to confirm no journey references an element that wasn't tagged (follow-up check, not blocking this ticket)

## Notes

**Assumptions surfaced:**

* "Onboarding / first launch" (mentioned in the epic's candidate list) is folded into J1 (empty-state Home) — there is no separate onboarding wizard in the codebase today; first launch is simply Home with no data and the VC key nudge.
* J5 (download missing days) and J9-J11 were added beyond the epic's candidate list because they are observable, testable flows already present in the code and add good smoke coverage at low marginal authoring cost. Kimmo/Seppo can demote any P1 item or cut it entirely without affecting P0 scope.
* "View place summary / monthly statistics" was split into two journeys (J3, J4) because they are two distinct navigation hops with different preconditions (J4 requires a FULL month to exist) — keeps each journey atomic for test authoring.
* Settings → Visual Crossing key configuration was not added as its own journey since J10 (Anthropic key) already exercises the identical `ApiKeyDialog` component; Visual Crossing can reuse the same test with a different row selector if QA wants belt-and-braces coverage.

**Sizing:** This ticket is a spec-only deliverable (no code), so it doesn't need splitting. Engineering tickets [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android-run-on-every-pr)/294 that consume this list may still need splitting per-journey when scoped — that's their call at spec time.

**Reviewers needed:** none required for this ticket itself (no code diff). [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android-run-on-every-pr)/294 specs (when written) should reference this list; their code diffs need code-reviewer, and ux-ui-reviewer if any UI changes are made incidentally while adding testTags ([KIM-290](https://linear.app/kimmo-m/issue/KIM-290/add-testtag-contentdescription-coverage-to-commonmain-screens) territory).

Priority: Urgent (set by Seppo, retained — this is the hard blocker for [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android-run-on-every-pr)/294). Size: small (1 session) — spec-only.

## Comments

_No comments._
