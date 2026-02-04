package core

interface Navigator {
    fun navigateToHome()
    fun navigateToPlaceSummary(placeName: String)
    fun navigateToMonthlyStatistics(placeName: String, year: Int, month: Int)
    fun navigateBack()
    fun canNavigateBack(): Boolean
}
