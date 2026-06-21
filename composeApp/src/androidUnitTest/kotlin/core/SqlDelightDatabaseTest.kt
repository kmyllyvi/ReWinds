package core

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.km.rewinds.db.AppDatabase
import com.km.rewinds.db.Day as DayAdapterClass
import com.km.rewinds.db.Hour as HourAdapterClass
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Direct tests for [SqlDelightDatabase] — the real SQLDelight-backed persistence layer.
 *
 * Runs on the JVM (testDebugUnitTest) against an in-memory SQLite database via the JDBC driver.
 * This is the only path that exercises [SqlDelightDatabase.saveWeatherResponse]'s merge logic
 * (new-vs-existing place, day dedup, station replace) and the multi-level reassembly in
 * [SqlDelightDatabase.getSavedPlaceFull] — none of which was covered before (see
 * docs/agent/testing/COVERAGE-GAP-ANALYSIS.md §2.1).
 *
 * Lives in androidUnitTest (not commonTest) because the JDBC/in-memory driver is JVM-only.
 */
class SqlDelightDatabaseTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val appDatabase = AppDatabase(
            driver = driver,
            DayAdapter = DayAdapterClass.Adapter(preciptypeAdapter = listOfStringAdapter),
            HourAdapter = HourAdapterClass.Adapter(preciptypeAdapter = listOfStringAdapter)
        )
        database = SqlDelightDatabase(appDatabase)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    // ── Test data builders ──────────────────────────────────────────────────────

    private fun day(
        datetime: String,
        temp: Double = 18.0,
        hours: List<Hour> = emptyList()
    ): Day = Day(
        datetime = datetime,
        datetimeEpoch = 0L,
        tempmax = temp + 4,
        tempmin = temp - 4,
        temp = temp,
        feelslikemax = null,
        feelslikemin = null,
        feelslike = null,
        dew = null,
        humidity = 60.0,
        precip = null,
        precipprob = null,
        precipcover = null,
        preciptype = null,
        snow = null,
        snowdepth = null,
        windgust = 12.0,
        windspeed = 10.0,
        winddir = 230.0,
        pressure = null,
        cloudcover = null,
        visibility = null,
        solarradiation = null,
        solarenergy = null,
        uvindex = null,
        sunrise = null,
        sunriseEpoch = null,
        sunset = null,
        sunsetEpoch = null,
        moonphase = null,
        conditions = "Clear",
        description = null,
        icon = null,
        stations = null,
        source = "test",
        hours = hours,
        normal = null
    )

    private fun hour(datetime: String, temp: Double = 17.0): Hour = Hour(
        datetime = datetime,
        datetimeEpoch = 0L,
        temp = temp,
        feelslike = null,
        humidity = null,
        dew = null,
        precip = null,
        precipprob = null,
        snow = null,
        snowdepth = null,
        preciptype = null,
        windgust = null,
        windspeed = 9.0,
        winddir = null,
        pressure = null,
        visibility = null,
        cloudcover = null,
        solarradiation = null,
        solarenergy = null,
        uvindex = null,
        conditions = null,
        icon = null,
        source = "test",
        stations = null
    )

    private fun station(id: String, lat: Double, lon: Double): Station = Station(
        id = id,
        name = "Station $id",
        distance = 5.0,
        latitude = lat,
        longitude = lon,
        useCount = 1,
        quality = 100,
        contribution = 0.5
    )

    private fun response(
        place: String,
        days: List<Day>,
        stations: Map<String, Station>? = null
    ): WeatherResponse = WeatherResponse(
        resolvedAddress = place,
        address = place,
        queryCost = 1,
        latitude = 60.0,
        longitude = 25.0,
        timezone = "Europe/Helsinki",
        tzoffset = 2.0,
        days = days,
        stations = stations
    )

    // ── saveWeatherResponse: new place ──────────────────────────────────────────

    @Test
    fun saveWeatherResponse_newPlace_insertsAllDays() = runTest {
        database.saveWeatherResponse(
            response("Helsinki", listOf(day("2025-01-01"), day("2025-01-02"), day("2025-01-03")))
        )

        val loaded = database.getSavedPlaceFull("Helsinki")
        assertNotNull(loaded)
        assertEquals("Helsinki", loaded.resolvedAddress)
        assertEquals(3, loaded.days?.size)
        assertEquals(
            setOf("2025-01-01", "2025-01-02", "2025-01-03"),
            loaded.days?.map { it.datetime }?.toSet()
        )
    }

    @Test
    fun saveWeatherResponse_newPlace_persistsHours() = runTest {
        database.saveWeatherResponse(
            response(
                "Helsinki",
                listOf(day("2025-01-01", hours = listOf(hour("00:00:00"), hour("01:00:00"))))
            )
        )

        val loaded = database.getSavedPlaceFull("Helsinki")
        val theDay = loaded?.days?.single()
        assertEquals(2, theDay?.hours?.size)
        assertEquals(setOf("00:00:00", "01:00:00"), theDay?.hours?.map { it.datetime }?.toSet())
    }

    // ── saveWeatherResponse: existing place, accumulation ───────────────────────

    @Test
    fun saveWeatherResponse_existingPlaceAllNewDays_accumulates() = runTest {
        database.saveWeatherResponse(
            response("Helsinki", listOf(day("2025-01-01"), day("2025-01-02")))
        )
        database.saveWeatherResponse(
            response("Helsinki", listOf(day("2025-01-03"), day("2025-01-04")))
        )

        val loaded = database.getSavedPlaceFull("Helsinki")
        assertEquals(4, loaded?.days?.size, "All four days should accumulate with no loss")
        assertEquals(
            setOf("2025-01-01", "2025-01-02", "2025-01-03", "2025-01-04"),
            loaded?.days?.map { it.datetime }?.toSet()
        )
    }

    @Test
    fun saveWeatherResponse_overlappingDays_doesNotDuplicate() = runTest {
        database.saveWeatherResponse(
            response("Helsinki", listOf(day("2025-01-01"), day("2025-01-02")))
        )
        // Second save overlaps 01-02 and adds 01-03. Only the genuinely new day must be inserted.
        database.saveWeatherResponse(
            response("Helsinki", listOf(day("2025-01-02"), day("2025-01-03")))
        )

        val loaded = database.getSavedPlaceFull("Helsinki")
        val datetimes = loaded?.days?.map { it.datetime }
        assertEquals(3, datetimes?.size, "Overlapping day must not be duplicated")
        assertEquals(
            setOf("2025-01-01", "2025-01-02", "2025-01-03"),
            datetimes?.toSet()
        )
        assertEquals(1, datetimes?.count { it == "2025-01-02" }, "01-02 should appear exactly once")
    }

    // ── saveWeatherResponse: stations ───────────────────────────────────────────

    @Test
    fun saveWeatherResponse_withStations_persistsThem() = runTest {
        database.saveWeatherResponse(
            response(
                "Helsinki",
                listOf(day("2025-01-01")),
                stations = mapOf("EFHK" to station("EFHK", 60.3, 25.0))
            )
        )

        val stations = database.getStationsForPlace("Helsinki")
        assertEquals(1, stations.size)
        assertEquals("EFHK", stations.single().id)
    }

    @Test
    fun saveWeatherResponse_secondSaveWithStations_replacesNotMerges() = runTest {
        database.saveWeatherResponse(
            response(
                "Helsinki",
                listOf(day("2025-01-01")),
                stations = mapOf(
                    "OLD1" to station("OLD1", 60.0, 25.0),
                    "OLD2" to station("OLD2", 61.0, 26.0)
                )
            )
        )
        // A later response carrying a different station set must replace the previous set entirely.
        database.saveWeatherResponse(
            response(
                "Helsinki",
                listOf(day("2025-01-02")),
                stations = mapOf("NEW1" to station("NEW1", 62.0, 27.0))
            )
        )

        val stations = database.getStationsForPlace("Helsinki")
        assertEquals(1, stations.size, "Old stations should be deleted, not merged")
        assertEquals("NEW1", stations.single().id)
    }

    @Test
    fun saveWeatherResponse_noStationsInSecondSave_keepsExistingStations() = runTest {
        // Station replace only happens when the response actually carries stations
        // (day-range downloads carry none and must not wipe the persisted set).
        database.saveWeatherResponse(
            response(
                "Helsinki",
                listOf(day("2025-01-01")),
                stations = mapOf("EFHK" to station("EFHK", 60.3, 25.0))
            )
        )
        database.saveWeatherResponse(
            response("Helsinki", listOf(day("2025-01-02")), stations = null)
        )

        val stations = database.getStationsForPlace("Helsinki")
        assertEquals(1, stations.size, "A station-less save must not delete existing stations")
        assertEquals("EFHK", stations.single().id)
    }

    // ── getSavedPlaceFull round-trip with stations ──────────────────────────────

    @Test
    fun getSavedPlaceFull_reassemblesStationsIntoMap() = runTest {
        database.saveWeatherResponse(
            response(
                "Helsinki",
                listOf(day("2025-01-01")),
                stations = mapOf(
                    "EFHK" to station("EFHK", 60.3, 25.0),
                    "EFTU" to station("EFTU", 60.5, 22.3)
                )
            )
        )

        val loaded = database.getSavedPlaceFull("Helsinki")
        assertNotNull(loaded?.stations)
        assertEquals(2, loaded.stations.size)
        assertTrue(loaded.stations.containsKey("EFHK"))
        assertTrue(loaded.stations.containsKey("EFTU"))
    }

    @Test
    fun getSavedPlaceFull_unknownPlace_returnsNull() = runTest {
        assertNull(database.getSavedPlaceFull("Nowhere"))
    }

    // ── getPlaceDayCounts ───────────────────────────────────────────────────────

    @Test
    fun getPlaceDayCounts_returnsCountPerPlace() = runTest {
        database.saveWeatherResponse(
            response("Helsinki", listOf(day("2025-01-01"), day("2025-01-02")))
        )
        database.saveWeatherResponse(
            response("Tampere", listOf(day("2025-01-01")))
        )

        val counts = database.getPlaceDayCounts()
        assertEquals(2L, counts["Helsinki"])
        assertEquals(1L, counts["Tampere"])
    }

    // ── getWeatherDataFor ───────────────────────────────────────────────────────

    @Test
    fun getWeatherDataFor_existingDate_returnsThatDayOnly() = runTest {
        database.saveWeatherResponse(
            response("Helsinki", listOf(day("2025-01-01"), day("2025-01-02")))
        )

        val loaded = database.getWeatherDataFor("Helsinki", "2025-01-02")
        assertNotNull(loaded)
        assertEquals(1, loaded.days?.size)
        assertEquals("2025-01-02", loaded.days?.single()?.datetime)
    }

    @Test
    fun getWeatherDataFor_missingDate_returnsNull() = runTest {
        database.saveWeatherResponse(
            response("Helsinki", listOf(day("2025-01-01")))
        )
        assertNull(database.getWeatherDataFor("Helsinki", "2099-12-31"))
    }

    // ── deletePlace ─────────────────────────────────────────────────────────────

    @Test
    fun deletePlace_removesPlaceAndDays() = runTest {
        database.saveWeatherResponse(
            response("Helsinki", listOf(day("2025-01-01")))
        )
        database.saveWeatherResponse(
            response("Tampere", listOf(day("2025-01-01")))
        )

        database.deletePlace("Helsinki")

        assertNull(database.getSavedPlaceFull("Helsinki"))
        assertNotNull(database.getSavedPlaceFull("Tampere"))
        assertEquals(listOf("Tampere"), database.getAllSavedPlaces())
    }

    // ── cleanupForecastDays ─────────────────────────────────────────────────────

    @Test
    fun cleanupForecastDays_removesFutureDaysKeepsPast() = runTest {
        // One clearly-past day and one far-future day. cleanup deletes days after yesterday.
        database.saveWeatherResponse(
            response("Helsinki", listOf(day("2000-01-01"), day("2999-12-31")))
        )

        database.cleanupForecastDays()

        val loaded = database.getSavedPlaceFull("Helsinki")
        val datetimes = loaded?.days?.map { it.datetime }
        assertTrue(datetimes?.contains("2000-01-01") == true, "Past day should be kept")
        assertTrue(datetimes?.contains("2999-12-31") == false, "Future day should be removed")
    }
}
