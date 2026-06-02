package home

import core.Database
import core.DatabaseExportImport
import core.GeoSearchResult
import core.WeatherRepository
import core.WeatherResponse
import home.HomeViewModel
import home.PlaceDisplayData
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.OptIn
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Mock implementation of WeatherRepository for testing
 */
class MockWeatherRepository : WeatherRepository {
    override suspend fun searchForLocations(query: String): List<GeoSearchResult> = emptyList()
    override suspend fun getSavedPlaceNames(): List<String> = emptyList()
    override suspend fun getSavedDataFor(resolvedPlace: String): core.WeatherResponse? = null
    override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): core.WeatherResponse {
        return core.WeatherResponse(resolvedAddress = "", address = "", queryCost = 0, latitude = 0.0, longitude = 0.0, timezone = "", tzoffset = 0.0, days = emptyList())
    }
    override suspend fun getPreviousDays(place: String, previousDaysCount: Int): core.WeatherResponse {
        return core.WeatherResponse(resolvedAddress = "", address = "", queryCost = 0, latitude = 0.0, longitude = 0.0, timezone = "", tzoffset = 0.0, days = emptyList())
    }
    override suspend fun addPlaceFromSearch(place: GeoSearchResult): core.WeatherResponse {
        return core.WeatherResponse(resolvedAddress = place.name, address = place.name, queryCost = 0, latitude = place.latitude, longitude = place.longitude, timezone = "", tzoffset = 0.0, days = emptyList())
    }
    override suspend fun deletePlace(name: String) {}
    override suspend fun downloadFullMonth(place: String, year: Int, month: Int): core.WeatherResponse {
        return core.WeatherResponse(resolvedAddress = "", address = "", queryCost = 0, latitude = 0.0, longitude = 0.0, timezone = "", tzoffset = 0.0, days = emptyList())
    }
    override suspend fun checkDataAvailability(place: String, fromDate: String, toDate: String): core.DataAvailabilityStatus {
        return core.DataAvailabilityStatus.Available
    }
    override suspend fun fetchAndPersistStations(place: String): core.StationsResult = core.StationsResult.Empty
    override suspend fun getPersistedStations(place: String): List<core.Station> = emptyList()
}

/**
 * Mock implementation of Database for testing
 */
class MockDatabase : Database {
    private val savedPlaces = mutableMapOf<String, WeatherResponse>()

    override suspend fun getAllSavedPlaces(): List<String> = savedPlaces.keys.toList()

    override suspend fun getSavedPlaceFull(place: String): WeatherResponse? = savedPlaces[place]

    override suspend fun saveWeatherResponse(response: WeatherResponse) {
        savedPlaces[response.resolvedAddress] = response
    }

    override suspend fun getWeatherDataFor(placeName: String, date: String): WeatherResponse? = null

    override suspend fun deletePlace(placeName: String) {
        savedPlaces.remove(placeName)
    }

    override suspend fun cleanupForecastDays() {}

    override suspend fun upsertStations(place: String, stations: List<core.Station>) {}
    override suspend fun getStationsForPlace(place: String): List<core.Station> = emptyList()
}

/**
 * Unit tests for HomeViewModel to verify app logic and state management
 * Tests the core functionality without requiring full DI initialization
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel
    private lateinit var weatherRepository: MockWeatherRepository
    private lateinit var database: MockDatabase
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        // Set the Main dispatcher for ViewModel coroutines (viewModelScope uses Main by default)
        Dispatchers.setMain(testDispatcher)

        // Create mock implementations for testing
        weatherRepository = MockWeatherRepository()
        database = MockDatabase()

        // Initialize ViewModel with mock dependencies
        // Using real DatabaseExportImport since it's final and can't be mocked;
        // these tests don't exercise export/import functionality
        viewModel = HomeViewModel(weatherRepository, DatabaseExportImport(), database)
    }

    @AfterTest
    fun teardown() {
        // Reset the Main dispatcher
        Dispatchers.resetMain()
    }

    @Test
    fun testHomeViewModelInitialState() {
        // Verify initial UI state is correct
        val initialState = viewModel.uiState.value
        assertNotNull(initialState)
        assertEquals(emptyList<PlaceDisplayData>(), initialState.placeDisplayData)
        assertFalse(initialState.showDebugMenu)
        assertEquals("", initialState.debugMessage)
        assertEquals("", initialState.importFilePath)
    }

    @Test
    fun testDebugMenuToggle() {
        // Initially debug menu should be hidden
        assertFalse(viewModel.uiState.value.showDebugMenu)

        // Toggle debug menu
        viewModel.toggleDebugMenu()
        assertTrue(viewModel.uiState.value.showDebugMenu)

        // Toggle again
        viewModel.toggleDebugMenu()
        assertFalse(viewModel.uiState.value.showDebugMenu)
    }

    @Test
    fun testImportFilePathUpdate() {
        assertEquals("", viewModel.uiState.value.importFilePath)

        // Update file path
        val testPath = "/documents/test.db"
        viewModel.onImportFilePathChange(testPath)

        assertEquals(testPath, viewModel.uiState.value.importFilePath)
    }

    @Test
    fun testImportFilePathSetting() {
        // Test that we can set import file path
        assertEquals("", viewModel.uiState.value.importFilePath)

        val testPath = "/documents/test.db"
        viewModel.onImportFilePathChange(testPath)
        assertEquals(testPath, viewModel.uiState.value.importFilePath)

        // Clear the path
        viewModel.onImportFilePathChange("")
        assertEquals("", viewModel.uiState.value.importFilePath)
    }

    @Test
    fun testSearchTextChange() {
        assertEquals("", viewModel.searchText.value)

        // Change search text
        viewModel.onSearchTextChange("Test Place")
        assertEquals("Test Place", viewModel.searchText.value)

        // Clear search text
        viewModel.onSearchTextChange("")
        assertEquals("", viewModel.searchText.value)
    }

    @Test
    fun testDeleteRequestFlow() {
        // Initially no delete confirmation
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)

        // Request delete
        viewModel.onDeleteRequest("Test Place")
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals("Test Place", viewModel.uiState.value.placeToDelete)

        // Cancel delete
        viewModel.onDeleteCancelled()
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun testErrorDismissal() {
        // Verify error dismissal works
        assertFalse(viewModel.uiState.value.showDebugMenu)

        // Toggle debug menu as a sanity check
        viewModel.toggleDebugMenu()
        assertTrue(viewModel.uiState.value.showDebugMenu)

        // Dismiss error (even if no error, function should not crash)
        viewModel.onErrorDismissed()
    }

    @Test
    fun testDeleteCancellationFlow() {
        // Request a delete
        viewModel.onDeleteRequest("Place To Delete")
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        // Cancel it
        viewModel.onDeleteCancelled()
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(null, viewModel.uiState.value.placeToDelete)
    }
}
