package core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

/**
 * Worked example of the seed-route + assert-navigation pattern from [FakeNavigator] (KIM-292).
 *
 * These exercise the navigation *contract* a screen depends on, runnable as a plain JVM unit
 * test (no Compose runtime). KIM-293's Compose semantic tests reuse the same [FakeNavigator] and
 * the same `assertEquals(expectedCall, navigator.lastCall)` assertion, just driven by a real tap.
 *
 * The Place → Chat case mirrors `PlaceSummaryView`, which on the "Ask AI about this place" button
 * calls `navigator.navigateToChat(placeId = currentPlaceName)` (KIM-291 journey J3).
 */
class FakeNavigatorExampleTest {

    @Test
    fun askAiAboutPlaceRecordsChatNavigationWithPlaceId() {
        val navigator = FakeNavigator()
        val route = PlaceSummaryRoute("Helsinki")

        // The exact call PlaceSummaryView's "Ask AI about this place" button makes.
        navigator.navigateToChat(placeId = route.placeName)

        assertEquals(NavigationCall.Chat(placeId = "Helsinki"), navigator.lastCall)
    }

    @Test
    fun openingMonthlyStatisticsRecordsTheRequestedMonth() {
        val navigator = FakeNavigator()

        navigator.navigateToMonthlyStatistics("Oulu", year = 2026, month = 6)

        assertEquals(NavigationCall.MonthlyStatistics("Oulu", 2026, 6), navigator.lastCall)
    }

    @Test
    fun goToSettingsRecordsSettingsNavigation() {
        // KIM-291 journey J7: "chat blocked → Go to Settings".
        val navigator = FakeNavigator()

        navigator.navigateToSettings()

        assertEquals(NavigationCall.Settings, navigator.lastCall)
    }

    @Test
    fun backIsRecordedAndRootDepthIsSimulatable() {
        val canPop = FakeNavigator(canNavigateBack = true)
        assertTrue(canPop.canNavigateBack())

        // Simulate sitting at a tab root: back can still be *requested*, but the screen knows
        // there is nothing to pop.
        val atRoot = FakeNavigator(canNavigateBack = false)
        atRoot.navigateBack()

        assertEquals(NavigationCall.Back, atRoot.lastCall)
        assertTrue(!atRoot.canNavigateBack())
    }

    @Test
    fun callsAreRecordedInOrder() {
        val navigator = FakeNavigator()

        navigator.navigateToPlaceSummary("Tampere")
        navigator.navigateToChat(initialMessage = "How windy this week?")

        assertEquals(
            listOf(
                NavigationCall.PlaceSummary("Tampere"),
                NavigationCall.Chat(initialMessage = "How windy this week?")
            ),
            navigator.calls
        )
        assertNull((navigator.calls.last() as NavigationCall.Chat).placeId)
    }
}
