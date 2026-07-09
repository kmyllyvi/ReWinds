# KIM-246: Error handling for missing or invalid API keys (Claude + Visual Crossing)

**Status:** Done · **Priority:** Urgent · **Labels:** in-review, spec-ready
**Created:** 2026-05-31T17:09:49.160Z · **Completed:** 2026-06-01T19:54:10.205Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-246/error-handling-for-missing-or-invalid-api-keys-claude-visual-crossing

## Description

## Spec

The app currently has partial error handling for missing API keys: the Claude (Anthropic) missing-key case shows a dialog, but that dialog has no "Go to Settings" action. The Visual Crossing missing-key case is handled differently (throws `IllegalStateException`). Neither service distinguishes an invalid/expired key (HTTP 401/403) from other failures — both fall through to a generic error string. This issue adds two new UI states — "key missing" and "key invalid" — for both services, each with a clear call to action that navigates the user to the Settings screen.

Scope: `ChatViewModel`, `HomeViewModel` (for VC fetch errors surfaced from `PlaceSummaryViewModel` / `WeatherRepository`), and the UI layer that consumes those states. No changes to keychain storage or key validation logic.

## Acceptance criteria

- [ ] When the user attempts a chat message and the Anthropic key is absent (fails `ApiKeyManager.hasValidKey()`), a dialog is shown with title indicating the Claude key is not configured and a "Go to Settings" button that navigates to the Settings screen.
- [ ] When the user attempts a chat message and the Anthropic key is absent, dismissing the dialog without tapping "Go to Settings" closes the dialog and does not send the message.
- [ ] When the Anthropic API returns an HTTP 401 or 403 response (caught as `AnthropicException` containing status 401 or 403), `ChatViewModel` exposes a distinct UI state that the view renders as a dismissible error message distinguishable from generic errors, with a "Go to Settings" action button.
- [ ] When the Visual Crossing API returns an HTTP 401 or 403 response (caught as `NetworkException` containing status 401 or 403), the calling ViewModel exposes a distinct "VC key invalid" UI state that the view renders with a "Go to Settings" action button.
- [ ] When the Visual Crossing key is absent (fails `WeatherApiKeyManager.hasValidKey()`) at the point a network fetch is attempted, the calling ViewModel exposes a distinct "VC key missing" UI state that the view renders with a "Go to Settings" action button.
- [ ] The "Go to Settings" action in all four cases above navigates the user to the Settings screen using the existing `Navigator`/`Router` — no new navigation routes are introduced.
- [ ] The existing dialog for missing Anthropic key in `ChatView` gains a "Go to Settings" confirm button (in addition to or replacing the current dismiss-only button) that navigates to Settings.
- [ ] All new error-state logic lives in ViewModels, not in Composable functions — no `remember { mutableStateOf(...) }` in View files for these states.
- [ ] `ChatUiState` is extended with a new field (or enum value) to represent the "key invalid" case, distinct from the existing `error: String?` and `showApiKeyMissingDialog: Boolean` fields.
- [ ] No regression: generic (non-auth) network errors still surface as the existing dismissible error banner without a "Go to Settings" button.

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`); new ViewModel state transitions for missing-key and invalid-key cases have unit test coverage
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md) — all error-state logic in ViewModels
- [ ] No new lint violations
- [ ] The four scenarios (Claude missing, Claude invalid, VC missing, VC invalid) are manually verified on Android emulator
- [ ] Existing chat and weather-fetch flows are not broken

## Notes

Priority: Urgent (set by po) — this is Milestone A / MVP; users who misconfigure their BYOK keys currently get silent failures or raw exception strings.

Size estimate: Medium (2–3 days). Four distinct error paths to wire, but the UI pattern is the same each time and Settings navigation already exists.

Reviewers needed: code-reviewer, qa-test-agent (multiple error paths), ux-ui-reviewer (new dialog action button and error states touch Compose UI).

Assumptions:

* "Invalid/expired key" is defined as HTTP 401 or 403 from the respective API. Other HTTP errors (429, 5xx, network timeout) remain generic.
* The Visual Crossing error surface point is whichever ViewModel initiates the download (likely `HomeViewModel` for adding a place, `PlaceSummaryViewModel` for refresh). Developer to confirm the right call sites — if more than two ViewModels are involved, they may propose splitting the VC scope into a follow-on issue.
* No new string resources need translating beyond English for this spec; localisation is a separate concern.

## Comments

### kimmo.myllyviita@gmail.com — 2026-05-31T20:52:15.527Z

**Developer handover — KIM-246**

**Branch:** `kim-246-api-key-error-handling`
**PR:** https://github.com/kmyllyvi/ReWinds/pull/6

**Build:** PASS — `./gradlew buildAndroidOnly` exit 0 (Android tasks all green)
**Tests:** PASS — `./gradlew :composeApp:testDebugUnitTest` BUILD SUCCESSFUL, 36 tasks

**Summary of changes:**

- `AnthropicException` and `NetworkException` now carry an optional `httpStatus: Int?` so callers can distinguish 401/403 auth failures from generic errors
- `ChatViewModel` catches `AnthropicException` before the generic `Exception` handler; 401/403 sets `showApiKeyInvalidError = true`, others fall through to the existing `error` string
- `ChatView` existing missing-key dialog gains a "Go to Settings" confirm button; new `showApiKeyInvalidError` dialog added with the same pattern
- `HomeViewModel` catches `IllegalStateException` (VC key absent) → `VcKeyErrorType.MISSING` and `NetworkException` 401/403 → `VcKeyErrorType.INVALID`; generic errors still go to `error: String?`
- `HomeView` renders a `VcKeyErrorDialog` composable (private, stateless) driven by `vcKeyError` in `HomeUiState`
- All string copies (EN + DE) added to `AppStrings`
- 3 new unit test files: `ChatViewModelErrorHandlingTest`, `NetworkExceptionTest`, `HomeViewModelVcKeyErrorTest`

**Deviations from spec:** None. All four error paths implemented as specified.

