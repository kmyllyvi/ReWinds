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

/**
 * iOS implementation of database export/import
 */
actual class DatabaseExportImport {

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

    /**
     * Export database to Documents folder (iOS)
     * Placeholder: The actual export would require file manager integration
     */
    actual suspend fun exportDatabase(): Result<String> = withContext(Dispatchers.IO) {
        Result.success("Export placeholder - copy app.db from ${getDatabasePath()} manually")
    }

    /**
     * Import database from file (iOS)
     * Copies the file at filePath to the app's database location (app.db)
     */
    @OptIn(ExperimentalForeignApi::class)
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
     * List available backup files
     */
    actual suspend fun listBackups(): Result<List<String>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }
}
