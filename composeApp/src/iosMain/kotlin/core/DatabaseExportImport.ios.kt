package core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSString
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.NSFileManager
import platform.Foundation.NSError
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation of database export/import
 */
@OptIn(ExperimentalForeignApi::class)
actual class DatabaseExportImport {

    private val backupPrefix = "rewinds_backup_"
    private val backupSuffix = ".db"

    private fun getAppDocumentsDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ) as? List<*>
        return (paths?.firstOrNull() as? String) ?: ""
    }

    private fun getDatabasePath(): String {
        val documents = getAppDocumentsDirectory()
        val docString = documents as NSString
        return docString.stringByAppendingPathComponent("app.db")
    }

    /** Backup directory ("<documents>/ReWinds"), mirroring the Android layout. */
    private fun getBackupDirectory(): String {
        val documents = getAppDocumentsDirectory() as NSString
        return documents.stringByAppendingPathComponent("ReWinds")
    }

    /**
     * Export the database to a timestamped backup file in the ReWinds documents folder.
     * Returns failure if the source database does not exist yet.
     */
    actual suspend fun exportDatabase(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileManager = NSFileManager.defaultManager
            val sourcePath = getDatabasePath()

            if (!fileManager.fileExistsAtPath(sourcePath)) {
                return@withContext Result.failure(Exception("Database file not found"))
            }

            val backupDir = getBackupDirectory()
            if (!fileManager.fileExistsAtPath(backupDir)) {
                fileManager.createDirectoryAtPath(
                    backupDir,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
            }

            // Whole-millisecond timestamp, matching the Android backup naming.
            val timestamp = (NSDate().timeIntervalSince1970 * 1000).toLong()
            val backupPath = (backupDir as NSString)
                .stringByAppendingPathComponent("$backupPrefix$timestamp$backupSuffix")

            val copySuccess = fileManager.copyItemAtPath(sourcePath, toPath = backupPath, error = null)
            if (copySuccess) {
                Result.success("Database exported to: $backupPath")
            } else {
                Result.failure(Exception("Failed to copy database file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import database from file (iOS)
     * Copies the file at filePath to the app's database location (app.db)
     */
    actual suspend fun importDatabase(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileManager = NSFileManager.defaultManager
            val targetPath = getDatabasePath()
            val error: NSError? = null

            // Check if source file exists
            if (!fileManager.fileExistsAtPath(filePath)) {
                return@withContext Result.failure(Exception("Source file not found: $filePath"))
            }

            // Remove existing database file if it exists
            if (fileManager.fileExistsAtPath(targetPath)) {
                fileManager.removeItemAtPath(targetPath, error = null)
            }

            // Copy file to database location
            val copySuccess = fileManager.copyItemAtPath(filePath, toPath = targetPath, error = null)

            return@withContext if (copySuccess) {
                Result.success("Database imported successfully from $filePath")
            } else {
                Result.failure(Exception("Failed to copy database file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * List available backup files in the ReWinds documents folder.
     * Returns an empty list if the backup directory does not exist yet.
     */
    actual suspend fun listBackups(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val fileManager = NSFileManager.defaultManager
            val backupDir = getBackupDirectory()

            if (!fileManager.fileExistsAtPath(backupDir)) {
                return@withContext Result.success(emptyList())
            }

            val entries = fileManager.contentsOfDirectoryAtPath(backupDir, error = null) ?: emptyList<Any?>()
            val backupDirString = backupDir as NSString
            val backups = entries
                .mapNotNull { it as? String }
                .filter { it.startsWith(backupPrefix) && it.endsWith(backupSuffix) }
                .map { backupDirString.stringByAppendingPathComponent(it) }

            Result.success(backups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
