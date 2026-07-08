# Testable seams for platform-backed state

ViewModels that read platform key stores expose constructor-injected seams defaulting to
the platform singleton, so commonTest can substitute in-memory fakes:

- `ApiKeyChecker` (fun interface, `isAnthropicKeyConfigured()`) — inject `ApiKeyChecker { false/true }`.
  Default `PlatformApiKeyChecker` reads `BuildConfig.ANTHROPIC_API_KEY` baked from env at build
  time, so tests MUST inject a fake or "absent" assertions flake on runners where the env key is set.
- `AnthropicKeyStore` (save/delete) — inject a `FakeAnthropicKeyStore` counting saves/deletes.
- `WeatherApiKeyManager` is a shared singleton (not injected); tests that touch it must reset it
  (`setApiKey("")`) in a finally block or `@AfterTest` to avoid cross-test leakage.

"First run" in this app = no key in store; derived state (`showClaudeKeyNudge`) is re-computed
from the checker via a `refresh...()` method, never a persisted flag. Test the transition by
flipping a mutable `var configured` behind the fun-interface and calling refresh.

Real Keychain-absent path + system-browser hand-off are iOS-on-device only → manual rows in
`docs/human/sections/test-checklist.html`, not unit-testable.

Test infra: `StandardTestDispatcher`, track created VMs and cancel `viewModelScope` in
`@AfterTest` before `resetMain()` or leaked coroutines fail the next test class.
