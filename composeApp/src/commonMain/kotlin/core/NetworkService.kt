package core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.koin.core.annotation.Single

interface Networking {
    suspend fun fetchWeatherData(url: String): WeatherResponse
    suspend fun fetchGeoSearchData(url: String): GeoSearchResponse
}

@Single
class NetworkService internal constructor(
    // HttpClient is provided to Ktor based on the platform (engine differs per platform).
    // Kept as a constructor parameter so unit tests can inject a MockEngine-backed client to
    // exercise the error-mapping paths (4xx/5xx and connection failures) without real network I/O.
    private val client: HttpClient
): Networking {

    init {
        println("NetworkService initialized")
    }

    // Production constructor bound by Koin DI: builds the platform HttpClient.
    constructor(enableNetworkLogs: Boolean) : this(httpClient(enableNetworkLogs))

    override suspend fun fetchWeatherData(url: String): WeatherResponse {
        Log.d("NetworkService::fetchWeatherData: $url")
        return try {
            client.get(url).body()
        } catch (e: ClientRequestException) {
            val errorBody = e.response.bodyAsText()
            Log.e("API Client Error for $url: $errorBody", e)
            throw NetworkException(errorBody, e, httpStatus = e.response.status.value)
        } catch (e: Exception) {
            Log.e("Generic Network Error for $url", e)
            throw NetworkException("Network request failed: ${e.message}", e)
        }
    }

    override suspend fun fetchGeoSearchData(url: String): GeoSearchResponse {
        Log.d("NetworkService::fetchGeoSearchData: $url")
        return try {
            client.get(url).body()
        } catch (e: ClientRequestException) {
            val errorBody = e.response.bodyAsText()
            Log.e("API Client Error for $url: $errorBody", e)
            throw NetworkException(errorBody, e, httpStatus = e.response.status.value)
        } catch (e: Exception) {
            Log.e("Generic Network Error for $url", e)
            throw NetworkException("Network request failed: ${e.message}", e)
        }
    }
}
