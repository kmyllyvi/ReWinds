package core

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.km.rewinds.db.AppDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import kotlinx.cinterop.ExperimentalForeignApi

actual fun httpClient(enableNetworkLogs: Boolean): HttpClient {
    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        if (enableNetworkLogs) {
            install(Logging) {
                level = LogLevel.ALL
            }
        }
    }
}

actual class DatabaseDriverFactory {
    @OptIn(ExperimentalForeignApi::class)
    actual fun createDriver(): SqlDriver {
        // Get absolute path to Documents directory for consistent database location
        // This ensures the database is stored in the same location as the import function
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ) as? List<*>
        val documentsPath = (paths?.firstOrNull() as? String) ?: ""
        val databasePath = "$documentsPath/app.db"

        return NativeSqliteDriver(AppDatabase.Schema, databasePath)
    }
}

actual fun isAndroid(): Boolean = false
actual fun isIOS(): Boolean = true
