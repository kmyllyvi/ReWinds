package core

import ai.TestWeatherRepositoryFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [WeatherRepositoryImpl] — the merge/cache/gap-fetch logic between the
 * Visual Crossing network layer and the persistence layer. Previously 0% covered (KIM-325).
 *
 * Both collaborators are interfaces ([Networking], [Database]) so the repository can be driven
 * entirely with in-memory fakes — no real network or SQLite needed, which keeps these in
 * commonMain test scope. The fakes record calls so we can assert WHICH path the repository took
 * (DB hit vs network fetch, full-replace vs no-op), not just the returned value.
 *
 * [WeatherApiKeyManager] is a global gate consulted by every network request, so it is set to a
 * valid key for the duration of each test and reset afterwards to keep tests order-independent.
 */
class WeatherRepositoryImplTest {

    @BeforeTest
    fun setUp() {
        WeatherApiKeyManager.setApiKey("valid-test-key")
    }

    @AfterTest
    fun tearDown() {
        WeatherApiKeyManager.setApiKey("")
    }

    // ── Fakes ────────────────────────────────────────────────────────────────

    /**
     * In-memory [Networking] fake. Returns a configurable response per call and records the
     * URLs it was asked to fetch, or throws [toThrow] to exercise error paths.
     */
    private class FakeNetworking(
        var weatherResponse: WeatherResponse = TestWeatherRepositoryFactory.createWeatherResponse("Net"),
        var geoResponse: GeoSearchResponse = GeoSearchResponse(results = emptyList()),
        var toThrow: Throwable? = null
    ) : Networking {
        val weatherUrls = mutableListOf<String>()
        val geoUrls = mutableListOf<String>()

        override suspend fun fetchWeatherData(url: String): WeatherResponse {
            weatherUrls.add(url)
            toThrow?.let { throw it }
            return weatherResponse
        }

        override suspend fun fetchGeoSearchData(url: String): GeoSearchResponse {
            geoUrls.add(url)
            toThrow?.let { throw it }
            return geoResponse
        }
    }

    /**
     * In-memory [Database] fake. Holds one [WeatherResponse] per place and a station list per
     * place; [saveWeatherResponse] does a simple by-datetime day merge so the repository's
     * re-query-after-fetch steps see freshly-persisted days.
     */
    private open class FakeDatabase : Database {
        val saved = mutableMapOf<String, WeatherResponse>()
        val stations = mutableMapOf<String, List<Station>>()
        var upsertCalls = 0
        var deleteCalls = 0

        override suspend fun getAllSavedPlaces(): List<String> = saved.keys.toList()

        override suspend fun getPlaceDayCounts(): Map<String, Long> =
            saved.mapValues { (_, v) -> (v.days?.size ?: 0).toLong() }

        override suspend fun getSavedPlaceFull(place: String): WeatherResponse? = saved[place]

        override suspend fun saveWeatherResponse(weatherResponse: WeatherResponse) {
            val key = weatherResponse.resolvedAddress
            val existing = saved[key]
            if (existing == null) {
                saved[key] = weatherResponse
            } else {
                val merged = LinkedHashMap<String, Day>()
                (existing.days ?: emptyList()).forEach { merged[it.datetime] = it }
                (weatherResponse.days ?: emptyList()).forEach { merged[it.datetime] = it }
                saved[key] = existing.copy(days = merged.values.toList())
            }
        }

        override suspend fun getWeatherDataFor(placeName: String, date: String): WeatherResponse? =
            saved[placeName]

        override suspend fun deletePlace(placeName: String) {
            deleteCalls++
            saved.remove(placeName)
            stations.remove(placeName)
        }

        override suspend fun cleanupForecastDays() {}

        override suspend fun upsertStations(place: String, stations: List<Station>) {
            upsertCalls++
            this.stations[place] = stations
        }

        override suspend fun getStationsForPlace(place: String): List<Station> =
            stations[place] ?: emptyList()
    }

    private fun repo(net: FakeNetworking, db: FakeDatabase) =
        WeatherRepositoryImpl(networkService = net, database = db)

    private fun station(id: String, lat: Double?, lon: Double?) = Station(
        id = id, name = id, distance = 0.0, latitude = lat, longitude = lon,
        useCount = 1, quality = 1, contribution = 1.0
    )

    // ── Simple delegating reads ──────────────────────────────────────────────

    @Test
    fun getSavedPlaceNames_returnsDbPlaces() = runTest {
        val db = FakeDatabase().apply {
            saved["Tarifa"] = TestWeatherRepositoryFactory.createWeatherResponse("Tarifa")
        }
        assertEquals(listOf("Tarifa"), repo(FakeNetworking(), db).getSavedPlaceNames())
    }

    @Test
    fun getPlaceDayCounts_returnsCountsPerPlace() = runTest {
        val days = TestWeatherRepositoryFactory.generateTestDays("2026-01-01", "2026-01-03")
        val db = FakeDatabase().apply {
            saved["Tarifa"] = TestWeatherRepositoryFactory.createWeatherResponse("Tarifa", days = days)
        }
        assertEquals(mapOf("Tarifa" to 3L), repo(FakeNetworking(), db).getPlaceDayCounts())
    }

    @Test
    fun getPersistedStations_returnsDbStations() = runTest {
        val db = FakeDatabase().apply { stations["Tarifa"] = listOf(station("s1", 1.0, 2.0)) }
        val result = repo(FakeNetworking(), db).getPersistedStations("Tarifa")
        assertEquals(1, result.size)
        assertEquals("s1", result.single().id)
    }

    // ── getSavedDataFor caching ──────────────────────────────────────────────

    @Test
    fun getSavedDataFor_cachesAfterFirstLoad() = runTest {
        // A counting Database so we can prove the second read is served from the in-memory cache.
        val countingDb = object : FakeDatabase() {
            var fullLoads = 0
            override suspend fun getSavedPlaceFull(place: String): WeatherResponse? {
                fullLoads++
                return saved[place]
            }
        }
        countingDb.saved["Tarifa"] = TestWeatherRepositoryFactory.createWeatherResponse("Tarifa")
        val r = repo(FakeNetworking(), countingDb)

        assertEquals("Tarifa", r.getSavedDataFor("Tarifa")?.resolvedAddress)
        assertEquals("Tarifa", r.getSavedDataFor("Tarifa")?.resolvedAddress)
        assertEquals(1, countingDb.fullLoads, "second read should hit the cache, not the DB")
    }

    @Test
    fun getSavedDataFor_returnsNullWhenAbsent() = runTest {
        assertNull(repo(FakeNetworking(), FakeDatabase()).getSavedDataFor("Nowhere"))
    }

    // ── getDaysRange: single day ─────────────────────────────────────────────

    @Test
    fun getDaysRange_singleDay_servedFromDbWithoutNetwork() = runTest {
        val day = TestWeatherRepositoryFactory.generateTestDays("2026-01-10", "2026-01-10")
        val db = FakeDatabase().apply {
            saved["Tarifa"] = TestWeatherRepositoryFactory.createWeatherResponse("Tarifa", days = day)
        }
        val net = FakeNetworking()
        val result = repo(net, db).getDaysRange("Tarifa", "2026-01-10", null)

        assertEquals(1, result.days?.size)
        assertEquals("2026-01-10", result.days?.single()?.datetime)
        assertTrue(net.weatherUrls.isEmpty(), "existing single day must not trigger a network call")
    }

    @Test
    fun getDaysRange_singleDay_missing_fetchesAndPersists() = runTest {
        val db = FakeDatabase()
        val fetched = TestWeatherRepositoryFactory.createWeatherResponse(
            "ignored-by-network",
            days = TestWeatherRepositoryFactory.generateTestDays("2026-01-10", "2026-01-10")
        )
        val net = FakeNetworking(weatherResponse = fetched)
        val result = repo(net, db).getDaysRange("Tarifa", "2026-01-10", null)

        assertEquals(1, net.weatherUrls.size, "missing day must be fetched once")
        // The repository overwrites the network address with the requested place name.
        assertEquals("Tarifa", result.resolvedAddress)
        assertTrue(db.saved.containsKey("Tarifa"), "fetched day must be persisted under the place name")
    }

    // ── getDaysRange: ranges, gap-fetch, merge ───────────────────────────────

    @Test
    fun getDaysRange_fullRangeInDb_returnsWithoutNetwork() = runTest {
        val days = TestWeatherRepositoryFactory.generateTestDays("2026-01-01", "2026-01-05")
        val db = FakeDatabase().apply {
            saved["Tarifa"] = TestWeatherRepositoryFactory.createWeatherResponse("Tarifa", days = days)
        }
        val net = FakeNetworking()
        val result = repo(net, db).getDaysRange("Tarifa", "2026-01-01", "2026-01-05")

        assertEquals(5, result.days?.size)
        assertTrue(net.weatherUrls.isEmpty(), "complete range in DB must not fetch")
    }

    @Test
    fun getDaysRange_partialRange_fetchesMissingGapAndMerges() = runTest {
        // DB has Jan 1-2; request Jan 1-5 -> gap Jan 3-5 must be fetched and merged in.
        val existing = TestWeatherRepositoryFactory.generateTestDays("2026-01-01", "2026-01-02")
        val db = FakeDatabase().apply {
            saved["Tarifa"] = TestWeatherRepositoryFactory.createWeatherResponse("Tarifa", days = existing)
        }
        val gap = TestWeatherRepositoryFactory.createWeatherResponse(
            "net", days = TestWeatherRepositoryFactory.generateTestDays("2026-01-03", "2026-01-05")
        )
        val net = FakeNetworking(weatherResponse = gap)

        val result = repo(net, db).getDaysRange("Tarifa", "2026-01-01", "2026-01-05")

        assertEquals(1, net.weatherUrls.size, "exactly one contiguous gap should be fetched")
        assertEquals(5, result.days?.size, "merged result must span the full requested range")
        assertEquals(
            listOf("2026-01-01", "2026-01-02", "2026-01-03", "2026-01-04", "2026-01-05"),
            result.days?.map { it.datetime }
        )
    }

    @Test
    fun getDaysRange_gapFetchFailure_isSwallowedAndReturnsWhatExists() = runTest {
        // The gap fetch throws; the repository logs and continues, returning the days already in DB.
        val existing = TestWeatherRepositoryFactory.generateTestDays("2026-01-01", "2026-01-02")
        val db = FakeDatabase().apply {
            saved["Tarifa"] = TestWeatherRepositoryFactory.createWeatherResponse("Tarifa", days = existing)
        }
        val net = FakeNetworking(toThrow = NetworkException("boom", httpStatus = 500))

        val result = repo(net, db).getDaysRange("Tarifa", "2026-01-01", "2026-01-05")

        // Gap fetch failed, so only the pre-existing in-range days come back; no crash.
        assertEquals(2, result.days?.size)
    }

    @Test
    fun getDaysRange_invalidRange_returnsEmptyShellNoNetwork() = runTest {
        // Start after end -> generateDateList yields empty -> empty response shell, no fetch.
        val net = FakeNetworking()
        val result = repo(net, FakeDatabase()).getDaysRange("Tarifa", "2026-01-10", "2026-01-01")

        assertEquals(0, result.days?.size)
        assertEquals("Tarifa", result.resolvedAddress)
        assertTrue(net.weatherUrls.isEmpty())
    }

    // ── downloadFullMonth delegates to getDaysRange ──────────────────────────

    @Test
    fun downloadFullMonth_fetchesWholeMonth() = runTest {
        val db = FakeDatabase()
        val monthDays = TestWeatherRepositoryFactory.createWeatherResponse(
            "net", days = TestWeatherRepositoryFactory.generateTestDays("2026-02-01", "2026-02-28")
        )
        val net = FakeNetworking(weatherResponse = monthDays)

        val result = repo(net, db).downloadFullMonth("Tarifa", 2026, 2)

        assertEquals(1, net.weatherUrls.size)
        assertEquals(28, result.days?.size, "February 2026 has 28 days")
    }

    // ── getPreviousDays always hits network and persists ─────────────────────

    @Test
    fun getPreviousDays_fetchesAndPersists() = runTest {
        val db = FakeDatabase()
        val fetched = TestWeatherRepositoryFactory.createWeatherResponse(
            "Tarifa", days = TestWeatherRepositoryFactory.generateTestDays("2026-01-01", "2026-01-03")
        )
        val net = FakeNetworking(weatherResponse = fetched)

        val result = repo(net, db).getPreviousDays("Tarifa", 3)

        assertEquals(3, result.days?.size)
        assertEquals(1, net.weatherUrls.size)
        assertTrue(db.saved.containsKey("Tarifa"))
    }

    @Test
    fun getPreviousDays_noApiKey_throws() = runTest {
        WeatherApiKeyManager.setApiKey("") // simulate unconfigured gate
        val r = repo(FakeNetworking(), FakeDatabase())
        assertFailsWith<IllegalStateException> { r.getPreviousDays("Tarifa", 3) }
    }

    // ── deletePlace clears DB + cache ────────────────────────────────────────

    @Test
    fun deletePlace_removesFromDb() = runTest {
        val db = FakeDatabase().apply {
            saved["Tarifa"] = TestWeatherRepositoryFactory.createWeatherResponse("Tarifa")
        }
        repo(FakeNetworking(), db).deletePlace("Tarifa")
        assertEquals(1, db.deleteCalls)
        assertTrue(db.saved.isEmpty())
    }

    // ── searchForLocations ───────────────────────────────────────────────────

    @Test
    fun searchForLocations_blankQuery_returnsEmptyWithoutNetwork() = runTest {
        val net = FakeNetworking()
        assertTrue(repo(net, FakeDatabase()).searchForLocations("   ").isEmpty())
        assertTrue(net.geoUrls.isEmpty())
    }

    @Test
    fun searchForLocations_success_returnsResults() = runTest {
        val net = FakeNetworking(
            geoResponse = GeoSearchResponse(
                results = listOf(GeoSearchResult(id = 1, name = "Helsinki", latitude = 60.17, longitude = 24.94))
            )
        )
        val result = repo(net, FakeDatabase()).searchForLocations("Helsinki")
        assertEquals(1, result.size)
        assertEquals("Helsinki", result.single().name)
    }

    @Test
    fun searchForLocations_networkError_returnsEmpty() = runTest {
        val net = FakeNetworking(toThrow = NetworkException("offline"))
        assertTrue(repo(net, FakeDatabase()).searchForLocations("Helsinki").isEmpty())
    }

    // ── addPlaceFromSearch overwrites address + persists ─────────────────────

    @Test
    fun addPlaceFromSearch_persistsUnderSearchName() = runTest {
        val db = FakeDatabase()
        val net = FakeNetworking(
            weatherResponse = TestWeatherRepositoryFactory.createWeatherResponse("api-name")
        )
        val place = GeoSearchResult(id = 1, name = "Tarifa, Spain", latitude = 36.0, longitude = -5.6)

        val result = repo(net, db).addPlaceFromSearch(place)

        assertEquals("Tarifa, Spain", result.resolvedAddress)
        assertEquals("Tarifa, Spain", result.address)
        assertTrue(db.saved.containsKey("Tarifa, Spain"))
    }

    // ── fetchAndPersistStations: success / empty / error ─────────────────────

    @Test
    fun fetchAndPersistStations_success_persistsValidStationsOnly() = runTest {
        val db = FakeDatabase()
        val net = FakeNetworking(
            weatherResponse = TestWeatherRepositoryFactory.createWeatherResponse("Tarifa").copy(
                stations = mapOf(
                    "good" to station("good", 1.0, 2.0),
                    "noCoords" to station("noCoords", null, null)
                )
            )
        )

        val result = repo(net, db).fetchAndPersistStations("Tarifa")

        assertTrue(result is StationsResult.Success)
        assertEquals(1, result.stations.size, "station without coords is dropped")
        assertEquals(1, db.upsertCalls)
    }

    @Test
    fun fetchAndPersistStations_noValidStations_returnsEmptyAndDoesNotPersist() = runTest {
        val db = FakeDatabase()
        val net = FakeNetworking(
            weatherResponse = TestWeatherRepositoryFactory.createWeatherResponse("Tarifa").copy(
                stations = mapOf("noCoords" to station("noCoords", null, null))
            )
        )

        val result = repo(net, db).fetchAndPersistStations("Tarifa")

        assertTrue(result is StationsResult.Empty)
        assertEquals(0, db.upsertCalls, "no upsert when there are no plottable stations")
    }

    @Test
    fun fetchAndPersistStations_networkError_returnsErrorAndLeavesDbUntouched() = runTest {
        val db = FakeDatabase().apply { stations["Tarifa"] = listOf(station("old", 1.0, 2.0)) }
        val net = FakeNetworking(toThrow = NetworkException("down", httpStatus = 503))

        val result = repo(net, db).fetchAndPersistStations("Tarifa")

        assertTrue(result is StationsResult.Error)
        assertEquals(0, db.upsertCalls, "existing rows must be left untouched on error")
        assertEquals(listOf("old"), db.stations["Tarifa"]?.map { it.id })
    }

    // ── checkDataAvailability ────────────────────────────────────────────────

    @Test
    fun checkDataAvailability_allPresent_isAvailable() = runTest {
        val days = TestWeatherRepositoryFactory.generateTestDays("2026-01-01", "2026-01-03")
        val db = FakeDatabase().apply {
            saved["Tarifa"] = TestWeatherRepositoryFactory.createWeatherResponse("Tarifa", days = days)
        }
        assertEquals(
            DataAvailabilityStatus.Available,
            repo(FakeNetworking(), db).checkDataAvailability("Tarifa", "2026-01-01", "2026-01-03")
        )
    }

    @Test
    fun checkDataAvailability_somePresent_isPartial() = runTest {
        val days = TestWeatherRepositoryFactory.generateTestDays("2026-01-01", "2026-01-02")
        val db = FakeDatabase().apply {
            saved["Tarifa"] = TestWeatherRepositoryFactory.createWeatherResponse("Tarifa", days = days)
        }
        assertEquals(
            DataAvailabilityStatus.Partial,
            repo(FakeNetworking(), db).checkDataAvailability("Tarifa", "2026-01-01", "2026-01-05")
        )
    }

    @Test
    fun checkDataAvailability_noPlace_isMissing() = runTest {
        assertEquals(
            DataAvailabilityStatus.Missing,
            repo(FakeNetworking(), FakeDatabase()).checkDataAvailability("Nowhere", "2026-01-01", "2026-01-03")
        )
    }

    // ── observe/getDownloadedMonths delegation ───────────────────────────────

    @Test
    fun downloadedMonths_delegateToDatabase() = runTest {
        val db = object : FakeDatabase() {
            override suspend fun getDownloadedMonths(place: String): Set<String> = setOf("2026-01")
            override fun observeDownloadedMonths(place: String): Flow<Set<String>> = flowOf(setOf("2026-01"))
        }
        val r = repo(FakeNetworking(), db)
        assertEquals(setOf("2026-01"), r.getDownloadedMonths("Tarifa"))
    }
}
