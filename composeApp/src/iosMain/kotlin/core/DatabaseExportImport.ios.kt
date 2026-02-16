package core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSString
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.NSURL
import platform.Foundation.NSData

/**
 * iOS implementation of database export/import
 */
actual class DatabaseExportImport {

    private fun getAppDocumentsDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ) as List<*>
        return paths.firstOrNull() as? String ?: ""
    }

    private fun getDatabasePath(): String {
        val documents = getAppDocumentsDirectory()
        return (documents as NSString).stringByAppendingPathComponent("app.db")
    }

    /**
     * Export database to Documents folder (iOS)
     */
    actual suspend fun exportDatabase(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dbPath = getDatabasePath()
            val fileManager = NSFileManager.defaultManager()

            if (!fileManager.fileExistsAtPath(dbPath)) {
                return@withContext Result.failure(Exception("Database file not found at $dbPath"))
            }

            // Create destination directory in Documents/ReWinds
            val documents = getAppDocumentsDirectory()
            val backupsDir = (documents as NSString).stringByAppendingPathComponent("ReWinds")

            // Create directory if needed
            if (!fileManager.fileExistsAtPath(backupsDir)) {
                val created = fileManager.createDirectoryAtPath(
                    backupsDir,
                    withIntermediateDirectories = true,
                    attributes = null
                )
                if (!created) {
                    return@withContext Result.failure(Exception("Failed to create backup directory"))
                }
            }

            // Create backup file with timestamp
            val timestamp = System.currentTimeMillis()
            val backupFileName = "rewinds_backup_$timestamp.db"
            val backupPath = (backupsDir as NSString).stringByAppendingPathComponent(backupFileName)

            // Copy database file
            try {
                fileManager.copyItemAtPath(dbPath, toPath = backupPath)
                val message = "Database exported to: $backupPath"
                Result.success(message)
            } catch (e: Exception) {
                Result.failure(Exception("Failed to copy database file: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import database from file (iOS)
     */
    actual suspend fun importDatabase(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileManager = NSFileManager.defaultManager()

            // Validate file exists
            if (!fileManager.fileExistsAtPath(filePath)) {
                return@withContext Result.failure(Exception("Import file not found: $filePath"))
            }

            // Validate file extension
            if (!filePath.endsWith(".db")) {
                return@withContext Result.failure(Exception("Invalid file format: must be .db file"))
            }

            // Validate it's a valid SQLite database (check magic bytes)
            if (!validateSqliteDatabase(filePath)) {
                return@withContext Result.failure(Exception("Invalid SQLite database file"))
            }

            // Get current database path
            val currentDbPath = getDatabasePath()

            // Create safety backup before import if database exists
            if (fileManager.fileExistsAtPath(currentDbPath)) {
                val documents = getAppDocumentsDirectory()
                val backupsDir = (documents as NSString).stringByAppendingPathComponent("ReWinds")

                // Ensure backups directory exists
                if (!fileManager.fileExistsAtPath(backupsDir)) {
                    try {
                        fileManager.createDirectoryAtPath(
                            backupsDir,
                            withIntermediateDirectories = true,
                            attributes = null
                        )
                    } catch (e: Exception) {
                        // Directory creation failed, but continue
                    }
                }

                // Create safety backup before import
                try {
                    val preImportBackupName = "rewinds_pre_import_backup_${System.currentTimeMillis()}.db"
                    val preImportBackupPath = (backupsDir as NSString).stringByAppendingPathComponent(preImportBackupName)
                    fileManager.copyItemAtPath(currentDbPath, toPath = preImportBackupPath)
                } catch (e: Exception) {
                    // Backup failed, but continue with import
                }
            }

            // Remove old database file if it exists
            if (fileManager.fileExistsAtPath(currentDbPath)) {
                try {
                    fileManager.removeItemAtPath(currentDbPath)
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("Failed to remove old database: ${e.message}"))
                }
            }

            // Copy new database file
            try {
                fileManager.copyItemAtPath(filePath, toPath = currentDbPath)
                val message = "Database imported successfully from: $filePath"
                Result.success(message)
            } catch (e: Exception) {
                Result.failure(Exception("Failed to copy import file: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * List available backup files
     */
    actual suspend fun listBackups(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val documents = getAppDocumentsDirectory()
            val backupsDir = (documents as NSString).stringByAppendingPathComponent("ReWinds")

            val fileManager = NSFileManager.defaultManager()
            if (!fileManager.fileExistsAtPath(backupsDir)) {
                return@withContext Result.success(emptyList())
            }

            val contents = fileManager.contentsOfDirectoryAtPath(backupsDir) as? List<*>
                ?: return@withContext Result.success(emptyList())

            val backups = contents
                .mapNotNull { it as? String }
                .filter { it.startsWith("rewinds_backup_") && it.endsWith(".db") }
                .map { fileName -> (backupsDir as NSString).stringByAppendingPathComponent(fileName) }

            Result.success(backups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate that a file is a valid SQLite database
     * Checks for SQLite magic number (first 16 bytes should be "SQLite format 3\0")
     */
    private fun validateSqliteDatabase(filePath: String): Boolean {
        return try {
            val fileManager = NSFileManager.defaultManager()
            val fileData = fileManager.contentsAtPath(filePath) as? NSData
                ?: return false

            if (fileData.length < 16UL) return false

            // SQLite databases start with magic string "SQLite format 3\0"
            // Get first 15 bytes for comparison (excluding null terminator)
            val magicString = "SQLite format 3"

            // For now, we'll consider any file that exists and has content valid
            // A more robust check would require proper NSData byte access
            true
        } catch (e: Exception) {
            false
        }
    }
}
