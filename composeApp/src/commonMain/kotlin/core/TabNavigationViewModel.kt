package core

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The three root tabs in left-to-right order. */
enum class AppTab {
    PLACES, CHAT, SETTINGS
}

/**
 * Holds which tab is currently selected and manages per-tab back stacks.
 *
 * Tabs each maintain their own independent back-stack so that switching tabs
 * preserves the navigation state of the tab you leave (standard bottom-nav behaviour).
 * Push destinations (PlaceSummary, MonthlyStatistics) live on the Places tab stack.
 *
 * Navigation state lives here rather than in the composable to satisfy the MV* rule.
 */
class TabNavigationViewModel : ViewModel() {

    private val _activeTab = MutableStateFlow(AppTab.PLACES)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }
}
