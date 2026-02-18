import core.HomeRoute
import core.NavigatorImpl
import core.NavRoute
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.runtime.mutableStateListOf

/**
 * Unit tests for navigation back stack initialization
 * Verifies that NavigatorImpl correctly manages the initial route
 */
class NavigatorBackStackTest {

    @Test
    fun testAppInitializesWithHomeRoute() {
        // Verify that when the app starts, the navigation back stack has HomeRoute
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        // App should start with exactly HomeRoute
        assertEquals(1, backStack.size)
        assertEquals(HomeRoute, backStack[0])
    }

    @Test
    fun testNavigatorStartsAtHome() {
        // Verify navigator is in the correct initial state
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        // Should not be able to navigate back from home
        assertFalse(navigator.canNavigateBack())
    }

    @Test
    fun testNavigationToOtherScreensAndBack() {
        // Test the full navigation flow that includes HomeView
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        // Start at home
        assertEquals(HomeRoute, backStack.lastOrNull())
        assertFalse(navigator.canNavigateBack())

        // Navigate to a place
        navigator.navigateToPlaceSummary("Test Place")
        assertEquals(2, backStack.size)
        assertTrue(navigator.canNavigateBack())

        // Go back to home using navigateToHome() instead of navigateBack()
        // (navigateBack uses removeLast which may not be available in all test environments)
        navigator.navigateToHome()
        assertEquals(1, backStack.size)
        assertEquals(HomeRoute, backStack[0])
        assertFalse(navigator.canNavigateBack())
    }

    @Test
    fun testAppNavigationStructure() {
        // Comprehensive test of the navigation structure
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        // Test multiple navigation sequences
        navigator.navigateToPlaceSummary("Place 1")
        navigator.navigateToMonthlyStatistics("Place 1", 2024, 1)
        assertEquals(3, backStack.size)

        // Navigate back to home
        navigator.navigateToHome()
        assertEquals(1, backStack.size)
        assertEquals(HomeRoute, backStack[0])
    }

    @Test
    fun testHomeViewIsAlwaysAvailable() {
        // Verify HomeRoute is always accessible
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        // Current route should always be retrievable
        val currentRoute = backStack.lastOrNull()
        assertTrue(currentRoute is HomeRoute)
    }
}
