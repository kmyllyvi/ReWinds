package core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the routing decision extracted from the AppTabs composable (Router.kt).
 *
 * [isShowingPlacesPush] decides whether a full-screen push destination (PlaceSummary /
 * MonthlyStatistics) takes over from the tab surface. It used to live inline in the composable —
 * the "conditional rendering logic in the View" anti-pattern (ARCHITECTURE-RULES.md) — and was
 * untested (docs/agent/testing/COVERAGE-GAP-ANALYSIS.md §2.2). Pulling it out makes the decision
 * directly unit-testable.
 */
class RouterTest {

    @Test
    fun placesTab_withPlaceSummaryRoute_showsPush() {
        assertTrue(isShowingPlacesPush(AppTab.PLACES, PlaceSummaryRoute("Helsinki")))
    }

    @Test
    fun placesTab_withMonthlyStatisticsRoute_showsPush() {
        assertTrue(
            isShowingPlacesPush(AppTab.PLACES, MonthlyStatisticsRoute("Helsinki", 2025, 1))
        )
    }

    @Test
    fun placesTab_withHomeRoute_doesNotShowPush() {
        assertFalse(isShowingPlacesPush(AppTab.PLACES, HomeRoute))
    }

    @Test
    fun chatTab_withPlaceSummaryRoute_doesNotShowPush() {
        // The activeTab guard: even with a push route left on the Places stack, the Chat tab
        // keeps its surface (the Place → Chat flow depends on this).
        assertFalse(isShowingPlacesPush(AppTab.CHAT, PlaceSummaryRoute("Helsinki")))
    }

    @Test
    fun chatTab_withMonthlyStatisticsRoute_doesNotShowPush() {
        assertFalse(
            isShowingPlacesPush(AppTab.CHAT, MonthlyStatisticsRoute("Helsinki", 2025, 6))
        )
    }

    @Test
    fun settingsTab_withPlaceSummaryRoute_doesNotShowPush() {
        assertFalse(isShowingPlacesPush(AppTab.SETTINGS, PlaceSummaryRoute("Helsinki")))
    }

    @Test
    fun settingsTab_withHomeRoute_doesNotShowPush() {
        assertFalse(isShowingPlacesPush(AppTab.SETTINGS, HomeRoute))
    }
}
