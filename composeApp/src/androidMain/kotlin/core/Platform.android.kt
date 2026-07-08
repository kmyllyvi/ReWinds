package core

import android.content.Context
import android.content.Intent
import android.net.Uri
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

actual fun sendEmail(recipient: String, subject: String, body: String) {
    val ctx = androidAppContext ?: run {
        Log.d("Platform: androidAppContext not set - cannot open email client")
        return
    }
    // ACTION_SENDTO with a mailto: data URI restricts the chooser to email apps only.
    // Subject/body ride as intent extras (no manual encoding needed) — the mailto URI
    // carries just the recipient, which every email app resolves reliably.
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$recipient")).apply {
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        // Required because we launch from the application context, not an Activity.
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        ctx.startActivity(intent)
    } catch (e: Exception) {
        // No email app installed / nothing resolved the intent.
        Log.d("Platform: no email client available - ${e.message}")
    }
}

actual fun openUrl(url: String) {
    val ctx = androidAppContext ?: run {
        Log.d("Platform: androidAppContext not set - cannot open URL")
        return
    }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        // Required because we launch from the application context, not an Activity.
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        ctx.startActivity(intent)
    } catch (e: Exception) {
        // No browser available / nothing resolved the intent.
        Log.d("Platform: no browser available - ${e.message}")
    }
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
