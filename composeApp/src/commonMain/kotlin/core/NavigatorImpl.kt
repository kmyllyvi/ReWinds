package core

import androidx.compose.runtime.snapshots.SnapshotStateList

class NavigatorImpl(
    private val backStack: SnapshotStateList<NavRoute>
) : Navigator {

    override fun navigateToHome() {
        backStack.clear()
        backStack.add(HomeRoute)
    }

    override fun navigateToPlaceSummary(placeName: String) {
        backStack.add(PlaceSummaryRoute(placeName))
    }

    override fun navigateToMonthlyStatistics(placeName: String, year: Int, month: Int) {
        backStack.add(MonthlyStatisticsRoute(placeName, year, month))
    }

    override fun navigateToChat(initialMessage: String?, placeId: String?) {
        backStack.add(ChatRoute(initialMessage, placeId))
    }

    override fun navigateToSettings() {
        backStack.add(SettingsRoute)
    }

    override fun navigateToWelcome() {
        backStack.add(WelcomeRoute)
    }

    override fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    override fun canNavigateBack(): Boolean = backStack.size > 1
}
