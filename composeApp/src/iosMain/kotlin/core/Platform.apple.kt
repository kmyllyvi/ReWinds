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

actual fun httpClient(enableNetworkLogs: Boolean): HttpClient {
    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true  // Important: serialize fields even if they have default values
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
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(AppDatabase.Schema, "app.db")
    }
}

actual fun isAndroid(): Boolean = false
actual fun isIOS(): Boolean = true

actual fun getAnthropicApiKey(): String {
    // For iOS development: Using placeholder key
    // Keychain integration planned for future release
    Log.d("Platform: Using placeholder API key for development on iOS")
    return "sk-placeholder-dev-key-not-configured"
}

actual fun saveApiKeyPlatform(key: String) {
    // TODO: Implement Keychain save when framework bindings are stable
    Log.d("Platform: saveApiKeyPlatform not yet implemented on iOS")
}

actual fun deleteApiKeyPlatform() {
    // TODO: Implement Keychain delete when framework bindings are stable
    Log.d("Platform: deleteApiKeyPlatform not yet implemented on iOS")
}
