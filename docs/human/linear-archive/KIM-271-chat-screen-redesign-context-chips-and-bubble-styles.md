# KIM-271: Chat screen redesign — context chips and bubble styles

**Status:** Done · **Priority:** Medium · **Labels:** design, in-review, spec-ready, Feature
**Created:** 2026-06-04T12:10:57.688Z · **Completed:** 2026-06-09T09:38:31.842Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-271/chat-screen-redesign-context-chips-and-bubble-styles
**Related:** related: KIM-266 — Background isobar texture — shared Canvas layer; related: KIM-267 — Tab bar navigation — Places / Chat / Settings; related: KIM-265 — Design token / theme system — Midnight Blue palette

## Description

## Spec

Implement Chat screen design from /docs/designs/screens-v1.html

Redesign the Chat screen with the Midnight Blue theme. New elements: context chips at the top of the message list (showing the selected place or "All places"), updated chat bubble styles for user and assistant messages, and the input bar positioned above the tab bar. No changes to AI logic or ViewModel behaviour — this is a pure UI restyle.

## Acceptance criteria

- [ ] Screen background is `pageBg` with isobar texture as the lowest layer
- [ ] A horizontal row of context chips is rendered above the message list; chips show the currently selected place name or "All places"; chip background is `surfaceRaised`, text is `accentBlue`; selected chip has a border in `accentBlue`
- [ ] Which chip is selected is driven by ViewModel state, not local `remember` state
- [ ] User message bubbles use `surfaceRaised` background, `textPrimary` text, right-aligned
- [ ] Assistant message bubbles use `surface` background, `textPrimary` text, left-aligned
- [ ] The text input field is positioned above the tab bar and does not overlap it; background `surface`, border `border`, placeholder in `textTertiary`, send button in `accentBlue`
- [ ] Send button is disabled (greyed, `textTertiary`) when the input is empty; enabled when non-empty — this state is driven by the ViewModel
- [ ] The screen compiles and renders inside the new tab bar shell ([KIM-267](https://linear.app/kimmo-m/issue/KIM-267/tab-bar-navigation-places-chat-settings))

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`); new logic has basic coverage
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations
- [ ] All colour values reference theme tokens
- [ ] Blocked by: [KIM-265](https://linear.app/kimmo-m/issue/KIM-265/design-token-theme-system-midnight-blue-palette) (tokens), [KIM-266](https://linear.app/kimmo-m/issue/KIM-266/background-isobar-texture-shared-canvas-layer) (isobar), [KIM-267](https://linear.app/kimmo-m/issue/KIM-267/tab-bar-navigation-places-chat-settings) (tab nav)

## Notes

Priority: Medium.
Proposed size: Medium (2 hours). Mostly visual restyle of an existing screen.
Reviewers needed: code-reviewer, ux-ui-reviewer.
Reference: `docs/designs/screens-v1.html` (fourth mockup, "Chat").
Assumption: the context chip's place-selection logic (which place filters the chat) is already handled in `ChatViewModel`; this ticket only styles the chip UI.

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-07T08:24:08.278Z

Implementation complete — ready for review.

**Branch:** `kimmomyllyviita/kim-271-chat-screen-redesign-context-chips-and-bubble-styles`
**PR:** https://github.com/kmyllyvi/ReWinds/pull/21
**Build:** PASS (`compileDebugKotlinAndroid` succeeds)
**Tests:** PASS (`testDebugUnitTest` — 12/12 in `ChatViewModelErrorHandlingTest`, incl. new chip-selection and send-enablement tests)

**Summary**
- Pure UI restyle of the Chat screen to Midnight Blue — no AI/ViewModel-behaviour/business-logic changes.
- Page background uses `pageBg` with `IsobarBackground` as the lowest layer.
- New horizontal context-chip row above the message list (selected place or "All places"). Selection is driven by ViewModel state (`ChatUiState.contextChips` + `selectContextChip`), not local `remember`. Chips populated from `getSavedPlaceNames()`.
- Bubbles: user `surfaceRaised`, assistant `surface`, both `textPrimary`.
- Input bar: `surface` background, `border` outline, `textTertiary` placeholder, `accentBlue` send button greyed to `textTertiary` when disabled. Send enablement derived in the ViewModel (`ChatUiState.isSendEnabled`).
- All chip/bubble/input colours use `MaterialTheme.rewinds.*` tokens — no hard-coded hex.

**Deviations / notes**
- The spec assumed place-selection logic already existed in `ChatViewModel`; it did not. I added minimal VM state (`contextChips`, `selectContextChip`, `isSendEnabled`) to satisfy the "driven by ViewModel state, not local remember" acceptance criteria, without touching AI logic.
- Input bar sits inside the KIM-267 Scaffold body, so `innerPadding` already keeps it above the tab bar (no manual offset needed).
- iOS not built via Gradle per project policy — needs manual Xcode verification.
- `./gradlew buildAndroidOnly` aggregate task hits a pre-existing config-cache problem (`Task.project` at execution time) unrelated to this change; the underlying compile and test tasks both pass.

