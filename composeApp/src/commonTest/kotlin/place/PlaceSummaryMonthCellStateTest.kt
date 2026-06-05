package place

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the month-grid cell-state mapping logic.
 *
 * calculateMonthCellStates() lives in the ViewModel companion as a pure function over
 * the stored-day list, so it is tested directly without constructing a ViewModel (no DB
 * or coroutine state involved).
 *
 * NOTE: the FULL/PARTIAL boundary uses PARTIAL_DAY_THRESHOLD (placeholder = 20),
 * flagged for Kimmo to confirm as a product decision.
 */
class PlaceSummaryMonthCellStateTest {

    private fun mapStates(year: Int?, days: List<DayWeatherSummary>) =
        PlaceSummaryViewModel.calculateMonthCellStates(year, days)

    private fun day(date: String) =
        DayWeatherSummary(date, null, null, null, null, null, null, null, null, false, 0)

    @Test
    fun noStoredDays_allMonthsAreNoData() {
        val cells = mapStates(2025, emptyList())

        assertEquals(12, cells.size)
        cells.forEach { assertEquals(MonthCellState.NO_DATA, it.state, "month ${it.month}") }
    }

    @Test
    fun fullMonth_isFull() {
        // January 2025 has 31 days; provide all of them.
        val days = (1..31).map { day("2025-01-${it.toString().padStart(2, '0')}") }

        val january = mapStates(2025, days).first { it.month == 1 }

        assertEquals(MonthCellState.FULL, january.state)
        assertEquals(31, january.presentDaysCount)
        assertEquals(31, january.totalDaysInMonth)
    }

    @Test
    fun atOrAboveThreshold_butIncomplete_isFull() {
        // 20 days (== PARTIAL_DAY_THRESHOLD) out of 31 → treated as FULL.
        val days = (1..PlaceSummaryViewModel.PARTIAL_DAY_THRESHOLD)
            .map { day("2025-01-${it.toString().padStart(2, '0')}") }

        val january = mapStates(2025, days).first { it.month == 1 }

        assertEquals(MonthCellState.FULL, january.state)
        assertEquals(20, january.presentDaysCount)
    }

    @Test
    fun belowThreshold_isPartial() {
        // 5 days out of 31 → below threshold → PARTIAL.
        val days = (1..5).map { day("2025-01-0$it") }

        val january = mapStates(2025, days).first { it.month == 1 }

        assertEquals(MonthCellState.PARTIAL, january.state)
        assertEquals(5, january.presentDaysCount)
    }

    @Test
    fun daysFromOtherYears_areIgnored() {
        val days = listOf(
            day("2024-01-15"),
            day("2024-01-16"),
            day("2025-01-15")
        )

        val january = mapStates(2025, days).first { it.month == 1 }

        assertEquals(1, january.presentDaysCount)
        assertEquals(MonthCellState.PARTIAL, january.state)
    }

    @Test
    fun nullSelectedYear_allMonthsNoData() {
        val days = (1..31).map { day("2025-01-${it.toString().padStart(2, '0')}") }

        val cells = mapStates(null, days)

        cells.forEach { assertEquals(MonthCellState.NO_DATA, it.state, "month ${it.month}") }
    }
}
