package core



import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.serialization.json.Json

// platform specific code, using "actual" implementations

// HttpClient is provided to ktor based on the platform
actual fun httpClient(): HttpClient {
    return HttpClient(CIO) {
        println("HttpClient for Android")
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