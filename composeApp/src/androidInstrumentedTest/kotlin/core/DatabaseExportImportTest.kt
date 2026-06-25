package core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Instrumented coverage for the Android [DatabaseExportImport] against a real [Context] and the
 * device filesystem. Runs on an emulator/device via `connectedDebugAndroidTest`.
 *
 * Each test seeds/cleans the app.db so runs are order-independent and don't disturb the live app db.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseExportImportTest {

    private lateinit var context: Context
    private lateinit var dao: DatabaseExportImport

    private fun appDbFile(): File = context.getDatabasePath("app.db")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        initializeDatabaseExportImport(context)
        dao = DatabaseExportImport()
        appDbFile().delete()
    }

    @After
    fun tearDown() {
        appDbFile().delete()
    }

    @Test
    fun exportDatabase_missingDb_returnsFailure() = runBlocking {
        // No app.db seeded.
        val result = dao.exportDatabase()
        assertTrue("export should fail when app.db is absent", result.isFailure)
    }

    @Test
    fun exportDatabase_withDb_succeedsAndAppearsInBackups() = runBlocking {
        val dbFile = appDbFile()
        dbFile.parentFile?.mkdirs()
        dbFile.writeText("SQLite payload")

        val export = dao.exportDatabase()
        assertTrue("export should succeed when app.db exists: ${export.exceptionOrNull()}", export.isSuccess)

        val list = dao.listBackups()
        assertTrue(list.isSuccess)
        val backups = list.getOrNull().orEmpty()
        assertTrue(
            "exported backup should be listed by listBackups(): $backups",
            backups.any { it.substringAfterLast('/').startsWith("rewinds_backup_") && it.endsWith(".db") }
        )
    }

    @Test
    fun importDatabase_copiesSourceOverAppDb() = runBlocking {
        val source = File(context.cacheDir, "import_source.db")
        source.writeText("imported payload")
        appDbFile().delete()

        val result = dao.importDatabase(source.absolutePath)

        assertTrue("import of an existing file should succeed: ${result.exceptionOrNull()}", result.isSuccess)
        assertTrue("app.db should exist after import", appDbFile().exists())
        assertTrue("app.db should contain the imported bytes", appDbFile().readText() == "imported payload")

        source.delete()
    }

    @Test
    fun importDatabase_missingSource_returnsFailure() = runBlocking {
        val missing = File(context.cacheDir, "does_not_exist.db").absolutePath

        val result = dao.importDatabase(missing)

        assertTrue("import of a missing source file should fail", result.isFailure)
        assertFalse("no app.db should be created from a missing source", appDbFile().exists())
    }
}
