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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        assertTrue(result.isFailure, "export should fail when app.db is absent")
    }

    @Test
    fun exportDatabase_withDb_succeedsAndAppearsInBackups() = runBlocking {
        val dbFile = appDbFile()
        dbFile.parentFile?.mkdirs()
        dbFile.writeText("SQLite payload")

        val export = dao.exportDatabase()
        assertTrue(export.isSuccess, "export should succeed when app.db exists: ${export.exceptionOrNull()}")

        val list = dao.listBackups()
        assertTrue(list.isSuccess)
        val backups = list.getOrNull().orEmpty()
        assertTrue(
            backups.any { it.substringAfterLast('/').startsWith("rewinds_backup_") && it.endsWith(".db") },
            "exported backup should be listed by listBackups(): $backups"
        )
    }

    @Test
    fun importDatabase_copiesSourceOverAppDb() = runBlocking {
        val source = File(context.cacheDir, "import_source.db")
        source.writeText("imported payload")
        appDbFile().delete()

        val result = dao.importDatabase(source.absolutePath)

        assertTrue(result.isSuccess, "import of an existing file should succeed: ${result.exceptionOrNull()}")
        assertTrue(appDbFile().exists(), "app.db should exist after import")
        assertTrue(appDbFile().readText() == "imported payload", "app.db should contain the imported bytes")

        source.delete()
    }

    @Test
    fun importDatabase_missingSource_returnsFailure() = runBlocking {
        val missing = File(context.cacheDir, "does_not_exist.db").absolutePath

        val result = dao.importDatabase(missing)

        assertTrue(result.isFailure, "import of a missing source file should fail")
        assertFalse(appDbFile().exists(), "no app.db should be created from a missing source")
    }
}
