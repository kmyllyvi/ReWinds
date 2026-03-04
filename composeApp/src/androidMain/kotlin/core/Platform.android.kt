package core

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.km.rewinds.BuildConfig
import com.km.rewinds.db.AppDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun httpClient(enableNetworkLogs: Boolean): HttpClient {
    return HttpClient(OkHttp) {
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

actual open class DatabaseDriverFactory(private val context: Context) {
    actual open fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(AppDatabase.Schema, context, "app.db")
    }
}

actual fun isAndroid(): Boolean = true
actual fun isIOS(): Boolean = false

actual fun getAnthropicApiKey(): String {
    // Try to get from BuildConfig (set at build time from gradle.properties)
    val apiKey = BuildConfig.ANTHROPIC_API_KEY
    if (apiKey.isNotBlank() && !apiKey.contains("placeholder")) {
        return apiKey
    }

    // Development fallback: return a placeholder key
    // NOTE: This will fail at runtime when calling Anthropic API unless a real key is set
    // To use the chat feature, set ANTHROPIC_API_KEY in gradle.properties
    Log.d("Platform: ANTHROPIC_API_KEY not configured, using placeholder for development")
    return "sk-placeholder-dev-key-not-configured"
}

actual fun saveApiKeyPlatform(key: String) {
    // No-op on Android: uses BuildConfig at build time
    // If we wanted to support runtime key saving on Android, we could use SharedPreferences
    Log.d("Platform: saveApiKeyPlatform is no-op on Android (use gradle.properties)")
}

actual fun deleteApiKeyPlatform() {
    // No-op on Android
    Log.d("Platform: deleteApiKeyPlatform is no-op on Android")
}
