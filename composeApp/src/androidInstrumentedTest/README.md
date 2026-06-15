# Compose semantic UI tests (Layer 1)

Fast, deterministic Compose UI tests for the P0 critical journeys (KIM-289 epic, KIM-293 ticket).
They drive the **real** screen composables through the AndroidX Compose test runner, with all I/O
replaced by in-memory fakes — no network, no database, no platform key store.

These run on an emulator/device:

```bash
./gradlew :composeApp:connectedDebugAndroidTest
# compile-only check (no device needed):
./gradlew :composeApp:compileDebugAndroidTestKotlin
```

> CI wiring ("run on every PR") is intentionally **out of scope** for KIM-293 — the current
> `.github/workflows/ci.yml` has no emulator job. Adding one is a separate follow-up.

## Journey coverage

| ID  | Journey                                   | Test                                            |
|-----|-------------------------------------------|-------------------------------------------------|
| J2  | Add a place via search                    | `uitest/HomeJourneyTest.kt`                      |
| J3  | Open a saved place's summary              | `uitest/HomeJourneyTest.kt`                      |
| J6  | Chat send / receive                       | `uitest/ChatJourneyTest.kt`                      |
| J7  | Chat blocked without Anthropic key        | `uitest/ChatJourneyTest.kt`                      |
| J8  | Switch chat sessions                      | `uitest/ChatJourneyTest.kt`                      |
| J10 | Configure Anthropic key in Settings       | `uitest/SettingsJourneyTest.kt`                  |
| —   | Header-above-messages layout (reference)  | `ai/ChatLayoutTest.kt`                           |

J1, J4, J5, J9, J11 are covered at the E2E layer by the Maestro suite (KIM-294) and can get
Compose coverage in a follow-up if needed.

## The pattern

Every test follows the same three steps.

### 1. Build the screen's ViewModel from fakes

Each screen composable takes its `ViewModel` and `Navigator` as parameters, so a test constructs
the **production** ViewModel wired to in-memory fakes and passes it directly — no Koin singleton,
which keeps tests order-independent:

```kotlin
val vm = ChatViewModel(
    aiRepository = FakeAiConversationRepository(cannedReply = "..."),
    weatherRepository = FakeWeatherRepository(),
    chatRepository = FakeChatRepository().apply { seed(...) },
    apiKeyChecker = ApiKeyChecker { keyConfigured }   // force the key branch (J7)
)
composeTestRule.setContent {
    ReWindsTheme { ChatView(vm = vm, navigator = FakeNavigator()) }
}
```

The shared fakes live in `uitest/Fakes.kt`:

- `FakeWeatherRepository` — scriptable search results + observable saved-place list (J2/J3).
- `FakeAiConversationRepository` — returns a canned reply, never calls Anthropic (J6).
- `FakeChatRepository` — in-memory sessions you can `seed(...)` (J8).
- `FakeDatabase` — no-op `Database`.

Production code exposes the seams these fakes plug into:

- `AiConversationRepository` — the interface `ChatViewModel` depends on (`AiRepository` implements it).
- `ApiKeyChecker` — injectable Anthropic-key gate (default `PlatformApiKeyChecker`), so the
  "key missing/present" branches are deterministic instead of depending on the developer's local
  `gradle.properties` / Keychain.

> Equivalent Koin-module injection is possible (`startKoin { module { single { fake } } }` +
> `koinViewModel()`), but direct construction is preferred here: it avoids a global singleton and
> guarantees isolation between tests.

### 2. Drive the UI via `TestTags`

Never select by display text for controls — use the shared `core.TestTags` constants (KIM-290).
The same constants back the Maestro `id:` selectors (KIM-294), so the two layers never drift.

```kotlin
composeTestRule.onNodeWithTag(TestTags.CHAT_INPUT_FIELD).performTextInput("...")
composeTestRule.onNodeWithTag(TestTags.CHAT_SEND_BUTTON).performClick()
```

Async UI state (debounced search, coroutine responses) is awaited with `waitUntil { ... }` polling
`fetchSemanticsNodes()`, never a fixed sleep. Text assertions are fine for **content** (message
bubbles, status chips) where no tag exists; pull the expected strings from `AppStrings.English`.

### 3. Assert navigation via `FakeNavigator`

`FakeNavigator` (KIM-292, `src/commonTestFixtures/kotlin/core/FakeNavigator.kt`) records every
outgoing navigation as a `NavigationCall`. It is shared between these Compose tests and the JVM unit
tests via a `commonTestFixtures` source dir added to both source sets in `build.gradle.kts`.

```kotlin
val navigator = FakeNavigator()
// ... tap a control that should navigate ...
composeTestRule.runOnIdle {
    assertEquals(NavigationCall.Settings, navigator.lastCall)
}
```

## Rules

- One fake set, in `uitest/Fakes.kt` — extend it, don't re-declare fakes per test file.
- No real network or DB. If a screen needs a new seam to be testable, extract a minimal interface
  in production code (as done for `AiConversationRepository` / `ApiKeyChecker`) rather than reaching
  into platform globals from the test.
- Tests must pass in isolation **and** as part of the full suite — keep them free of shared mutable
  global state (the one exception, `ApiKeyManager` in J10, is reset in `@Before`/`@After`).
