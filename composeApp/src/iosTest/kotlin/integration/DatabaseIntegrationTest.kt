package integration

import core.DatabaseDriverFactory
import core.createDatabase
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseIntegrationTest {
    @Test
    fun testDatabaseCreationSucceeds() {
        val factory = DatabaseDriverFactory()
        val database = createDatabase(factory)
        assertNotNull(database, "Database should be created successfully")
    }

    @Test
    fun testDatabaseQueryOperations() {
        val factory = DatabaseDriverFactory()
        val database = createDatabase(factory)
        val dbQuery = database.appDatabaseQueries

        // Verify we can get all weather responses (should start empty)
        val responses = dbQuery.selectAllWeatherResponses().executeAsList()
        assertTrue(responses.isEmpty(), "New database should have no weather responses")
    }

    @Test
    fun testTransactionSupport() {
        val factory = DatabaseDriverFactory()
        val database = createDatabase(factory)

        // Verify database supports transactions
        database.transaction {
            // Empty transaction just to verify support
        }

        // If we get here without error, transactions work
        assertTrue(true, "Database supports transactions")
    }
}
