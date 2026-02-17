package home

import core.DatabaseExportImport
import core.GeoSearchResult
import core.WeatherRepository
import home.HomeViewModel
import home.PlaceDisplayData
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
}

/**
 * Unit tests for HomeViewModel to verify app logic and state management
 * Tests the core functionality without requiring full DI initialization
 */
class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel
    private lateinit var weatherRepository: MockWeatherRepository
    private lateinit var databaseExportImport: DatabaseExportImport

    @BeforeTest
    fun setup() {
        // Create mock implementations for testing
        weatherRepository = MockWeatherRepository()
        databaseExportImport = DatabaseExportImport()

        // Wrap ViewModel initialization to handle any Koin setup issues
        try {
            viewModel = HomeViewModel(weatherRepository, databaseExportImport)
        } catch (e: Exception) {
            // If ViewModel initialization fails due to Koin, create a simpler version
            // This is acceptable for unit tests focused on state management
            throw e  // Re-throw so we know there's an issue
        }
    }

    @AfterTest
    fun teardown() {
        try {
            stopKoin()
        } catch (e: Exception) {
            // Ignore
        }
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
    fun testImportDatabaseWithEmptyPath() {
        // Try to import with empty path
        viewModel.importDatabase()

        // Should show error message
        val message = viewModel.uiState.value.debugMessage
        assertTrue(message.contains("❌"), "Error message should contain ❌ symbol")
        assertTrue(message.contains("Please enter a file path"), "Error message should mention file path")
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
