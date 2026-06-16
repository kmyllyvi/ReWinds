package place

import androidx.lifecycle.viewModelScope
import core.AppSettingsStore
import core.DataAvailabilityStatus
import core.Day
import core.GeoSearchResult
import core.Hour
import core.MonthlyStatisticsRoute
import core.Station
import core.StationsResult
import core.WeatherRepository
import core.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the selected-day sheet state on MonthlyStatisticsViewModel (KIM-303):
 * selectDay(summary) opens the sheet (emits the summary + resolves hours); dismissDaySheet()
 * closes it (clears summary + hours).
 *
 * Drives the real ViewModel against a fake WeatherRepository and an in-memory settings store —
 * no DB. The ctor type for settings is the AppSettingsStore interface, which both the prod
 * binding and this fake satisfy.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MonthlyStatisticsViewModelSelectDayTest {

    private val dispatcher = StandardTestDispatcher()
    private val placeName = "Tarifa"
    private val created = mutableListOf<MonthlyStatisticsViewModel>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        created.clear()
        dispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
    }

    @Test
    fun selectDay_emitsSummaryAndResolvesHours() = runTest {
        val vm = makeViewModel(response = responseWithHours())
        advanceUntilIdle()

        val summary = vm.dailySummaries.value.first()
        vm.selectDay(summary)
        advanceUntilIdle()

        assertEquals(summary, vm.selectedDay.value)
        // Two of the three stored hours fall inside the 09:00–21:00 UTC window.
        assertEquals(listOf("09", "15"), vm.selectedDayHours.value.map { it.label })
        assertTrue(!vm.isLoadingHours.value)
    }

    @Test
    fun dismissDaySheet_clearsSelectionAndHours() = runTest {
        val vm = makeViewModel(response = responseWithHours())
        advanceUntilIdle()

        vm.selectDay(vm.dailySummaries.value.first())
        advanceUntilIdle()

        vm.dismissDaySheet()

        assertNull(vm.selectedDay.value)
        assertTrue(vm.selectedDayHours.value.isEmpty())
    }

    @Test
    fun selectDay_dayWithoutHours_emitsSummaryWithEmptyHours() = runTest {
        val vm = makeViewModel(response = responseWithHours(hours = null))
        advanceUntilIdle()

        val summary = vm.dailySummaries.value.first()
        vm.selectDay(summary)
        advanceUntilIdle()

        assertEquals(summary, vm.selectedDay.value)
        assertTrue(vm.selectedDayHours.value.isEmpty())
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun makeViewModel(response: WeatherResponse): MonthlyStatisticsViewModel =
        MonthlyStatisticsViewModel(
            route = MonthlyStatisticsRoute(placeName, year = 2025, month = 1),
            weatherRepository = FakeRepo(response),
            settingsRepo = FakeSettingsStore()
        ).also { created.add(it) }

    private fun responseWithHours(hours: List<Hour>? = sampleHours()): WeatherResponse =
        WeatherResponse(
            resolvedAddress = placeName,
            tzoffset = 0.0,
            days = listOf(buildDay(datetime = "2025-01-15", hours = hours))
        )

    // Hours at 08:00 (excluded), 09:00 (included), 15:00 (included) UTC.
    private fun sampleHours(): List<Hour> {
        val dayBase = 1700000000L - (1700000000L % 86400L)
        return listOf(
            buildHour(dayBase + 8 * 3600L),
            buildHour(dayBase + 9 * 3600L),
            buildHour(dayBase + 15 * 3600L)
        )
    }

    private class FakeSettingsStore : AppSettingsStore {
        override fun getString(key: String): String? = null
        override fun setString(key: String, value: String) {}
    }

    private class FakeRepo(private val data: WeatherResponse) : WeatherRepository {
        override suspend fun getSavedPlaceNames(): List<String> = listOf(data.resolvedAddress)
        override suspend fun getPlaceDayCounts(): Map<String, Long> = emptyMap()
        override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse = data
        override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse = data
        override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse = data
        override suspend fun deletePlace(placeName: String) {}
        override suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse = data
        override suspend fun searchForLocations(query: String): List<GeoSearchResult> = emptyList()
        override suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse = data
        override suspend fun fetchAndPersistStations(place: String): StationsResult = StationsResult.Empty
        override suspend fun getPersistedStations(place: String): List<Station> = emptyList()
        override suspend fun checkDataAvailability(place: String, fromDate: String, toDate: String): DataAvailabilityStatus =
            DataAvailabilityStatus.Available
    }

    private fun buildHour(epoch: Long): Hour = Hour(
        datetime = "", datetimeEpoch = epoch, temp = null, feelslike = null, humidity = null,
        dew = null, precip = null, precipprob = null, snow = null, snowdepth = null, preciptype = null,
        windgust = 15.0, windspeed = 10.0, winddir = 200.0, pressure = null, visibility = null,
        cloudcover = null, solarradiation = null, solarenergy = null, uvindex = null, conditions = null,
        icon = null, source = null, stations = null
    )

    private fun buildDay(datetime: String, hours: List<Hour>?): Day = Day(
        datetime = datetime, datetimeEpoch = null, tempmax = 20.0, tempmin = 10.0, temp = 15.0,
        feelslikemax = null, feelslikemin = null, feelslike = null, dew = null, humidity = null,
        precip = null, precipprob = null, precipcover = null, preciptype = null, snow = null,
        snowdepth = null, windgust = 25.0, windspeed = 18.0, winddir = 200.0, pressure = null,
        cloudcover = null, visibility = null, solarradiation = null, solarenergy = null, uvindex = null,
        sunrise = null, sunriseEpoch = null, sunset = null, sunsetEpoch = null, moonphase = null,
        conditions = "Clear", description = null, icon = null, stations = null, source = "test",
        hours = hours, normal = null
    )
}
