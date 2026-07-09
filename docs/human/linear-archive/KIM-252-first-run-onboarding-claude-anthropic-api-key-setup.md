# KIM-252: First-run onboarding: Claude (Anthropic) API key setup

**Status:** Done · **Priority:** High · **Labels:** spec-ready
**Created:** 2026-05-31T17:30:59.754Z · **Completed:** 2026-07-08T07:55:56.802Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-252/first-run-onboarding-claude-anthropic-api-key-setup
**Related:** related: KIM-255 — Refactor SettingsView: move mutable state and platform calls into SettingsViewModel (MV* violation); related: KIM-245 — First-run onboarding: Visual Crossing API key setup; related: KIM-103 — Handle Claude re-authentication

## Description

## Spec

A brand-new public user installs ReWinds on iOS and has no Anthropic API key configured. Currently the Claude Settings screen exists but offers no first-run prompt, no guidance on where to obtain a key, and no validation feedback on save. Without this, public BYOK users hit a silent failure in the AI chat tab and have no path to resolution. This issue adds first-run detection and contextual onboarding so users can self-serve their Claude API key end-to-end.

**Scope: iOS only. Android key entry is out of scope for Milestone A — Android remains on build-config for internal use.**

## Acceptance criteria

- [ ] On first launch (no Claude API key stored in iOS Keychain), the app surfaces a prompt or banner directing the user to the Claude Settings screen before they attempt to use AI chat
- [ ] The Claude Settings screen displays a visible, tappable link to [https://console.anthropic.com/settings/keys](<https://console.anthropic.com/settings/keys>) with label text explaining it is where to obtain an API key
- [ ] The Settings screen shows an inline validation message when the user saves an empty key field (does not silently accept a blank value)
- [ ] The Settings screen shows a success confirmation (e.g. inline text or toast) when a non-empty key is saved successfully
- [ ] Once a key is saved, the first-run prompt/banner is no longer shown on subsequent launches
- [ ] The UI copy does not use technical jargon (e.g. "token"); it says "API key" throughout
- [ ] External URL opens in system browser (not in-app WebView)

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`); new logic has basic coverage
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations
- [ ] First-run state persisted correctly (key-absent = show prompt; key-present = hide prompt)
- [ ] iOS Keychain is the sole storage mechanism for the API key in this flow

## Notes

Priority set to High; Kimmo reranks freely. Size proposed: M (new UI state + one link + validation).
Reviewers needed: code-reviewer, ux-ui-reviewer (touches Compose UI and user-facing copy), qa-test-agent (first-run state logic).

Assumptions:

* "First run" is defined as: no Claude API key present in iOS Keychain — no separate first-launch flag needed.
* The existing iOS Keychain Settings screen is the target; this issue adds the guidance layer on top, not a new screen.
* Android key entry is explicitly out of scope for Milestone A. Android continues to use BuildConfig (build-time gradle.properties) for internal builds only.

Related: [KIM-245](https://linear.app/kimmo-m/issue/KIM-245/first-run-onboarding-visual-crossing-api-key-setup) (parallel Visual Crossing onboarding), [KIM-103](https://linear.app/kimmo-m/issue/KIM-103/handle-claude-re-authentication) (re-auth failure — separate concern, mid-session only).

## Comments

### kimmo.myllyviita@gmail.com — 2026-07-08T07:56:16.008Z

**Marcy (code-review) — PASS, merged.**

Merged PR #70 into `develop` as merge commit `ecae8ab`. CI green on all three jobs (Android Tests, Android Instrumented/Compose, iOS Kotlin/Native) at merge time. Status → Done, `in-review` removed.

**AC / DoD:** all met in code. MV* clean — all validation/persistence lives in `SettingsViewModel` (`saveAnthropicKey`/`deleteAnthropicKey`/`resetAnthropicKeySaveState` + `AnthropicKeySaveState`) and `ChatViewModel.refreshClaudeKeyNudge()`; the new `AnthropicKeyDialog` and `KeyNudgeBanner` are pure render. The KIM-255/256 logic-in-Composable pattern is **not** reintroduced. Persistence seam (`AnthropicKeyStore` + `PlatformAnthropicKeyStore`) makes the flow unit-testable; tests are real (in-memory fake store, trim/blank/whitespace/delete/reset/default-Idle + nudge shown/hidden/refresh), not trivial. `openUrl` expect/actual is correct on both platforms; commonMain is KMP-safe.

**One behaviour note for the manual checklist (non-blocking):** the inline "✓ API key saved" success text is set and then immediately torn down — on a successful save the dialog does `if (vm.saveAnthropicKey(key)) onDismiss()`, and `onDismiss` calls `resetAnthropicKeySaveState()`, so the dialog closes in the same frame the `Saved` state is set. The user's actual success confirmation is the Settings row flipping to "✓ API key is configured", which satisfies AC #4. But the checklist row for KIM-252 explicitly asserts the inline "✓ API key saved" text appears — it won't, since the dialog dismisses. On-device this should be verified: either accept the row-flip as the confirmation (and reword the checklist expectation), or keep the dialog open on save to show the inline text before an explicit close. Flagging as a checklist/UX-copy item for async verification, not a code-correctness blocker.

Honest limit (same model family as the implementer): I reliably catch spec-drift, MV* violations, and missing coverage — the above is the kind of UX-timing nuance best confirmed on a real device, which is why it's logged to the checklist rather than held as a merge gate.

### kimmo.myllyviita@gmail.com — 2026-07-08T07:53:10.318Z

**Dev handoff — Randy**

- **Branch:** `kimmomyllyviita/kim-252-first-run-onboarding-claude-anthropic-api-key-setup`
- **PR:** https://github.com/kmyllyvi/ReWinds/pull/70 (Closes KIM-252)
- **Build:** ✅ pass — `./gradlew buildAndroidOnly` (Android; shared commonMain compiles, feature iOS-gated)
- **Tests:** ✅ pass — `./gradlew :composeApp:testDebugUnitTest`. CI green on all three jobs (Android Tests, Android Instrumented/Compose, iOS Kotlin/Native).

**Summary**
- First-run nudge in Chat: MV*-driven banner (`ChatViewModel.showClaudeKeyNudge`), re-derived from the Anthropic key store on entry; clears automatically once a key is saved. "First run" = no key in Keychain (no separate flag).
- Settings Anthropic key routed through the ViewModel: inline blank-save validation, "✓ API key saved" success confirmation, and a tappable "get an API key" link opening `console.anthropic.com/settings/keys` in the **system browser**.
- New `openUrl()` platform seam (iOS `UIApplication.openURL` / Android `ACTION_VIEW`) — no in-app WebView.
- New `AnthropicKeyStore` seam + `PlatformAnthropicKeyStore` so save/validate is unit-testable; `SettingsView` stays pure render.
- Reusable `KeyNudgeBanner` component; copy says "API key" throughout (never "token"). Fixed stale chat message that told users to run an `export ANTHROPIC_API_KEY` shell command.

**Deviations**
- None from spec. The existing send-time "API key missing" dialog with "Go to Settings" is kept as a secondary path; this PR adds the proactive first-run banner on top.
- Did not touch Visual Crossing onboarding (KIM-245) or Claude re-auth (KIM-103), per scope.

**Manual test cases:** appended 3 rows to `docs/human/sections/test-checklist.html` (iOS Chat first-run nudge; Settings key dialog link/validation/success; nudge clears + persists across relaunch) — all iOS-on-device/Keychain paths that unit tests can't cover.

Next: handing off to **Marcy (code-reviewer)** in-session (gating), plus **Mr.T (ux-ui-reviewer)** and **Seppo (qa-test-agent)** as advisory reviewers per the ticket.

