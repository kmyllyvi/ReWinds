package core

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.km.rewinds.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * KIM-321: DB-backed tests for getDownloadedMonths / observeDownloadedMonths against a real
 * in-memory SQLDelight database (JDBC driver, JVM-only — hence androidUnitTest, not commonTest).
 */
class DownloadedMonthsDbTest {

    private fun newDatabase(): SqlDelightDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val appDb = AppDatabase(
            driver = driver,
            DayAdapter = com.km.rewinds.db.Day.Adapter(preciptypeAdapter = listOfStringAdapter),
            HourAdapter = com.km.rewinds.db.Hour.Adapter(preciptypeAdapter = listOfStringAdapter)
        )
        return SqlDelightDatabase(appDb)
    }

    private fun responseFor(place: String, dates: List<String>): WeatherResponse =
        WeatherResponse(
            resolvedAddress = place,
            address = place,
            latitude = 36.0,
            longitude = -5.0,
            timezone = "UTC",
            tzoffset = 0.0,
            days = dates.map { date ->
                Day(
                    datetime = date,
                    datetimeEpoch = 0L,
                    tempmax = null, tempmin = null, temp = null,
                    feelslikemax = null, feelslikemin = null, feelslike = null,
                    dew = null, humidity = null, precip = null, precipprob = null,
                    precipcover = null, preciptype = null, snow = null, snowdepth = null,
                    windgust = null, windspeed = null, winddir = null, pressure = null,
                    cloudcover = null, visibility = null, solarradiation = null,
                    solarenergy = null, uvindex = null, sunrise = null, sunriseEpoch = null,
                    sunset = null, sunsetEpoch = null, moonphase = null, conditions = null,
                    description = null, icon = null, stations = null, source = null
                )
            }
        )

    @Test
    fun getDownloadedMonths_emptyWhenNoDays() = runTest {
        val db = newDatabase()
        assertTrue(db.getDownloadedMonths("Tarifa").isEmpty())
    }

    @Test
    fun getDownloadedMonths_returnsMonthsAcrossMultipleMonths() = runTest {
        val db = newDatabase()
        db.saveWeatherResponse(responseFor("Tarifa", listOf("2025-10-05", "2025-10-20", "2025-11-02", "2025-12-31")))
        assertEquals(setOf("2025-10", "2025-11", "2025-12"), db.getDownloadedMonths("Tarifa"))
    }

    @Test
    fun getDownloadedMonths_ignoresOtherPlaces() = runTest {
        val db = newDatabase()
        db.saveWeatherResponse(responseFor("Tarifa", listOf("2025-10-05")))
        db.saveWeatherResponse(responseFor("Maui", listOf("2024-01-15", "2024-02-15")))
        assertEquals(setOf("2025-10"), db.getDownloadedMonths("Tarifa"))
        assertEquals(setOf("2024-01", "2024-02"), db.getDownloadedMonths("Maui"))
    }

    @Test
    fun observeDownloadedMonths_emitsAfterDayInsert() = runTest {
        val db = newDatabase()
        // Seed the place row with one month so the Flow has a baseline.
        db.saveWeatherResponse(responseFor("Tarifa", listOf("2025-10-05")))
        assertEquals(setOf("2025-10"), db.observeDownloadedMonths("Tarifa").first())

        // Insert a Day in a new month; the reactive Flow must reflect it.
        db.saveWeatherResponse(responseFor("Tarifa", listOf("2025-11-05")))
        assertEquals(setOf("2025-10", "2025-11"), db.observeDownloadedMonths("Tarifa").first())
    }
}
