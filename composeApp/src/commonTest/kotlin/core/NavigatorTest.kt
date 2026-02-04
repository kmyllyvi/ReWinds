package core

import androidx.compose.runtime.mutableStateListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class NavigatorTest {

    @Test
    fun `initial state has home route`() {
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        assertEquals(1, backStack.size)
        assertEquals(HomeRoute, backStack[0])
        assertFalse(navigator.canNavigateBack())
    }

    @Test
    fun `navigateToPlaceSummary adds route to back stack`() {
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        navigator.navigateToPlaceSummary("Test Place")

        assertEquals(2, backStack.size)
        assertEquals(HomeRoute, backStack[0])
        assertTrue(backStack[1] is PlaceSummaryRoute)
        assertEquals("Test Place", (backStack[1] as PlaceSummaryRoute).placeName)
        assertTrue(navigator.canNavigateBack())
    }

    @Test
    fun `navigateToMonthlyStatistics preserves all parameters`() {
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        navigator.navigateToMonthlyStatistics("Test", 2024, 5)

        assertEquals(2, backStack.size)
        val route = backStack[1] as MonthlyStatisticsRoute
        assertEquals("Test", route.placeName)
        assertEquals(2024, route.year)
        assertEquals(5, route.month)
    }

    @Test
    fun `navigateBack removes last route`() {
        val backStack = mutableStateListOf<NavRoute>(
            HomeRoute,
            PlaceSummaryRoute("Test")
        )
        val navigator = NavigatorImpl(backStack)

        navigator.navigateBack()

        assertEquals(1, backStack.size)
        assertEquals(HomeRoute, backStack[0])
        assertFalse(navigator.canNavigateBack())
    }

    @Test
    fun `navigateBack does nothing when only home remains`() {
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        navigator.navigateBack()

        assertEquals(1, backStack.size)
        assertEquals(HomeRoute, backStack[0])
    }

    @Test
    fun `navigateToHome clears back stack`() {
        val backStack = mutableStateListOf<NavRoute>(
            HomeRoute,
            PlaceSummaryRoute("Test1"),
            MonthlyStatisticsRoute("Test2", 2024, 5)
        )
        val navigator = NavigatorImpl(backStack)

        navigator.navigateToHome()

        assertEquals(1, backStack.size)
        assertEquals(HomeRoute, backStack[0])
        assertFalse(navigator.canNavigateBack())
    }
}
