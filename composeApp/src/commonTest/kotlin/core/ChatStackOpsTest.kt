package core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Place → Chat one-shot context (follow-up to 3e49a95). Verifies the Chat back stack stays
 * at a constant size and that the placeId is consumed exactly once, so revisiting the Chat
 * tab does not re-trigger place resolution or override a manual session switch.
 */
class ChatStackOpsTest {

    @Test
    fun setPlaceContextReplacesTopChatRouteRatherThanPushing() {
        val stack = mutableListOf<NavRoute>(ChatRoute())

        ChatStackOps.setPlaceContext(stack, placeId = "Helsinki")

        assertEquals(1, stack.size)
        assertEquals("Helsinki", (stack.single() as ChatRoute).placeId)
    }

    @Test
    fun repeatedPlaceEntriesDoNotGrowTheStack() {
        // "Ask AI about Helsinki", then "Ask AI about Oulu" — no stale routes pile up.
        val stack = mutableListOf<NavRoute>(ChatRoute())

        ChatStackOps.setPlaceContext(stack, placeId = "Helsinki")
        ChatStackOps.setPlaceContext(stack, placeId = "Oulu")

        assertEquals(1, stack.size)
        assertEquals("Oulu", (stack.single() as ChatRoute).placeId)
    }

    @Test
    fun consumePlaceContextClearsPlaceIdViaReplacement() {
        val original = ChatRoute(placeId = "Helsinki")
        val stack = mutableListOf<NavRoute>(original)

        ChatStackOps.consumePlaceContext(stack)

        assertEquals(1, stack.size)
        assertNull((stack.single() as ChatRoute).placeId)
        // Data class is immutable and shared — must be a new element, not a mutation.
        assertTrue(stack.single() !== original)
    }

    @Test
    fun revisitAfterConsumptionDoesNotReintroducePlaceId() {
        // Simulates: enter place chat → ChatView consumes → revisit Chat tab (no new entry).
        val stack = mutableListOf<NavRoute>(ChatRoute())
        ChatStackOps.setPlaceContext(stack, placeId = "Helsinki")

        ChatStackOps.consumePlaceContext(stack)
        // Second consume on revisit is a no-op; placeId stays null → no re-resolution.
        ChatStackOps.consumePlaceContext(stack)

        assertNull((stack.single() as ChatRoute).placeId)
    }

    @Test
    fun consumePlaceContextIsNoOpWhenTopHasNoPlaceId() {
        val plain = ChatRoute()
        val stack = mutableListOf<NavRoute>(plain)

        ChatStackOps.consumePlaceContext(stack)

        // Untouched (same instance) so the LaunchedEffect won't re-fire on recomposition.
        assertTrue(stack.single() === plain)
    }

    @Test
    fun setPlaceContextTargetsTheChatRouteEvenWithRouteUnderneath() {
        // The chat stack's root is always a ChatRoute, but guard against a deeper layout.
        val stack = mutableListOf<NavRoute>(HomeRoute, ChatRoute())

        ChatStackOps.setPlaceContext(stack, placeId = "Tampere")

        assertEquals(2, stack.size)
        assertEquals("Tampere", (stack.last() as ChatRoute).placeId)
        assertEquals(HomeRoute, stack.first())
    }
}
