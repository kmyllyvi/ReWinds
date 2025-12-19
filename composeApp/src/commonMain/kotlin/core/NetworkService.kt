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
class NetworkService(enableNetworkLogs: Boolean): Networking {

    init {
        println("NetworkService initialized")
    }

    // HttpClient is provided to ktor based on the platform
    private val client = httpClient(enableNetworkLogs)

    override suspend fun fetchWeatherData(url: String): WeatherResponse {
        Log.d("NetworkService::fetchWeatherData: $url")
        return try {
            client.get(url).body()
        } catch (e: ClientRequestException) {
            val errorBody = e.response.bodyAsText()
            Log.e("API Client Error for $url: $errorBody", e)
            throw NetworkException(errorBody, e)
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
            throw NetworkException(errorBody, e)
        } catch (e: Exception) {
            Log.e("Generic Network Error for $url", e)
            throw NetworkException("Network request failed: ${e.message}", e)
        }
    }
}
