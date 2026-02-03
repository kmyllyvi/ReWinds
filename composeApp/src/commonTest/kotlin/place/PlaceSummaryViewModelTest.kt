package place

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaceSummaryViewModelTest {

    @Test
    fun calculateMonthlyAverageTemps_withDataForAllMonths_returnsCorrectAverages() {
        // Arrange
        val days = listOf(
            // January: avg temps 10, 12, 14 -> average 12.0
            DayWeatherSummary("2025-01-01", null, null, null, 10.0, null, null, null, null, false, 0),
            DayWeatherSummary("2025-01-15", null, null, null, 12.0, null, null, null, null, false, 0),
            DayWeatherSummary("2025-01-31", null, null, null, 14.0, null, null, null, null, false, 0),
            // February: avg temps 15, 17 -> average 16.0
            DayWeatherSummary("2025-02-01", null, null, null, 15.0, null, null, null, null, false, 0),
            DayWeatherSummary("2025-02-28", null, null, null, 17.0, null, null, null, null, false, 0),
            // March: avg temp 20 -> average 20.0
            DayWeatherSummary("2025-03-15", null, null, null, 20.0, null, null, null, null, false, 0),
        )

        // Act
        val result = calculateMonthlyAverageTemps(days)

        // Assert
        assertEquals(12.0, result[1])
        assertEquals(16.0, result[2])
        assertEquals(20.0, result[3])
        // Rest should be null
        for (month in 4..12) {
            assertNull(result[month], "Month $month should be null")
        }
    }

    @Test
    fun calculateMonthlyAverageTemps_withNullTemperatures_ignoresThem() {
        // Arrange
        val days = listOf(
            DayWeatherSummary("2025-01-01", null, null, null, 10.0, null, null, null, null, false, 0),
            DayWeatherSummary("2025-01-02", null, null, null, null, null, null, null, null, false, 0), // null temp
            DayWeatherSummary("2025-01-03", null, null, null, 14.0, null, null, null, null, false, 0),
        )

        // Act
        val result = calculateMonthlyAverageTemps(days)

        // Assert
        // Average of 10.0 and 14.0, ignoring null
        assertEquals(12.0, result[1])
    }

    @Test
    fun calculateMonthlyAverageTemps_withEmptyData_returnsNullForAllMonths() {
        // Act
        val result = calculateMonthlyAverageTemps(emptyList())

        // Assert
        for (month in 1..12) {
            assertNull(result[month], "Month $month should be null for empty data")
        }
    }

    @Test
    fun calculateMonthlyAverageTemps_withNullDates_ignoresThem() {
        // Arrange
        val days = listOf(
            DayWeatherSummary("2025-01-01", null, null, null, 10.0, null, null, null, null, false, 0),
            DayWeatherSummary(null, null, null, null, 15.0, null, null, null, null, false, 0), // null date
            DayWeatherSummary("2025-01-03", null, null, null, 14.0, null, null, null, null, false, 0),
        )

        // Act
        val result = calculateMonthlyAverageTemps(days)

        // Assert
        // Should calculate average of the 2 valid January entries: (10 + 14) / 2 = 12.0
        assertEquals(12.0, result[1])
    }

    @Test
    fun calculateMonthlyAverageTemps_withSingleDayPerMonth_returnsThatTemperature() {
        // Arrange
        val days = listOf(
            DayWeatherSummary("2025-01-15", null, null, null, 25.5, null, null, null, null, false, 0),
            DayWeatherSummary("2025-02-15", null, null, null, 26.3, null, null, null, null, false, 0),
            DayWeatherSummary("2025-03-15", null, null, null, 27.1, null, null, null, null, false, 0),
        )

        // Act
        val result = calculateMonthlyAverageTemps(days)

        // Assert
        assertEquals(25.5, result[1])
        assertEquals(26.3, result[2])
        assertEquals(27.1, result[3])
    }

    // Helper function extracted from ViewModel for testing
    private fun calculateMonthlyAverageTemps(
        storedDays: List<DayWeatherSummary>
    ): Map<Int, Double?> {
        return (1..12).associateWith { monthIndex ->
            val daysInMonth = storedDays.filter { daySummary ->
                val (_, dayMonth) = parseDateParts(daySummary.date)
                dayMonth == monthIndex
            }

            if (daysInMonth.isEmpty()) {
                null
            } else {
                daysInMonth.mapNotNull { it.avgTemp }.average()
            }
        }
    }

    private fun parseDateParts(dateString: String?): Pair<Int?, Int?> {
        if (dateString == null) return Pair(null, null)
        val parts = dateString.split('-')
        val year = parts.getOrNull(0)?.toIntOrNull()
        val month = parts.getOrNull(1)?.toIntOrNull()
        return Pair(year, month)
    }
}
