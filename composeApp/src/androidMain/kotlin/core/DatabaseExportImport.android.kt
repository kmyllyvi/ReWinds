package core

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import java.io.File

// Global context reference for Android
private lateinit var appContext: Context

/**
 * Initialize Android database export/import with application context
 */
fun initializeDatabaseExportImport(context: Context) {
    appContext = context.applicationContext
}

/**
 * Android implementation of database export/import
 */
actual class DatabaseExportImport {

    private val context: Context
        get() = appContext

    /**
     * Export database to Documents folder
     */
    actual suspend fun exportDatabase(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Get the database file
            val dbFile = context.getDatabasePath("app.db")

            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("Database file not found"))
            }

            // Create destination directory in Documents
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ReWinds")
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            // Create backup file with timestamp
            val timestamp = System.currentTimeMillis()
            val backupFile = File(documentsDir, "rewinds_backup_$timestamp.db")

            // Copy database file
            dbFile.copyTo(backupFile, overwrite = true)

            val message = "Database exported to: ${backupFile.absolutePath}"
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import database from file (placeholder for Android - actual import on iOS)
     */
    actual suspend fun importDatabase(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        Result.failure(Exception("Import not implemented on Android - use iOS"))
    }

    /**
     * List available backup files
     */
    actual suspend fun listBackups(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ReWinds")

            if (!documentsDir.exists()) {
                return@withContext Result.success(emptyList())
            }

            val backups = documentsDir.listFiles { file ->
                file.isFile && file.name.startsWith("rewinds_backup_") && file.name.endsWith(".db")
            }?.map { it.absolutePath } ?: emptyList()

            Result.success(backups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
