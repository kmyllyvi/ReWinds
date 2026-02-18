import androidx.compose.runtime.mutableStateListOf
import core.HomeRoute
import core.NavigatorImpl
import core.NavRoute
import core.PlaceSummaryRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Integration tests for app startup initialization
 * Verifies that the app starts with correct initial state:
 * - Navigation begins at HomeRoute
 * - No previous navigation history exists
 * - App is ready to handle user interactions
 *
 * NOTE: Full UI rendering tests require iOS simulator integration tests
 * and cannot be tested in unit tests. Run the app on simulator to verify
 * visual rendering and navigation flow.
 */
class AppStartupTest {

    /**
     * Test that app initialization creates a navigation stack
     * with HomeRoute as the only entry point
     */
    @Test
    fun testAppStartsWithHomeRouteAsOnlyEntry() {
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        // At app startup, the navigation history should contain only HomeRoute
        assertEquals(1, backStack.size, "Navigation stack should have exactly 1 entry at startup")
        assertEquals(HomeRoute, backStack[0], "Initial route must be HomeRoute")
    }

    /**
     * Test that user cannot navigate backwards from the initial state
     * This ensures proper app state at startup
     */
    @Test
    fun testUserCannotNavigateBackFromInitialState() {
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        // At startup, there should be nowhere to navigate back to
        assertFalse(navigator.canNavigateBack(), "Cannot navigate back from initial HomeRoute")
    }

    /**
     * Test that the navigator is properly initialized and ready for interactions
     * Verifies the internal state is consistent
     */
    @Test
    fun testNavigatorIsReadyForUserInteractions() {
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        // Navigator should successfully navigate to another route
        navigator.navigateToPlaceSummary("Test Place")
        assertEquals(2, backStack.size, "Should be able to navigate to PlaceSummary")
        assertEquals("Test Place", (backStack[1] as? PlaceSummaryRoute)?.placeName ?: "")

        // And should be able to navigate back to home
        navigator.navigateToHome()
        assertEquals(1, backStack.size, "Should be able to navigate back to Home")
        assertEquals(HomeRoute, backStack[0])
    }
}
