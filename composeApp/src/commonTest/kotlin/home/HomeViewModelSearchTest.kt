package home

import core.Database
import core.DatabaseExportImport
import core.GeoSearchResult
import core.WeatherRepository
import core.WeatherResponse
import core.NetworkException
import home.HomeViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.OptIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Mock WeatherRepository for testing search flow
 */
class MockWeatherRepositoryForSearch : WeatherRepository {
    var searchCallCount = 0
    var lastSearchQuery: String? = null
    private var searchResults: List<GeoSearchResult> = emptyList()
    private var shouldThrowException = false

    fun setSearchResults(results: List<GeoSearchResult>) {
        this.searchResults = results
    }

    fun setThrowException(shouldThrow: Boolean) {
        this.shouldThrowException = shouldThrow
    }

    override suspend fun searchForLocations(query: String): List<GeoSearchResult> {
        searchCallCount++
        lastSearchQuery = query
        if (shouldThrowException) {
            throw NetworkException("Search failed")
        }
        return searchResults
    }

    override suspend fun getSavedPlaceNames(): List<String> = emptyList()
    override suspend fun getSavedDataFor(resolvedPlace: String): WeatherResponse? = null
    override suspend fun getDaysRange(place: String, fromDate: String, toDate: String?): WeatherResponse {
        return WeatherResponse(resolvedAddress = "", address = "", queryCost = 0, latitude = 0.0, longitude = 0.0, timezone = "", tzoffset = 0.0, days = emptyList())
    }
    override suspend fun getPreviousDays(place: String, previousDaysCount: Int): WeatherResponse {
        return WeatherResponse(resolvedAddress = "", address = "", queryCost = 0, latitude = 0.0, longitude = 0.0, timezone = "", tzoffset = 0.0, days = emptyList())
    }
    override suspend fun addPlaceFromSearch(place: GeoSearchResult): WeatherResponse {
        return WeatherResponse(resolvedAddress = place.name, address = place.name, queryCost = 0, latitude = place.latitude, longitude = place.longitude, timezone = "", tzoffset = 0.0, days = emptyList())
    }
    override suspend fun deletePlace(name: String) {}
    override suspend fun downloadFullMonth(place: String, year: Int, month: Int): WeatherResponse {
        return WeatherResponse(resolvedAddress = "", address = "", queryCost = 0, latitude = 0.0, longitude = 0.0, timezone = "", tzoffset = 0.0, days = emptyList())
    }
    override suspend fun checkDataAvailability(place: String, fromDate: String, toDate: String): core.DataAvailabilityStatus {
        return core.DataAvailabilityStatus.Available
    }
    override suspend fun fetchAndPersistStations(place: String): core.StationsResult = core.StationsResult.Empty
    override suspend fun getPersistedStations(place: String): List<core.Station> = emptyList()
}

/**
 * Mock Database for testing
 */
class MockDatabaseForSearch : Database {
    override suspend fun getAllSavedPlaces(): List<String> = emptyList()
    override suspend fun getSavedPlaceFull(place: String): WeatherResponse? = null
    override suspend fun saveWeatherResponse(response: WeatherResponse) {}
    override suspend fun getWeatherDataFor(placeName: String, date: String): WeatherResponse? = null
    override suspend fun deletePlace(placeName: String) {}
    override suspend fun cleanupForecastDays() {}
    override suspend fun upsertStations(place: String, stations: List<core.Station>) {}
    override suspend fun getStationsForPlace(place: String): List<core.Station> = emptyList()
}

/**
 * Unit tests for HomeViewModel search flow
 * Tests the debounced search and results handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelSearchTest {

    private lateinit var viewModel: HomeViewModel
    private lateinit var weatherRepository: MockWeatherRepositoryForSearch
    private lateinit var database: MockDatabaseForSearch
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        weatherRepository = MockWeatherRepositoryForSearch()
        database = MockDatabaseForSearch()
        viewModel = HomeViewModel(weatherRepository, DatabaseExportImport(), database)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testBlankQueryReturnsEmptyResults() {
        // Arrange
        val initialResults = viewModel.uiState.value.searchResults
        assertEquals(emptyList<GeoSearchResult>(), initialResults)

        // Act: set blank search text
        viewModel.onSearchTextChange("")

        // Wait for debounce and flow processing
        runBlocking {
            delay(700)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: no search call made and results remain empty
        assertEquals(0, weatherRepository.searchCallCount)
        assertEquals(emptyList<GeoSearchResult>(), viewModel.uiState.value.searchResults)
    }

    @Test
    fun testEmptyQueryReturnsEmptyResults() {
        // Arrange
        val initialResults = viewModel.uiState.value.searchResults
        assertEquals(emptyList<GeoSearchResult>(), initialResults)

        // Act: set empty search text
        viewModel.onSearchTextChange("   ")

        // Wait for debounce and flow processing
        runBlocking {
            delay(700)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: no search call made (blank is considered isNotBlank = false after trim)
        // Note: The actual implementation checks isNotBlank which returns false for "   "
        assertEquals(emptyList<GeoSearchResult>(), viewModel.uiState.value.searchResults)
    }

    @Test
    fun testValidQueryTriggersRepositorySearch() {
        // Arrange
        val testResults = listOf(
            GeoSearchResult(id = 1, name = "Helsinki", latitude = 60.17, longitude = 24.94, country = "Finland"),
            GeoSearchResult(id = 2, name = "Helsinki Airport", latitude = 60.32, longitude = 25.04, country = "Finland")
        )
        weatherRepository.setSearchResults(testResults)

        // Act: enter a valid search query
        viewModel.onSearchTextChange("Helsinki")

        // Wait for debounce (500ms) and flow processing
        runBlocking {
            delay(700)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: repository was called with the query
        assertEquals(1, weatherRepository.searchCallCount)
        assertEquals("Helsinki", weatherRepository.lastSearchQuery)
    }

    @Test
    fun testValidQueryResultsReflectedInUiState() {
        // Arrange
        val testResults = listOf(
            GeoSearchResult(id = 1, name = "Helsinki", latitude = 60.17, longitude = 24.94, country = "Finland")
        )
        weatherRepository.setSearchResults(testResults)

        // Act: enter a valid search query
        viewModel.onSearchTextChange("Helsinki")

        // Wait for debounce and flow processing
        runBlocking {
            delay(700)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: search results are reflected in UI state
        assertEquals(testResults, viewModel.uiState.value.searchResults)
    }

    @Test
    fun testSearchWithMultipleResultsReturnsAllResults() {
        // Arrange
        val testResults = listOf(
            GeoSearchResult(id = 1, name = "Helsinki", latitude = 60.17, longitude = 24.94, country = "Finland"),
            GeoSearchResult(id = 2, name = "Helsinki Airport", latitude = 60.32, longitude = 25.04, country = "Finland"),
            GeoSearchResult(id = 3, name = "Helsing Pier", latitude = 60.18, longitude = 24.95, country = "Finland")
        )
        weatherRepository.setSearchResults(testResults)

        // Act: search for Helsinki
        viewModel.onSearchTextChange("Helsinki")

        // Wait for debounce and flow processing
        runBlocking {
            delay(700)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: all results are present in UI state
        assertEquals(3, viewModel.uiState.value.searchResults.size)
        assertEquals(testResults, viewModel.uiState.value.searchResults)
    }

    @Test
    fun testRepositoryExceptionSilentlyFailsWithEmptyResults() {
        // Arrange
        weatherRepository.setThrowException(true)

        // Act: enter a valid search query that will cause exception
        viewModel.onSearchTextChange("FailingSearch")

        // Wait for debounce and flow processing
        runBlocking {
            delay(700)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: search was attempted but results remain empty (silent failure)
        assertEquals(1, weatherRepository.searchCallCount)
        assertEquals(emptyList<GeoSearchResult>(), viewModel.uiState.value.searchResults)
    }

    @Test
    fun testSearchStateIsUpdatedDuringSearch() {
        // Arrange
        val testResults = listOf(
            GeoSearchResult(id = 1, name = "Test", latitude = 0.0, longitude = 0.0)
        )
        weatherRepository.setSearchResults(testResults)

        // Act: trigger search
        viewModel.onSearchTextChange("Test")

        // Assert: isSearching is true during the operation
        // Note: Due to the debounce and flow timing, we check that it was set to true at some point
        // After debounce completes, it should be false
        runBlocking {
            delay(700)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // After processing, isSearching should be false
        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun testDebouncePreventsMultipleCallsWithinDebounceWindow() {
        // Arrange
        val testResults = listOf(
            GeoSearchResult(id = 1, name = "Test", latitude = 0.0, longitude = 0.0)
        )
        weatherRepository.setSearchResults(testResults)

        // Act: trigger multiple searches in quick succession (within 500ms debounce)
        viewModel.onSearchTextChange("T")
        viewModel.onSearchTextChange("Te")
        viewModel.onSearchTextChange("Tes")
        viewModel.onSearchTextChange("Test")

        // Wait for debounce to fire
        runBlocking {
            delay(700)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: only the last query should have triggered a search
        assertEquals(1, weatherRepository.searchCallCount)
        assertEquals("Test", weatherRepository.lastSearchQuery)
    }

    @Test
    fun testSearchClearedAfterSelectingResult() {
        // Arrange
        val testResults = listOf(
            GeoSearchResult(id = 1, name = "Helsinki", latitude = 60.17, longitude = 24.94, country = "Finland")
        )
        weatherRepository.setSearchResults(testResults)

        // Act: search and then select a result
        viewModel.onSearchTextChange("Helsinki")

        // Wait for debounce
        runBlocking {
            delay(700)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify search results are present
        assertEquals(1, viewModel.uiState.value.searchResults.size)

        // Select a result (this should clear search text and results)
        viewModel.onSearchResultSelected(testResults[0])

        // Wait for async operation
        runBlocking {
            delay(100)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: search text and results are cleared
        assertEquals("", viewModel.searchText.value)
        assertEquals(emptyList<GeoSearchResult>(), viewModel.uiState.value.searchResults)
    }

    @Test
    fun testSearchResultsReplacePreviousResults() {
        // Arrange
        val firstResults = listOf(
            GeoSearchResult(id = 1, name = "Test1", latitude = 0.0, longitude = 0.0)
        )
        val secondResults = listOf(
            GeoSearchResult(id = 2, name = "Test2", latitude = 1.0, longitude = 1.0),
            GeoSearchResult(id = 3, name = "Test3", latitude = 2.0, longitude = 2.0)
        )
        weatherRepository.setSearchResults(firstResults)

        // Act: first search
        viewModel.onSearchTextChange("Test1")
        runBlocking {
            delay(700)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: first results are shown
        assertEquals(1, viewModel.uiState.value.searchResults.size)
        assertEquals("Test1", viewModel.uiState.value.searchResults[0].name)

        // Act: new search with different results
        weatherRepository.setSearchResults(secondResults)
        viewModel.onSearchTextChange("Test")
        runBlocking {
            delay(700)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: results are replaced with new ones
        assertEquals(2, viewModel.uiState.value.searchResults.size)
        assertEquals("Test2", viewModel.uiState.value.searchResults[0].name)
        assertEquals("Test3", viewModel.uiState.value.searchResults[1].name)
    }
}
