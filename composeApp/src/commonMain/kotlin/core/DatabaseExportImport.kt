package core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Platform-specific database export/import manager
 */
expect class DatabaseExportImport {
    /**
     * Export the current database to an accessible location
     * @return Path to exported file or error message
     */
    suspend fun exportDatabase(): Result<String>

    /**
     * Import a database from a file
     * @param filePath Path to the .db file to import
     * @return Success message or error message
     */
    suspend fun importDatabase(filePath: String): Result<String>

    /**
     * Get list of available backup files
     */
    suspend fun listBackups(): Result<List<String>>
}

/**
 * Result wrapper for export/import operations
 */
sealed class ExportImportResult {
    data class Success(val message: String) : ExportImportResult()
    data class Error(val message: String) : ExportImportResult()
}
