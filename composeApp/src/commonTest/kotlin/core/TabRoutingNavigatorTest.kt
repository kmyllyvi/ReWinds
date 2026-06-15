package core

import androidx.compose.runtime.mutableStateListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * KIM-286 navigation fixes: cross-tab routing for chat entry (Place → Chat) and root-tab
 * back handling. Exercises [TabRoutingNavigator] as plain Kotlin, no Compose runtime.
 */
class TabRoutingNavigatorTest {

    private fun placesNavigator(vararg routes: NavRoute) =
        NavigatorImpl(mutableStateListOf(*routes))

    // --- Bug 3: "Chat about <place>" from a Place screen must reach the Chat tab ---

    @Test
    fun navigateToChatSwitchesToChatTabInsteadOfPushingOnOwnStack() {
        val stack = mutableStateListOf<NavRoute>(HomeRoute, PlaceSummaryRoute("Helsinki"))
        val base = NavigatorImpl(stack)
        var selected: AppTab? = null
        val nav = TabRoutingNavigator(base = base, selectTab = { selected = it })

        nav.navigateToChat(initialMessage = "Chat about Helsinki")

        assertEquals(AppTab.CHAT, selected)
        // Must NOT push a ChatRoute onto the Places stack (the old bug → fell through to Home).
        assertEquals(2, stack.size)
        assertTrue(stack.none { it is ChatRoute })
    }

    @Test
    fun navigateToChatForwardsPlaceIdToChatRequestedHandler() {
        var capturedPlaceId: String? = "unset"
        var capturedMessage: String? = "unset"
        val nav = TabRoutingNavigator(
            base = placesNavigator(HomeRoute, PlaceSummaryRoute("Helsinki")),
            selectTab = {},
            onChatRequested = { message, placeId ->
                capturedMessage = message
                capturedPlaceId = placeId
            }
        )

        nav.navigateToChat(placeId = "Helsinki")

        assertEquals("Helsinki", capturedPlaceId)
        assertNull(capturedMessage)
    }

    @Test
    fun navigateToSettingsSwitchesToSettingsTab() {
        var selected: AppTab? = null
        val nav = TabRoutingNavigator(base = placesNavigator(HomeRoute), selectTab = { selected = it })

        nav.navigateToSettings()

        assertEquals(AppTab.SETTINGS, selected)
    }

    // --- Bug 2: back from the chat root returns to Places instead of being a dead no-op ---

    @Test
    fun navigateBackFromRootInvokesRootBackHandler() {
        var selected: AppTab? = null
        val chatStack = mutableStateListOf<NavRoute>(ChatRoute())
        val nav = TabRoutingNavigator(
            base = NavigatorImpl(chatStack),
            selectTab = { selected = it },
            onRootBack = { selected = AppTab.PLACES; true }
        )

        nav.navigateBack()

        assertEquals(AppTab.PLACES, selected)
        // Stack untouched — back at a root tab doesn't pop the root.
        assertEquals(1, chatStack.size)
    }

    @Test
    fun navigateBackPopsOwnStackWhenNotAtRoot() {
        var rootBackCalled = false
        val stack = mutableStateListOf<NavRoute>(HomeRoute, PlaceSummaryRoute("Oulu"))
        val nav = TabRoutingNavigator(
            base = NavigatorImpl(stack),
            selectTab = {},
            onRootBack = { rootBackCalled = true; true }
        )

        nav.navigateBack()

        // Popped its own stack; root-back handler not consulted.
        assertEquals(1, stack.size)
        assertEquals(HomeRoute, stack.single())
        assertTrue(!rootBackCalled)
    }

    @Test
    fun defaultRootBackIsNoOp() {
        val chatStack = mutableStateListOf<NavRoute>(ChatRoute())
        val nav = TabRoutingNavigator(base = NavigatorImpl(chatStack), selectTab = {})

        // No onRootBack supplied → back at root does nothing and does not crash.
        nav.navigateBack()

        assertEquals(1, chatStack.size)
    }

    @Test
    fun delegatesPassThroughToBaseForPlaceSummary() {
        val stack = mutableStateListOf<NavRoute>(HomeRoute)
        val nav = TabRoutingNavigator(base = NavigatorImpl(stack), selectTab = {})

        nav.navigateToPlaceSummary("Tampere")

        assertEquals(2, stack.size)
        assertEquals(PlaceSummaryRoute("Tampere"), stack.last())
    }
}
