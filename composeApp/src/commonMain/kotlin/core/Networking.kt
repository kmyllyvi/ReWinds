package core

interface Networking {
    suspend fun fetchWeatherData(url: String): WeatherResponse
    suspend fun fetchGeoSearchData(url: String): GeoSearchResponse
}
