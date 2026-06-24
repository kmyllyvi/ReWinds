# Navigation Module

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. -->

**Last updated:** 2026-06-19 (PR #45 — KIM-309)
**Status:** Active

---

## Purpose

Manages all in-app routing using a custom per-tab back-stack model built on top of Compose's `mutableStateListOf`. Provides a tab bar with three root tabs (Places, Chat, Settings) and push-destination screens that hide the tab bar.

---

## Responsibilities

- Defining all navigation routes as serializable sealed-interface variants (`NavRoute`).
- Managing per-tab back stacks (`placesStack`, `chatStack`, `settingsStack`), each initialised with their root route.
- Implementing `Navigator` via `NavigatorImpl` wrapping a `SnapshotStateList<NavRoute>`.
- Delegating cross-tab navigations (chat from Places tab, settings from Places tab) to `TabNavigationViewModel.selectTab()` rather than pushing onto the Places stack.
- Hiding the tab bar when the Places stack is on a push destination (`PlaceSummaryRoute`, `MonthlyStatisticsRoute`).
- Tab bar appearance driven by the Midnight Blue design system constants (`TabBarBg`, `TabBarActive`, etc.).

---

## Dependencies

### Internal
- `core.TabNavigationViewModel` — holds `activeTab: StateFlow<AppTab>`, mutated by `selectTab()`.
- `onboarding.VcKeyOnboardingViewModel` — gate state; `Navigation()` collects `isWeatherKeyConfigured` and short-circuits before `AppTabs` while false.
- All screen composables: `HomeView`, `ChatView`, `SettingsView`, `PlaceSummaryView`, `MonthlyStatisticsView`.
- `onboarding.VcKeyOnboardingScreen` — full-screen blocking gate composable shown before the tab surface.
- Koin `koinViewModel()` for both `TabNavigationViewModel` and `VcKeyOnboardingViewModel`.

---

## Key interfaces

### NavRoute (sealed interface)
```kotlin
object HomeRoute : NavRoute
data class PlaceSummaryRoute(val placeName: String) : NavRoute
data class MonthlyStatisticsRoute(val placeName: String, val year: Int, val month: Int) : NavRoute
data class ChatRoute(val initialMessage: String? = null) : NavRoute
object SettingsRoute : NavRoute
```

### Navigator (interface)
```kotlin
fun navigateToHome()
fun navigateToPlaceSummary(placeName: String)
fun navigateToMonthlyStatistics(placeName: String, year: Int, month: Int)
fun navigateToChat(initialMessage: String? = null)
fun navigateToSettings()
fun navigateBack()
fun canNavigateBack(): Boolean
```

### AppTab (enum)
`PLACES` | `CHAT` | `SETTINGS`

### Navigation() — root composable entry point
Entry point that first evaluates the hard onboarding gate (KIM-309). While `VcKeyOnboardingViewModel.isWeatherKeyConfigured` is false, `OnboardingGate` is rendered and the function returns early — `AppTabs` is never composed. Once the key is configured, `AppTabs` instantiates all three stacks and `NavigatorImpl` instances and renders the `Scaffold` with `TabBar` or full-screen `PushDestination` depending on top of the Places stack.

---

## Window insets handling (KIM-298)

The `Scaffold` content `Box` in `Navigation()` applies `.consumeWindowInsets(innerPadding)` after `.padding(innerPadding)`. This ensures descendant `imePadding()` modifiers (e.g. in `ChatView`) correctly subtract the already-consumed bottom-tab-bar inset rather than double-adding it. Without this, opening the keyboard in the Chat tab caused a visible gap equal to the tab bar height between the text input and the keyboard.

## Known constraints

- The `initialMessage` parameter on `ChatRoute` and `navigateToChat()` is defined but deep-link delivery to the Chat tab is deferred (referenced as KIM-267 spec, not yet implemented as of bootstrap).
- Back navigation is stack-pop only; there is no deep-link or programmatic cross-tab back.
- Each tab's root route is fixed at initialisation; navigating to a tab always returns to that tab's current stack top, not its root.
- `NavigatorImpl` is not created through Koin; it is instantiated directly in the `Navigation()` composable using `remember`.

---

## Decisions log

- See `/docs/agent/decisions/` for records created after the bootstrap phase.
