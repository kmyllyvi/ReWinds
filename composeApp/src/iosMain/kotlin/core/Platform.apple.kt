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
import core.ApiKeyManager
import core.KeychainBridge

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
    val key = ApiKeyManager.getApiKey()
    if (key.isEmpty()) {
        Log.d("Platform: Using placeholder API key for development on iOS")
        return "sk-placeholder-dev-key-not-configured"
    }
    return key
}

actual fun saveApiKeyPlatform(key: String) {
    ApiKeyManager.setApiKey(key)
    KeychainBridge.saveKey(key)
    Log.d("Platform: API key saved to Keychain")
}

actual fun deleteApiKeyPlatform() {
    ApiKeyManager.setApiKey("")
    KeychainBridge.deleteKey()
    Log.d("Platform: API key deleted from Keychain")
}

actual fun getVisualCrossingApiKey(): String {
    val key = WeatherApiKeyManager.getApiKey()
    if (key.isEmpty()) {
        Log.d("Platform: Using placeholder Weather API key for development on iOS")
        return "placeholder-weather-key-not-configured"
    }
    return key
}

actual fun saveWeatherApiKeyPlatform(key: String) {
    WeatherApiKeyManager.setApiKey(key)
    WeatherKeychainBridge.saveKey(key)
    Log.d("Platform: Weather API key saved to Keychain")
}

actual fun deleteWeatherApiKeyPlatform() {
    WeatherApiKeyManager.setApiKey("")
    WeatherKeychainBridge.deleteKey()
    Log.d("Platform: Weather API key deleted from Keychain")
}
