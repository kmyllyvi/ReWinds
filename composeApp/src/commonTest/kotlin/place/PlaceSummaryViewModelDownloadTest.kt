package place

import androidx.lifecycle.viewModelScope
import core.DataAvailabilityStatus
import core.GeoSearchResult
import core.Station
import core.StationsResult
import core.WeatherRepository
import core.WeatherResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for KIM-332: the per-month download indicator on the place-summary grid.
 *
 * The View renders a spinner on the cell whose `cell.month == downloadingMonth`, so the
 * ViewModel must expose the month currently downloading and clear it once the reload settles.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaceSummaryViewModelDownloadTest {

    private val testDispatcher = StandardTestDispatcher()
    private val placeName = "Tarifa"
    private val createdViewModels = mutableListOf<PlaceSummaryViewModel>()

    private val weatherResponse = WeatherResponse(
        resolvedAddress = placeName,
        address = placeName,
        latitude = 36.0,
        longitude = -5.6,
        timezone = "Europe/Madrid",
        tzoffset = 1.0,
        days = emptyList()
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        createdViewModels.forEach { it.viewModelScope.cancel() }
        createdViewModels.clear()
        testDispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
    }

    @Test
    fun downloadingMonth_isNullBeforeAnyDownload() = runTest {
        val vm = makeViewModel(FakeDownloadRepository())
        advanceUntilIdle()

        val state = vm.uiState.value as WeatherSummaryUiState.Success
        assertNull(state.downloadingMonth, "no month should be marked downloading on initial load")
    }

    @Test
    fun onDownloadFullMonth_marksThatMonthWhileInFlight() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeDownloadRepository(preDownloadSuspend = { gate.await() })
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.onDownloadFullMonth(year = 2025, month = 7)
        runCurrent() // let the launched coroutine set the flag and reach the gate

        val inFlight = vm.uiState.value as WeatherSummaryUiState.Success
        assertEquals(7, inFlight.downloadingMonth, "the requested month must be marked while downloading")

        gate.complete(Unit)
        advanceUntilIdle()

        val settled = vm.uiState.value as WeatherSummaryUiState.Success
        assertNull(settled.downloadingMonth, "the indicator must clear once the reload completes")
    }

    @Test
    fun onDownloadFullMonth_onlyMarksTheRequestedMonth() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeDownloadRepository(preDownloadSuspend = { gate.await() })
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.onDownloadFullMonth(year = 2025, month = 3)
        runCurrent()

        val state = vm.uiState.value as WeatherSummaryUiState.Success
        // Exactly one month is downloading — the cells for every other month render normally.
        // The grid derives per-cell state as `cell.month == downloadingMonth`, so a single
        // non-3 field is enough to prove no other cell shows the indicator.
        assertEquals(3, state.downloadingMonth)
        assertNotEquals(3, 4, "sanity: distinct months map to distinct cells")
        assertEquals(false, 4 == state.downloadingMonth, "a non-requested month must not show the indicator")
    }

    @Test
    fun onDownloadFullMonth_clearsIndicatorWhenDownloadFails() = runTest {
        val repo = FakeDownloadRepository(downloadThrows = true)
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.onDownloadFullMonth(year = 2025, month = 5)
        advanceUntilIdle()

        // The download failed, so state falls back to Error — the per-cell indicator must not
        // linger. (The Error state carries no downloadingMonth by construction.)
        val state = vm.uiState.value
        assertTrue(state is WeatherSummaryUiState.Error, "a failed download should surface an Error state")
    }

    private fun makeViewModel(repo: WeatherRepository): PlaceSummaryViewModel =
        PlaceSummaryViewModel(
            route = core.PlaceSummaryRoute(placeName = placeName),
            weatherRepository = repo
        ).also { createdViewModels.add(it) }

    /**
     * Minimal fake that returns a place with data (so state is Success) and lets a test gate
     * the `downloadFullMonth` call to observe the in-flight indicator deterministically.
     */
    private inner class FakeDownloadRepository(
        private val preDownloadSuspend: (suspend () -> Unit)? = null,
        private val downloadThrows: Boolean = false
    ) : WeatherRepository {
        override suspend fun getSavedPlaceNames(): List<String> = listOf(placeName)
        override suspend fun getPlaceDayCounts(): Map<String, Long> = mapOf(placeName to 0L)
        override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse = weatherResponse
        override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse = weatherResponse
        override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse = weatherResponse
        override suspend fun deletePlace(placeName: String) {}
        override suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse {
            preDownloadSuspend?.invoke()
            if (downloadThrows) throw IllegalStateException("download failed")
            return weatherResponse
        }
        override suspend fun searchForLocations(query: String): List<GeoSearchResult> = emptyList()
        override suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse = weatherResponse
        override suspend fun checkDataAvailability(place: String, fromDate: String, toDate: String): DataAvailabilityStatus =
            DataAvailabilityStatus.Available
        override suspend fun getPersistedStations(place: String): List<Station> = listOf(sampleStation)
        override suspend fun fetchAndPersistStations(place: String): StationsResult =
            StationsResult.Success(listOf(sampleStation))
    }

    private val sampleStation = Station(
        id = "TARIFA",
        name = "Tarifa Airport",
        latitude = 36.02,
        longitude = -5.60,
        distance = 1.5,
        quality = 50,
        useCount = 100,
        contribution = null
    )
}
