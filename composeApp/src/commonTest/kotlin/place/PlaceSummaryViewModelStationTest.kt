package place

import core.DataAvailabilityStatus
import core.GeoSearchResult
import core.Station
import core.StationsResult
import core.WeatherRepository
import core.WeatherResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for KIM-258 station-related logic in PlaceSummaryViewModel.
 *
 * Covers the four cases called out in the acceptance criteria:
 *   (a) auto-backfill fires when station table is empty for a place
 *   (b) refreshStations() replaces existing stations
 *   (c) empty-result case sets stations to empty list without crashing
 *   (d) error case leaves prior stations intact
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaceSummaryViewModelStationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val placeName = "Tarifa"

    private val weatherResponse = WeatherResponse(
        resolvedAddress = placeName,
        address = placeName,
        latitude = 36.0,
        longitude = -5.6,
        timezone = "Europe/Madrid",
        tzoffset = 1.0,
        days = emptyList()
    )

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

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── (a) Auto-backfill ──────────────────────────────────────────────────────

    @Test
    fun autoBackfill_firesWhenStationTableIsEmpty() = runTest {
        var fetchCalled = false
        val repo = FakeWeatherRepository(
            data = weatherResponse,
            persistedStations = emptyList(), // empty → backfill should fire
            fetchStationsResult = {
                fetchCalled = true
                StationsResult.Success(listOf(sampleStation))
            }
        )

        val vm = makePlaceSummaryViewModel(repo)
        advanceUntilIdle()

        assertTrue(fetchCalled, "fetchAndPersistStations should be called when no stations are stored")
        val state = vm.uiState.value as? WeatherSummaryUiState.Success
        assertNotNull(state)
        assertEquals(1, state.stations.size)
        assertEquals("Tarifa Airport", state.stations[0].name)
    }

    @Test
    fun autoBackfill_doesNotFireWhenStationsAlreadyExist() = runTest {
        var fetchCalled = false
        val repo = FakeWeatherRepository(
            data = weatherResponse,
            persistedStations = listOf(sampleStation),
            fetchStationsResult = {
                fetchCalled = true
                StationsResult.Success(listOf(sampleStation))
            }
        )

        val vm = makePlaceSummaryViewModel(repo)
        advanceUntilIdle()

        assertTrue(!fetchCalled, "fetchAndPersistStations should NOT be called when stations already exist")
        val state = vm.uiState.value as? WeatherSummaryUiState.Success
        assertNotNull(state)
        assertEquals(1, state.stations.size)
    }

    // ─── (b) refreshStations() replaces existing stations ──────────────────────

    @Test
    fun refreshStations_replacesExistingStations() = runTest {
        val updatedStation = Station(
            id = "NEW",
            name = "New Station",
            latitude = 36.10,
            longitude = -5.70,
            distance = null,
            quality = null,
            useCount = null,
            contribution = null
        )

        var callCount = 0
        val repo = FakeWeatherRepository(
            data = weatherResponse,
            persistedStations = listOf(sampleStation),
            fetchStationsResult = {
                callCount++
                StationsResult.Success(listOf(updatedStation))
            }
        )

        val vm = makePlaceSummaryViewModel(repo)
        advanceUntilIdle()

        // After load, old stations in place (no auto-backfill since table was not empty)
        var state = vm.uiState.value as? WeatherSummaryUiState.Success
        assertEquals(1, state?.stations?.size)
        assertEquals("Tarifa Airport", state?.stations?.get(0)?.name)

        // User triggers a refresh
        vm.refreshStations()
        advanceUntilIdle()

        state = vm.uiState.value as? WeatherSummaryUiState.Success
        assertNotNull(state)
        assertEquals(1, state.stations.size)
        assertEquals("New Station", state.stations[0].name, "Stations should be replaced by refresh")
        assertEquals(1, callCount, "fetchAndPersistStations should have been called once (by refresh)")
    }

    // ─── (c) Empty result case ──────────────────────────────────────────────────

    @Test
    fun emptyResultCase_setsStationsToEmptyListWithoutCrash() = runTest {
        val repo = FakeWeatherRepository(
            data = weatherResponse,
            persistedStations = emptyList(),
            fetchStationsResult = { StationsResult.Empty }
        )

        val vm = makePlaceSummaryViewModel(repo)
        advanceUntilIdle()

        val state = vm.uiState.value as? WeatherSummaryUiState.Success
        assertNotNull(state, "State should be Success even when API returns empty stations")
        assertTrue(state.stations.isEmpty(), "stations should be empty list for Empty result")
        assertNull(state.stationsError, "No error should be reported for Empty result")
    }

    // ─── (d) Error case leaves prior stations intact ────────────────────────────

    @Test
    fun errorCase_leavesPriorStationsIntact() = runTest {
        // The repository has stations stored already (auto-backfill won't fire).
        // When refreshStations() is called, the network fails.
        // After the error, the previously-persisted station should still be visible in state.
        val repo = FakeWeatherRepository(
            data = weatherResponse,
            persistedStations = listOf(sampleStation),
            fetchStationsResult = { StationsResult.Error("Network timeout") }
        )

        val vm = makePlaceSummaryViewModel(repo)
        advanceUntilIdle()

        // Sanity: initial load shows the persisted station, no error
        val stateAfterLoad = vm.uiState.value as? WeatherSummaryUiState.Success
        assertNotNull(stateAfterLoad)
        assertEquals(1, stateAfterLoad.stations.size)
        assertNull(stateAfterLoad.stationsError)

        // User triggers a refresh, which fails
        vm.refreshStations()
        advanceUntilIdle()

        val stateAfterError = vm.uiState.value as? WeatherSummaryUiState.Success
        assertNotNull(stateAfterError)
        // Prior stations should still be visible (fallback to getPersistedStations)
        assertEquals(1, stateAfterError.stations.size, "Prior stations must remain after a network error")
        assertEquals("Tarifa Airport", stateAfterError.stations[0].name)
        assertNotNull(stateAfterError.stationsError, "An error message should be exposed in state")
        assertEquals("Network timeout", stateAfterError.stationsError)
    }

    // ─── Additional QA coverage (KIM-258 gaps) ─────────────────────────────────

    /**
     * isRefreshingStations must flip true while fetchAndPersistStations is in flight
     * and back to false once it settles. We gate the fake repo with a CompletableDeferred
     * so we can observe both states deterministically.
     */
    @Test
    fun refreshStations_togglesIsRefreshingStationsFlagAroundFetch() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeWeatherRepository(
            data = weatherResponse,
            persistedStations = listOf(sampleStation),
            fetchStationsResult = { StationsResult.Success(listOf(sampleStation)) },
            preFetchSuspend = { gate.await() }
        )

        val vm = makePlaceSummaryViewModel(repo)
        advanceUntilIdle()
        assertFalse(vm.isRefreshingStations.value, "initial state should not be refreshing")

        vm.refreshStations()
        runCurrent() // let the launched coroutine start and reach the gate
        assertTrue(vm.isRefreshingStations.value, "flag must be true while fetch is in flight")

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.isRefreshingStations.value, "flag must be reset after fetch completes")
    }

    /**
     * Regression guard for auto-backfill: after the very first load triggers backfill,
     * subsequent reloads (e.g. refreshData() after a download) must not re-fire the
     * station fetch — that would be an unintended extra API call.
     */
    @Test
    fun autoBackfill_doesNotRefireOnReloadAfterStationsArePersisted() = runTest {
        var fetchCalled = 0
        val repo = FakeWeatherRepository(
            data = weatherResponse,
            persistedStations = emptyList(),
            fetchStationsResult = {
                fetchCalled++
                StationsResult.Success(listOf(sampleStation))
            }
        )

        val vm = makePlaceSummaryViewModel(repo)
        advanceUntilIdle()
        assertEquals(1, fetchCalled, "auto-backfill should fire once on initial load")

        // Trigger a reload — stations are now persisted, so backfill must NOT fire again.
        vm.refreshData()
        advanceUntilIdle()
        assertEquals(1, fetchCalled, "auto-backfill must not re-fire on subsequent loads")
    }

    /**
     * Documents current behaviour for StationsResult.Empty when prior stations exist.
     * The impl returns Pair(emptyList(), null), which clears the displayed list on refresh.
     * If product later decides Empty should preserve prior rows, this test will flag it.
     */
    @Test
    fun emptyResult_onRefreshWithPriorStations_clearsDisplayedList() = runTest {
        // Prior stations persisted, so initial load won't fetch. Refresh returns Empty.
        val repo = FakeWeatherRepository(
            data = weatherResponse,
            persistedStations = listOf(sampleStation),
            fetchStationsResult = { StationsResult.Empty }
        )

        val vm = makePlaceSummaryViewModel(repo)
        advanceUntilIdle()

        vm.refreshStations()
        advanceUntilIdle()

        val state = vm.uiState.value as? WeatherSummaryUiState.Success
        assertNotNull(state)
        assertTrue(
            state.stations.isEmpty(),
            "Current impl: Empty result on refresh replaces displayed list with empty"
        )
        assertNull(state.stationsError)
    }

    /**
     * Concurrent / repeated refreshStations() calls must not crash or corrupt state.
     * Each call launches its own coroutine; final state should reflect a successful fetch
     * and isRefreshingStations should settle to false.
     */
    @Test
    fun refreshStations_repeatedRapidCallsSettleCleanly() = runTest {
        var callCount = 0
        val repo = FakeWeatherRepository(
            data = weatherResponse,
            persistedStations = listOf(sampleStation),
            fetchStationsResult = {
                callCount++
                StationsResult.Success(listOf(sampleStation.copy(id = "call-$callCount")))
            }
        )

        val vm = makePlaceSummaryViewModel(repo)
        advanceUntilIdle()

        // Fire three refreshes back-to-back without awaiting between them.
        vm.refreshStations()
        vm.refreshStations()
        vm.refreshStations()
        advanceUntilIdle()

        assertEquals(3, callCount, "each refreshStations call should reach the repo")
        assertFalse(vm.isRefreshingStations.value, "refreshing flag must settle to false")
        val state = vm.uiState.value as? WeatherSummaryUiState.Success
        assertNotNull(state)
        assertEquals(1, state.stations.size, "final state should hold the last fetch's single station")
    }

    /**
     * Defense-in-depth: the ViewModel's toDisplayData() also drops stations with null
     * lat/lon (the repository is the primary filter, but this protects against any
     * future code path that hands the VM raw stations).
     */
    @Test
    fun viewModel_dropsStationsWithNullLatOrLon() = runTest {
        val good = sampleStation
        val noLat = sampleStation.copy(id = "noLat", latitude = null)
        val noLon = sampleStation.copy(id = "noLon", longitude = null)
        val noBoth = sampleStation.copy(id = "noBoth", latitude = null, longitude = null)

        val repo = FakeWeatherRepository(
            data = weatherResponse,
            persistedStations = listOf(good, noLat, noLon, noBoth),
            fetchStationsResult = { StationsResult.Success(listOf(good)) }
        )

        val vm = makePlaceSummaryViewModel(repo)
        advanceUntilIdle()

        val state = vm.uiState.value as? WeatherSummaryUiState.Success
        assertNotNull(state)
        assertEquals(1, state.stations.size, "stations with null lat/lon must be filtered out")
        assertEquals("Tarifa Airport", state.stations[0].name)
    }

    // ─── Map visibility (KIM-259) ────────────────────────────────────────────────

    @Test
    fun showStationMap_startsHidden() = runTest {
        val repo = FakeWeatherRepository(
            data = weatherResponse,
            persistedStations = listOf(sampleStation),
            fetchStationsResult = { StationsResult.Success(listOf(sampleStation)) }
        )
        val vm = makePlaceSummaryViewModel(repo)
        advanceUntilIdle()

        assertFalse(vm.showStationMap.value, "map should be hidden on init")
    }

    @Test
    fun openStationMap_makesShowStationMapTrue() = runTest {
        val repo = FakeWeatherRepository(
            data = weatherResponse,
            persistedStations = listOf(sampleStation),
            fetchStationsResult = { StationsResult.Success(listOf(sampleStation)) }
        )
        val vm = makePlaceSummaryViewModel(repo)
        advanceUntilIdle()

        vm.openStationMap()

        assertTrue(vm.showStationMap.value, "openStationMap() should set showStationMap to true")
    }

    @Test
    fun closeStationMap_makesShowStationMapFalse() = runTest {
        val repo = FakeWeatherRepository(
            data = weatherResponse,
            persistedStations = listOf(sampleStation),
            fetchStationsResult = { StationsResult.Success(listOf(sampleStation)) }
        )
        val vm = makePlaceSummaryViewModel(repo)
        advanceUntilIdle()

        vm.openStationMap()
        assertTrue(vm.showStationMap.value)

        vm.closeStationMap()
        assertFalse(vm.showStationMap.value, "closeStationMap() should set showStationMap to false")
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private fun makePlaceSummaryViewModel(repo: WeatherRepository): PlaceSummaryViewModel {
        return PlaceSummaryViewModel(
            route = core.PlaceSummaryRoute(placeName = placeName),
            weatherRepository = repo
        )
    }
}

/**
 * Configurable fake WeatherRepository for station tests.
 * All non-station methods return minimal/empty responses.
 */
private class FakeWeatherRepository(
    private val data: WeatherResponse,
    private var persistedStations: List<Station>,
    private val fetchStationsResult: () -> StationsResult,
    private val preFetchSuspend: (suspend () -> Unit)? = null
) : WeatherRepository {

    override suspend fun getSavedPlaceNames(): List<String> = listOf(data.resolvedAddress)
    override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse = data
    override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse = data
    override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse = data
    override suspend fun deletePlace(placeName: String) {}
    override suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse = data
    override suspend fun searchForLocations(query: String): List<GeoSearchResult> = emptyList()
    override suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse = data
    override suspend fun checkDataAvailability(place: String, fromDate: String, toDate: String): DataAvailabilityStatus =
        DataAvailabilityStatus.Available

    override suspend fun getPersistedStations(place: String): List<Station> = persistedStations

    override suspend fun fetchAndPersistStations(place: String): StationsResult {
        preFetchSuspend?.invoke()
        val result = fetchStationsResult()
        // Mirror the real impl: on success, update the in-memory persisted list
        if (result is StationsResult.Success) {
            persistedStations = result.stations
        }
        return result
    }
}
