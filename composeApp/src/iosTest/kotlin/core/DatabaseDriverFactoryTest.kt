package core

import kotlin.test.Test
import kotlin.test.assertNotNull

class DatabaseDriverFactoryTest {
    @Test
    fun testDriverCreationSucceeds() {
        val factory = DatabaseDriverFactory()
        val driver = factory.createDriver()
        assertNotNull(driver, "DatabaseDriverFactory should create a non-null driver")
        driver.close()
    }
}
