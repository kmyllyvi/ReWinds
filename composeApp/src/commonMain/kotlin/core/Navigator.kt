package core

interface Navigator {
    fun navigateToHome()
    fun navigateToPlaceSummary(placeName: String)
    fun navigateToMonthlyStatistics(placeName: String, year: Int, month: Int)
    fun navigateToChat(initialMessage: String? = null, placeId: String? = null)
    fun navigateToSettings()
    fun navigateBack()
    fun canNavigateBack(): Boolean
}
