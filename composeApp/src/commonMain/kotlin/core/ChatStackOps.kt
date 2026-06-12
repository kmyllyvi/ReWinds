package core

/**
 * Pure manipulations of the Chat tab's back stack for the "Ask AI about this place" flow.
 *
 * The Chat tab reads the placeId off the top [ChatRoute] to resolve a place session. That
 * placeId is one-shot context: once [ChatView] has consumed it we must drop it, otherwise
 * every revisit to the Chat tab re-triggers the place resolution and overrides the user's
 * manual session switches.
 *
 * Implemented as list-element replacement (not in-place mutation) because [ChatRoute] is an
 * immutable data class shared across the app.
 */
object ChatStackOps {

    /**
     * Sets a place context on the Chat tab by replacing the top [ChatRoute] with one tagged
     * [placeId]. Replacing rather than pushing keeps the stack at a constant size across
     * repeated "Ask AI about place A / place B" entries (no stale routes pile up).
     */
    fun setPlaceContext(stack: MutableList<NavRoute>, placeId: String, initialMessage: String? = null) {
        val target = ChatRoute(initialMessage = initialMessage, placeId = placeId)
        val topIndex = stack.indexOfLast { it is ChatRoute }
        if (topIndex >= 0) {
            stack[topIndex] = target
        } else {
            stack.add(target)
        }
    }

    /**
     * Clears the one-shot place context once [ChatView] has resolved it: replaces the top
     * placeId-tagged [ChatRoute] with a plain [ChatRoute]. No-op when the top chat route
     * already has no placeId, so subsequent revisits don't re-trigger resolution.
     */
    fun consumePlaceContext(stack: MutableList<NavRoute>) {
        val topIndex = stack.indexOfLast { it is ChatRoute }
        if (topIndex < 0) return
        val top = stack[topIndex] as ChatRoute
        if (top.placeId == null) return
        stack[topIndex] = top.copy(placeId = null)
    }
}
