package core

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.km.rewinds.db.AppDatabase
import com.km.rewinds.db.Day as DayAdapterClass
import com.km.rewinds.db.Hour as HourAdapterClass
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies the schema v6 -> v7 migration (KIM-364) that adds `WeatherResponse.archivedAt`.
 *
 * The critical AC is "existing rows migrate with archivedAt = NULL" — i.e. all currently-saved
 * places stay visible with no behaviour change. This test simulates a pre-migration database by
 * creating the schema only up to version 6, seeding a place with a stored day, then running the
 * real generated migration to version 7 and asserting the row survives and is active.
 *
 * Lives in androidUnitTest because the JDBC/in-memory driver is JVM-only.
 */
class ArchiveMigrationTest {

    @Test
    fun migrationV6ToV7_preservesExistingPlaceAsActive() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            // Recreate the WeatherResponse/Day tables exactly as they existed at schema version 6
            // (WeatherResponse has NO archivedAt column yet), then seed a place with a stored day —
            // this stands in for a database written by a pre-KIM-364 build.
            driver.execute(
                null,
                """CREATE TABLE WeatherResponse (
                       resolvedAddress TEXT NOT NULL PRIMARY KEY,
                       queryCost INTEGER,
                       latitude REAL NOT NULL,
                       longitude REAL NOT NULL,
                       address TEXT,
                       timezone TEXT,
                       tzoffset REAL
                   )""",
                0
            )
            driver.execute(
                null,
                """CREATE TABLE Day (
                       id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                       weatherResponseResolvedAddress TEXT NOT NULL,
                       datetime TEXT NOT NULL,
                       datetimeEpoch INTEGER NOT NULL,
                       humidity REAL,
                       conditions TEXT,
                       FOREIGN KEY(weatherResponseResolvedAddress) REFERENCES WeatherResponse(resolvedAddress) ON DELETE CASCADE
                   )""",
                0
            )
            driver.execute(
                null,
                """INSERT INTO WeatherResponse(resolvedAddress, queryCost, latitude, longitude, address, timezone, tzoffset)
                   VALUES ('Helsinki', 1, 60.0, 25.0, 'Helsinki', 'Europe/Helsinki', 2.0)""",
                0
            )
            driver.execute(
                null,
                """INSERT INTO Day(weatherResponseResolvedAddress, datetime, datetimeEpoch, humidity, conditions)
                   VALUES ('Helsinki', '2025-01-01', 0, 60.0, 'Clear')""",
                0
            )

            // Run the real generated migration from v6 to the current v7. Only the
            // `oldVersion <= 6` block (ADD COLUMN archivedAt) applies to our hand-built tables.
            AppDatabase.Schema.migrate(driver, oldVersion = 6, newVersion = 7).await()

            // Query through the production database layer (queries that only touch the columns our
            // hand-built tables have) and confirm the row survived and is active.
            val database: Database = SqlDelightDatabase(
                AppDatabase(
                    driver = driver,
                    DayAdapter = DayAdapterClass.Adapter(preciptypeAdapter = listOfStringAdapter),
                    HourAdapter = HourAdapterClass.Adapter(preciptypeAdapter = listOfStringAdapter)
                )
            )

            assertEquals(
                listOf("Helsinki"),
                database.getAllSavedPlaces(),
                "existing place must remain visible after migration (archivedAt defaulted NULL)"
            )
            assertEquals(
                PlaceArchiveState.ACTIVE,
                database.getArchiveState("Helsinki"),
                "migrated row must have archivedAt = NULL, i.e. active"
            )

            // The pre-existing Day row must survive the migration untouched. getPlaceDayCounts is a
            // pure COUNT(*) GROUP BY that touches no adapter columns, so it works against our
            // minimal Day table.
            assertEquals(
                1L,
                database.getPlaceDayCounts()["Helsinki"],
                "existing day must survive the migration"
            )
        } finally {
            driver.close()
        }
    }
}
