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
    // First check if key was set at runtime (via ApiKeyManager)
    val runtimeKey = ApiKeyManager.getApiKey()
    if (runtimeKey.isNotBlank() && runtimeKey.startsWith("sk-ant-")) {
        Log.d("Platform: Using API key from ApiKeyManager")
        return runtimeKey
    }

    // Otherwise use placeholder (will be loaded from Keychain at app startup)
    Log.d("Platform: Using placeholder API key for development on iOS")
    return "sk-placeholder-dev-key-not-configured"
}

actual fun saveApiKeyPlatform(key: String) {
    // Save to ApiKeyManager for immediate use
    ApiKeyManager.setApiKey(key)
    // Then save to Keychain via Swift callback
    KeychainBridge.saveKey(key)
    Log.d("Platform: API key saved to Keychain")
}

actual fun deleteApiKeyPlatform() {
    // Clear from ApiKeyManager
    ApiKeyManager.setApiKey("")
    // Clear from Keychain via Swift callback
    KeychainBridge.deleteKey()
    Log.d("Platform: API key deleted from Keychain")
}
