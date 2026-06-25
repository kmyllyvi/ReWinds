package core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the iOS [DatabaseExportImport] against the real simulator filesystem (NSFileManager).
 * Each test cleans the app.db and ReWinds backup directory so runs are order-independent and don't
 * leak state into the live app sandbox used by other tests.
 */
@OptIn(ExperimentalForeignApi::class)
class DatabaseExportImportTest {

    private val fileManager = NSFileManager.defaultManager

    private fun documentsDir(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        ) as? List<*>
        return (paths?.firstOrNull() as? String) ?: ""
    }

    private fun appDbPath(): String = (documentsDir() as NSString).stringByAppendingPathComponent("app.db")

    private fun backupDir(): String = (documentsDir() as NSString).stringByAppendingPathComponent("ReWinds")

    private fun joinPath(dir: String, name: String): String =
        (dir as NSString).stringByAppendingPathComponent(name)

    /** Writes [contents] to [path], creating a real on-disk file. */
    private fun writeFile(path: String, contents: String) {
        (contents as NSString).writeToFile(path, atomically = true)
    }

    private fun cleanup() {
        if (fileManager.fileExistsAtPath(appDbPath())) {
            fileManager.removeItemAtPath(appDbPath(), error = null)
        }
        if (fileManager.fileExistsAtPath(backupDir())) {
            fileManager.removeItemAtPath(backupDir(), error = null)
        }
    }

    @BeforeTest
    fun setUp() = cleanup()

    @AfterTest
    fun tearDown() = cleanup()

    @Test
    fun importDatabase_copiesSourceOverAppDb(): Unit = runBlocking {
        val sourcePath = joinPath(documentsDir(), "import_source.db")
        writeFile(sourcePath, "SQLite payload")

        val result = DatabaseExportImport().importDatabase(sourcePath)

        assertTrue(result.isSuccess, "import of an existing file should succeed")
        assertTrue(fileManager.fileExistsAtPath(appDbPath()), "app.db should exist after import")

        fileManager.removeItemAtPath(sourcePath, error = null)
    }

    @Test
    fun importDatabase_missingSource_returnsFailure() = runBlocking {
        val missing = joinPath(documentsDir(), "does_not_exist.db")

        val result = DatabaseExportImport().importDatabase(missing)

        assertTrue(result.isFailure, "import of a missing source file should fail")
    }

    @Test
    fun exportDatabase_missingDb_returnsFailure() = runBlocking {
        // No app.db present (cleaned in setUp).
        val result = DatabaseExportImport().exportDatabase()

        assertTrue(result.isFailure, "export should fail when app.db does not exist")
    }

    @Test
    fun exportDatabase_writesTimestampedBackup() = runBlocking {
        writeFile(appDbPath(), "SQLite payload")

        val result = DatabaseExportImport().exportDatabase()

        assertTrue(result.isSuccess, "export should succeed when app.db exists")
        val entries = fileManager.contentsOfDirectoryAtPath(backupDir(), error = null)
            ?.mapNotNull { it as? String } ?: emptyList()
        assertEquals(1, entries.size, "exactly one backup should be written")
        assertTrue(
            entries.single().startsWith("rewinds_backup_") && entries.single().endsWith(".db"),
            "backup name should follow the rewinds_backup_<ts>.db convention"
        )
    }

    @Test
    fun listBackups_missingDir_returnsEmpty() = runBlocking {
        val result = DatabaseExportImport().listBackups()

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrNull())
    }

    @Test
    fun listBackups_returnsOnlyBackupFiles() = runBlocking {
        val dao = DatabaseExportImport()
        writeFile(appDbPath(), "SQLite payload")
        dao.exportDatabase()

        // A non-backup file in the same dir must be ignored.
        writeFile(joinPath(backupDir(), "unrelated.txt"), "noise")

        val result = dao.listBackups()

        assertTrue(result.isSuccess)
        val backups = result.getOrNull().orEmpty()
        assertEquals(1, backups.size, "only the rewinds_backup_*.db file should be listed")
        assertTrue(backups.single().endsWith(".db"))
    }
}
