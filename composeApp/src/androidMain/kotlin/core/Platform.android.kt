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
    // 1. Runtime key set via Settings (highest priority — persisted across restarts)
    val runtimeKey = androidAppContext?.getSharedPreferences("rewinds_prefs", Context.MODE_PRIVATE)
        ?.getString("anthropic_api_key", null)
    if (!runtimeKey.isNullOrBlank() && !runtimeKey.contains("placeholder")) {
        return runtimeKey
    }

    // 2. Build-time key from gradle.properties
    val buildKey = BuildConfig.ANTHROPIC_API_KEY
    if (buildKey.isNotBlank() && !buildKey.contains("placeholder")) {
        return buildKey
    }

    Log.d("Platform: ANTHROPIC_API_KEY not configured, using placeholder for development")
    return "sk-placeholder-dev-key-not-configured"
}

actual fun saveApiKeyPlatform(key: String) {
    val ctx = androidAppContext ?: run {
        Log.d("Platform: androidAppContext not set - Anthropic API key not persisted")
        return
    }
    ctx.getSharedPreferences("rewinds_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("anthropic_api_key", key)
        .apply()
    Log.d("Platform: Anthropic API key saved to SharedPreferences")
}

actual fun deleteApiKeyPlatform() {
    androidAppContext?.getSharedPreferences("rewinds_prefs", Context.MODE_PRIVATE)
        ?.edit()
        ?.remove("anthropic_api_key")
        ?.apply()
    Log.d("Platform: Anthropic API key removed from SharedPreferences")
}

actual fun getVisualCrossingApiKey(): String {
    // 1. Runtime key set via Settings (highest priority)
    val runtimeKey = WeatherApiKeyManager.getApiKey()
    if (runtimeKey.isNotBlank() && !runtimeKey.contains("placeholder")) {
        return runtimeKey
    }
    // 2. Build-time key from gradle.properties
    val buildKey = BuildConfig.VISUAL_CROSSING_API_KEY
    if (buildKey.isNotBlank() && !buildKey.contains("placeholder")) {
        return buildKey
    }
    Log.d("Platform: VISUAL_CROSSING_API_KEY not configured, using placeholder for development")
    return "placeholder-weather-key-not-configured"
}

actual fun saveWeatherApiKeyPlatform(key: String) {
    WeatherApiKeyManager.setApiKey(key)
    val ctx = androidAppContext ?: run {
        Log.d("Platform: androidAppContext not set - weather API key not persisted")
        return
    }
    ctx.getSharedPreferences("rewinds_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("visual_crossing_api_key", key)
        .apply()
    Log.d("Platform: Visual Crossing API key saved to SharedPreferences")
}

actual fun deleteWeatherApiKeyPlatform() {
    WeatherApiKeyManager.setApiKey("")
    androidAppContext?.getSharedPreferences("rewinds_prefs", Context.MODE_PRIVATE)
        ?.edit()
        ?.remove("visual_crossing_api_key")
        ?.apply()
}

// Lazily obtained application context for SharedPreferences access.
// This is set indirectly via the Context stored by DatabaseExportImport initialization.
// We reuse the same pattern: a module-private lateinit var populated at startup.
private var androidAppContext: Context? = null

/**
 * Called from [initializeDatabaseExportImport] (or DI setup) to supply the app context
 * so language preferences can use SharedPreferences.
 */
fun provideAndroidContextForLanguage(context: Context) {
    androidAppContext = context.applicationContext
}

actual fun saveLanguagePreference(code: String) {
    val ctx = androidAppContext ?: run {
        Log.d("Platform: androidAppContext not set - language preference not saved")
        return
    }
    ctx.getSharedPreferences("rewinds_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("language_code", code)
        .apply()
}

actual fun loadLanguagePreference(): String? {
    val ctx = androidAppContext ?: return null
    return ctx.getSharedPreferences("rewinds_prefs", Context.MODE_PRIVATE)
        .getString("language_code", null)
}

fun loadWeatherApiKeyFromPreferences() {
    val ctx = androidAppContext ?: return
    val key = ctx.getSharedPreferences("rewinds_prefs", Context.MODE_PRIVATE)
        .getString("visual_crossing_api_key", null)
    if (!key.isNullOrBlank()) {
        WeatherApiKeyManager.setApiKey(key)
        Log.d("Platform: Visual Crossing API key loaded from SharedPreferences")
    }
}
