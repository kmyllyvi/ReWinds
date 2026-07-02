package core

/**
 * In-memory [Navigator] test double for ViewModel and Compose tests (KIM-292).
 *
 * Records every navigation request as a [NavigationCall] in [calls], so a test can trigger a
 * navigation action on the screen/logic under test and then assert the expected call was made —
 * without a real back stack or Koin graph.
 *
 * ### Seed-route + assert-navigation pattern
 *
 * 1. **Seed the starting screen.** Construct the View (or ViewModel) under test and pass a
 *    `FakeNavigator` where a real [Navigator] is expected. The route the screen starts on is
 *    its own constructor/parameter (e.g. `PlaceSummaryRoute("Helsinki")`); the navigator only
 *    handles *outgoing* navigation, so a fresh `FakeNavigator()` is enough.
 *
 *    ```kotlin
 *    val navigator = FakeNavigator()
 *    PlaceSummaryView(route = PlaceSummaryRoute("Helsinki"), onBackClick = {}, navigator = navigator)
 *    ```
 *
 * 2. **Trigger a navigation action.** Tap the control under test (Compose), or invoke the
 *    logic path that calls the navigator.
 *
 * 3. **Assert the recorded call.** Inspect [calls] / [lastCall]:
 *
 *    ```kotlin
 *    assertEquals(NavigationCall.Chat(placeId = "Helsinki"), navigator.lastCall)
 *    ```
 *
 * ### Simulating back-stack depth
 *
 * [canNavigateBack] returns [canNavigateBack], a settable flag, so a test can drive both the
 * "can pop" and the "at root" branches (e.g. [TabRoutingNavigator]'s root-back handling).
 * [navigateBack] always records a [NavigationCall.Back] regardless, so "back was requested" is
 * assertable independently of whether a pop would actually occur.
 */
class FakeNavigator(
    /** Controls [canNavigateBack]. Set to `false` to simulate a screen sitting at a tab root. */
    var canNavigateBack: Boolean = true
) : Navigator {

    /** Every navigation request, in order. Inspect this to assert what the screen requested. */
    val calls = mutableListOf<NavigationCall>()

    /** The most recently recorded call, or `null` if nothing has navigated yet. */
    val lastCall: NavigationCall? get() = calls.lastOrNull()

    override fun navigateToHome() {
        calls.add(NavigationCall.Home)
    }

    override fun navigateToPlaceSummary(placeName: String) {
        calls.add(NavigationCall.PlaceSummary(placeName))
    }

    override fun navigateToMonthlyStatistics(placeName: String, year: Int, month: Int) {
        calls.add(NavigationCall.MonthlyStatistics(placeName, year, month))
    }

    override fun navigateToChat(initialMessage: String?, placeId: String?) {
        calls.add(NavigationCall.Chat(initialMessage, placeId))
    }

    override fun navigateToSettings() {
        calls.add(NavigationCall.Settings)
    }

    override fun navigateToWelcome() {
        calls.add(NavigationCall.Welcome)
    }

    override fun navigateBack() {
        calls.add(NavigationCall.Back)
    }

    override fun canNavigateBack(): Boolean = canNavigateBack
}

/** One recorded navigation request, mirroring the methods on [Navigator]. */
sealed interface NavigationCall {
    data object Home : NavigationCall
    data class PlaceSummary(val placeName: String) : NavigationCall
    data class MonthlyStatistics(val placeName: String, val year: Int, val month: Int) : NavigationCall
    data class Chat(val initialMessage: String? = null, val placeId: String? = null) : NavigationCall
    data object Settings : NavigationCall
    data object Welcome : NavigationCall
    data object Back : NavigationCall
}
