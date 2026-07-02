package core

interface Navigator {
    fun navigateToHome()
    fun navigateToPlaceSummary(placeName: String)
    fun navigateToMonthlyStatistics(placeName: String, year: Int, month: Int)
    fun navigateToChat(initialMessage: String? = null, placeId: String? = null)
    fun navigateToSettings()
    /** Push the revisitable welcome / "what is ReWinds" guide (KIM-334). */
    fun navigateToWelcome() {}
    fun navigateBack()
    fun canNavigateBack(): Boolean
}
