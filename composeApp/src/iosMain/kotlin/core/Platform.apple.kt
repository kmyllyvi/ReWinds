package core
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.*

import kotlinx.serialization.json.Json

// platform specific code, using "actual" implementations

// HttpClient is provided to ktor based on the platform
actual fun httpClient(): HttpClient {

    return HttpClient(Darwin) {
        println("httpClient for ios")
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }

        install(ContentNegotiation) {
            println("init ContentNegotiation")
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
}

fun initLogger2() {
    Napier.base(DebugAntilog())
}