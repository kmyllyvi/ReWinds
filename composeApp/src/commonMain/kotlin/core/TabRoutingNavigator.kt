package core

/**
 * A [Navigator] decorator that resolves cross-tab navigation for a single tab's stack.
 *
 * Each tab owns an independent back stack (see [Navigation]). Some navigation intents are
 * not "push onto my stack" but "go to another root tab" — e.g. the Chat button on a Place
 * detail screen, or pressing back from a tab root. Encoding those rules inline in the
 * Composable made them untestable; this class lifts them out so the routing is plain Kotlin
 * (MV* — navigation decisions live in logic, not in `@Composable`s).
 *
 * @param base the underlying per-tab navigator (pushes/pops this tab's own stack)
 * @param selectTab switches the active root tab
 * @param onChatRequested how a "go to chat" intent is satisfied for this tab. Receives the
 *   optional [placeId] so the Place → Chat entry point can carry place context to the Chat
 *   tab. Defaults to switching to the Chat tab; the Chat tab itself passes its own push
 *   behaviour.
 * @param onRootBack invoked when [navigateBack] is called while this tab's stack is at its
 *   root (nothing left to pop). Returns true if it handled the back (e.g. switched tabs),
 *   false to leave the no-op behaviour. Defaults to no-op.
 */
class TabRoutingNavigator(
    private val base: Navigator,
    private val selectTab: (AppTab) -> Unit,
    private val onChatRequested: (initialMessage: String?, placeId: String?) -> Unit =
        { _, _ -> selectTab(AppTab.CHAT) },
    private val onRootBack: () -> Boolean = { false }
) : Navigator by base {

    override fun navigateToChat(initialMessage: String?, placeId: String?) {
        onChatRequested(initialMessage, placeId)
    }

    override fun navigateToSettings() {
        selectTab(AppTab.SETTINGS)
    }

    override fun navigateBack() {
        if (base.canNavigateBack()) {
            base.navigateBack()
        } else {
            onRootBack()
        }
    }
}
