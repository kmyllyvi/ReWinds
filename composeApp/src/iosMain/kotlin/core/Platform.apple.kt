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
    // On iOS, the API key should be provided via Info.plist or at runtime
    // For MVP, this is a placeholder that must be configured before use
    // In production, this should load from a secure configuration mechanism

    // Development fallback: return a placeholder key
    // NOTE: This will fail at runtime when calling Anthropic API unless a real key is set
    // TODO: Configure with actual API key mechanism for iOS (Info.plist, BuildConfig, or secure storage)
    Log.d("Platform: ANTHROPIC_API_KEY not configured on iOS, using placeholder for development")
    return "sk-placeholder-dev-key-not-configured"
}
