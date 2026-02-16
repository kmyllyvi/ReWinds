package core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSString
import platform.Foundation.stringByAppendingPathComponent

/**
 * iOS implementation of database export/import
 * Note: For now, this is a basic implementation that works around Kotlin/Native NSFileManager complexities
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
     * Placeholder: The actual import would require file picker and database connection management
     */
    actual suspend fun importDatabase(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        Result.success("Import placeholder - replace app.db at ${getDatabasePath()} with selected file")
    }

    /**
     * List available backup files
     */
    actual suspend fun listBackups(): Result<List<String>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }
}
