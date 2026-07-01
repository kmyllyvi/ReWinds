package place

import core.Day
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for MonthlyStatisticsViewModel mapping logic.
 *
 * The toDayWeatherSummary() function is private inside the ViewModel, so the
 * mapping logic is mirrored here as a local helper — the same pattern used in
 * PlaceSummaryViewModelTest — allowing field-level assertions without spinning
 * up the full ViewModel (which requires a live DB-backed AppSettingsRepository).
 */
class MonthlyStatisticsViewModelTest {

    // ---------------------------------------------------------------------------
    // Helper — mirrors MonthlyStatisticsViewModel.toDayWeatherSummary()
    // (no sustained-wind calculation, which requires filter state)
    // ---------------------------------------------------------------------------

    private fun Day.toDayWeatherSummary(): DayWeatherSummary {
        val foggyHours = this.hours?.count { (it.visibility ?: 24.0) < 1.0 } ?: 0
        return DayWeatherSummary(
            date = this.datetime,
            description = this.description ?: this.conditions,
            maxTemp = this.tempmax,
            minTemp = this.tempmin,
            avgTemp = this.temp,
            avgWindSpeed = this.windspeed,
            maxWindSpeed = this.windgust,
            sustainedWindSpeed = null, // omitted – requires filter state
            solarenergy = this.solarenergy,
            isFoggy = foggyHours > 0,
            foggyHours = foggyHours,
            precipitation = this.precip,
            windDirection = this.winddir,
            sunrise = this.sunrise,
            sunset = this.sunset
        )
    }

    // ---------------------------------------------------------------------------
    // Sunrise / sunset mapping
    // ---------------------------------------------------------------------------

    @Test
    fun toDayWeatherSummary_sunriseAndSunset_areMappedCorrectly() {
        val day = buildDay(sunrise = "07:15:00", sunset = "19:45:00")

        val summary = day.toDayWeatherSummary()

        assertEquals("07:15:00", summary.sunrise)
        assertEquals("19:45:00", summary.sunset)
    }

    @Test
    fun toDayWeatherSummary_nullSunriseAndSunset_remainNull() {
        val day = buildDay(sunrise = null, sunset = null)

        val summary = day.toDayWeatherSummary()

        assertNull(summary.sunrise)
        assertNull(summary.sunset)
    }

    // ---------------------------------------------------------------------------
    // minTemp / maxTemp mapping
    // ---------------------------------------------------------------------------

    @Test
    fun toDayWeatherSummary_minAndMaxTemp_areMappedCorrectly() {
        val day = buildDay(tempmin = 8.5, tempmax = 24.3)

        val summary = day.toDayWeatherSummary()

        assertEquals(8.5, summary.minTemp)
        assertEquals(24.3, summary.maxTemp)
    }

    @Test
    fun toDayWeatherSummary_nullMinAndMaxTemp_remainNull() {
        val day = buildDay(tempmin = null, tempmax = null)

        val summary = day.toDayWeatherSummary()

        assertNull(summary.minTemp)
        assertNull(summary.maxTemp)
    }

    // ---------------------------------------------------------------------------
    // Wind direction mapping
    // ---------------------------------------------------------------------------

    @Test
    fun toDayWeatherSummary_windDirection_isMappedCorrectly() {
        val day = buildDay(winddir = 230.0)

        val summary = day.toDayWeatherSummary()

        assertEquals(230.0, summary.windDirection)
    }

    @Test
    fun toDayWeatherSummary_nullWindDirection_remainsNull() {
        val day = buildDay(winddir = null)

        val summary = day.toDayWeatherSummary()

        assertNull(summary.windDirection)
    }

    // ---------------------------------------------------------------------------
    // Precipitation (rainfall) mapping
    // ---------------------------------------------------------------------------

    @Test
    fun toDayWeatherSummary_rainfall_isMappedCorrectly() {
        val day = buildDay(precip = 12.5)

        val summary = day.toDayWeatherSummary()

        assertEquals(12.5, summary.precipitation)
    }

    @Test
    fun toDayWeatherSummary_zeroRainfall_isMappedAsZeroNotNull() {
        // A day with 0 mm rainfall should map to 0.0, not null — the ViewModel
        // uses mapNotNull { it.precipitation } for aggregation, so 0.0 is
        // correctly excluded from totals, but the raw field must be 0.0.
        val day = buildDay(precip = 0.0)

        val summary = day.toDayWeatherSummary()

        assertEquals(0.0, summary.precipitation)
    }

    @Test
    fun toDayWeatherSummary_nullRainfall_remainsNull() {
        val day = buildDay(precip = null)

        val summary = day.toDayWeatherSummary()

        assertNull(summary.precipitation)
    }

    // ---------------------------------------------------------------------------
    // Combined — all new/extended fields mapped in one pass
    // ---------------------------------------------------------------------------

    @Test
    fun toDayWeatherSummary_allNewFields_areMappedTogetherCorrectly() {
        val day = buildDay(
            tempmin = 10.0,
            tempmax = 22.0,
            winddir = 180.0,
            precip = 5.5,
            sunrise = "06:00:00",
            sunset = "20:00:00"
        )

        val summary = day.toDayWeatherSummary()

        assertEquals(10.0, summary.minTemp)
        assertEquals(22.0, summary.maxTemp)
        assertEquals(180.0, summary.windDirection)
        assertEquals(5.5, summary.precipitation)
        assertEquals("06:00:00", summary.sunrise)
        assertEquals("20:00:00", summary.sunset)
    }

    // ---------------------------------------------------------------------------
    // Qualifying-day highlighting (KIM-328)
    //
    // The ViewModel ctor needs a live DB-backed AppSettingsRepository, so — as with
    // the mapping tests above — the small selection algorithm is mirrored here and
    // asserted directly. Mirror must stay in sync with
    // MonthlyStatisticsViewModel.qualifyingDayIndices.
    // ---------------------------------------------------------------------------

    private fun summaryWithMatch(date: String, isMatch: Boolean): DayWeatherSummary =
        DayWeatherSummary(
            date = date,
            description = null,
            maxTemp = null,
            minTemp = null,
            avgTemp = null,
            avgWindSpeed = null,
            maxWindSpeed = null,
            sustainedWindSpeed = null,
            solarenergy = null,
            isFoggy = false,
            foggyHours = 0,
            isMatch = isMatch
        )

    // Mirrors MonthlyStatisticsViewModel.qualifyingDayIndices.
    private fun qualifyingDayIndices(days: List<DayWeatherSummary>): Set<Int> =
        days.withIndex()
            .filter { (_, day) -> day.isMatch }
            .map { (index, _) -> index }
            .toSet()

    @Test
    fun qualifyingDayIndices_emptyList_returnsEmptySet() {
        assertEquals(emptySet(), qualifyingDayIndices(emptyList()))
    }

    @Test
    fun qualifyingDayIndices_noMatches_returnsEmptySet() {
        val days = listOf(
            summaryWithMatch("2025-01-01", isMatch = false),
            summaryWithMatch("2025-01-02", isMatch = false)
        )
        assertEquals(emptySet(), qualifyingDayIndices(days))
    }

    @Test
    fun qualifyingDayIndices_returnsAllMatchingDays_notJustOne() {
        val days = listOf(
            summaryWithMatch("2025-01-01", isMatch = true),
            summaryWithMatch("2025-01-02", isMatch = false),
            summaryWithMatch("2025-01-03", isMatch = true),
            summaryWithMatch("2025-01-04", isMatch = true)
        )
        assertEquals(setOf(0, 2, 3), qualifyingDayIndices(days))
    }

    @Test
    fun qualifyingDayIndices_allMatch_returnsEveryIndex() {
        val days = listOf(
            summaryWithMatch("2025-01-01", isMatch = true),
            summaryWithMatch("2025-01-02", isMatch = true)
        )
        assertEquals(setOf(0, 1), qualifyingDayIndices(days))
    }

    // ---------------------------------------------------------------------------
    // Builder helpers
    // ---------------------------------------------------------------------------

    private fun buildDay(
        datetime: String = "2025-01-15",
        tempmin: Double? = null,
        tempmax: Double? = null,
        temp: Double? = null,
        windspeed: Double? = null,
        windgust: Double? = null,
        winddir: Double? = null,
        precip: Double? = null,
        sunrise: String? = null,
        sunset: String? = null,
        solarenergy: Double? = null,
        conditions: String? = null,
        description: String? = null
    ): Day = Day(
        datetime = datetime,
        datetimeEpoch = null,
        tempmax = tempmax,
        tempmin = tempmin,
        temp = temp,
        feelslikemax = null,
        feelslikemin = null,
        feelslike = null,
        dew = null,
        humidity = null,
        precip = precip,
        precipprob = null,
        precipcover = null,
        preciptype = null,
        snow = null,
        snowdepth = null,
        windgust = windgust,
        windspeed = windspeed,
        winddir = winddir,
        pressure = null,
        cloudcover = null,
        visibility = null,
        solarradiation = null,
        solarenergy = solarenergy,
        uvindex = null,
        sunrise = sunrise,
        sunriseEpoch = null,
        sunset = sunset,
        sunsetEpoch = null,
        moonphase = null,
        conditions = conditions,
        description = description,
        icon = null,
        stations = null,
        source = "test",
        hours = null,
        normal = null
    )
}
