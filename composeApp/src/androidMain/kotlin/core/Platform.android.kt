package core

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
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
    // For MVP, try to get from environment variable
    // In production, this should load from BuildConfig or secure storage
    val apiKey = System.getenv("ANTHROPIC_API_KEY")
    if (apiKey != null && apiKey.isNotBlank()) {
        return apiKey
    }

    // Fallback error
    throw IllegalStateException(
        "ANTHROPIC_API_KEY environment variable not set. " +
        "Please set it before running the app or configure it in BuildConfig."
    )
}
