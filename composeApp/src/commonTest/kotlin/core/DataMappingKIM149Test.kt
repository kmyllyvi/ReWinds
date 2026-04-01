package core

import com.km.rewinds.db.Day as DayDb
import com.km.rewinds.db.WeatherResponse as WeatherResponseDb
import core.DataMapping
import core.Day
import core.Station
import core.WeatherResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for DataMapping.fromWeatherResponse() and toWeatherResponse()
 * focusing on station coordinate extraction (KIM-149)
 *
 * Tests verify:
 * - Primary station selection by highest useCount
 * - Station coordinate persistence from API response to DB
 * - Station coordinate reconstruction when loading from DB
 */
class DataMappingKIM149Test {

    private val dataMapping = DataMapping()

    // ==================== fromWeatherResponse() Tests ====================

    @Test
    fun testFromWeatherResponse_singleStationExtractsCoordinates() {
        // Arrange
        val station = Station(
            id = "KEEF",
            name = "Key West International Airport",
            distance = 10.5,
            latitude = 24.556,
            longitude = -81.760,
            useCount = 5,
            quality = 100,
            contribution = 0.8
        )
        val response = WeatherResponse(
            resolvedAddress = "Key West, Florida",
            address = "Key West",
            queryCost = 1,
            latitude = 24.5,
            longitude = -81.8,
            timezone = "America/Chicago",
            tzoffset = -6.0,
            days = emptyList(),
            stations = mapOf("KEEF" to station)
        )

        // Act
        val dbResponse = dataMapping.fromWeatherResponse(response)

        // Assert
        assertEquals(24.556, dbResponse.stationLatitude)
        assertEquals(-81.760, dbResponse.stationLongitude)
    }

    @Test
    fun testFromWeatherResponse_multipleStationsPicksHighestUseCount() {
        // Arrange
        val stations = mapOf(
            "station1" to Station(
                id = "station1",
                name = "Station 1",
                distance = 5.0,
                latitude = 60.0,
                longitude = 25.0,
                useCount = 2,
                quality = 100,
                contribution = 0.5
            ),
            "station2" to Station(
                id = "station2",
                name = "Station 2",
                distance = 10.0,
                latitude = 61.0,
                longitude = 26.0,
                useCount = 8,  // Highest useCount
                quality = 100,
                contribution = 0.8
            ),
            "station3" to Station(
                id = "station3",
                name = "Station 3",
                distance = 15.0,
                latitude = 62.0,
                longitude = 27.0,
                useCount = 3,
                quality = 100,
                contribution = 0.3
            )
        )
        val response = WeatherResponse(
            resolvedAddress = "Test Location",
            address = "Test",
            queryCost = 1,
            latitude = 60.5,
            longitude = 25.5,
            timezone = "UTC",
            tzoffset = 0.0,
            days = emptyList(),
            stations = stations
        )

        // Act
        val dbResponse = dataMapping.fromWeatherResponse(response)

        // Assert: station2 has highest useCount (8)
        assertEquals(61.0, dbResponse.stationLatitude)
        assertEquals(26.0, dbResponse.stationLongitude)
    }

    @Test
    fun testFromWeatherResponse_stationWithZeroUseCountNotPicked() {
        // Arrange
        val stations = mapOf(
            "station1" to Station(
                id = "station1",
                name = "Station 1",
                distance = 5.0,
                latitude = 60.0,
                longitude = 25.0,
                useCount = 0,
                quality = 100,
                contribution = 0.1
            ),
            "station2" to Station(
                id = "station2",
                name = "Station 2",
                distance = 10.0,
                latitude = 61.0,
                longitude = 26.0,
                useCount = 1,  // Highest (even though just 1)
                quality = 100,
                contribution = 0.9
            )
        )
        val response = WeatherResponse(
            resolvedAddress = "Test Location",
            address = "Test",
            queryCost = 1,
            latitude = 60.5,
            longitude = 25.5,
            timezone = "UTC",
            tzoffset = 0.0,
            days = emptyList(),
            stations = stations
        )

        // Act
        val dbResponse = dataMapping.fromWeatherResponse(response)

        // Assert: station2 is picked (useCount=1 > useCount=0)
        assertEquals(61.0, dbResponse.stationLatitude)
        assertEquals(26.0, dbResponse.stationLongitude)
    }

    @Test
    fun testFromWeatherResponse_stationWithNullUseCountTreatedAsZero() {
        // Arrange
        val stations = mapOf(
            "station1" to Station(
                id = "station1",
                name = "Station 1",
                distance = 5.0,
                latitude = 60.0,
                longitude = 25.0,
                useCount = null,
                quality = 100,
                contribution = 0.1
            ),
            "station2" to Station(
                id = "station2",
                name = "Station 2",
                distance = 10.0,
                latitude = 61.0,
                longitude = 26.0,
                useCount = 5,  // This is highest
                quality = 100,
                contribution = 0.9
            )
        )
        val response = WeatherResponse(
            resolvedAddress = "Test Location",
            address = "Test",
            queryCost = 1,
            latitude = 60.5,
            longitude = 25.5,
            timezone = "UTC",
            tzoffset = 0.0,
            days = emptyList(),
            stations = stations
        )

        // Act
        val dbResponse = dataMapping.fromWeatherResponse(response)

        // Assert: station2 is picked (useCount=5 > null which is treated as 0)
        assertEquals(61.0, dbResponse.stationLatitude)
        assertEquals(26.0, dbResponse.stationLongitude)
    }

    @Test
    fun testFromWeatherResponse_nullStationsResultsInNullCoordinates() {
        // Arrange
        val response = WeatherResponse(
            resolvedAddress = "Test Location",
            address = "Test",
            queryCost = 1,
            latitude = 60.5,
            longitude = 25.5,
            timezone = "UTC",
            tzoffset = 0.0,
            days = emptyList(),
            stations = null
        )

        // Act
        val dbResponse = dataMapping.fromWeatherResponse(response)

        // Assert
        assertNull(dbResponse.stationLatitude)
        assertNull(dbResponse.stationLongitude)
    }

    @Test
    fun testFromWeatherResponse_emptyStationsResultsInNullCoordinates() {
        // Arrange
        val response = WeatherResponse(
            resolvedAddress = "Test Location",
            address = "Test",
            queryCost = 1,
            latitude = 60.5,
            longitude = 25.5,
            timezone = "UTC",
            tzoffset = 0.0,
            days = emptyList(),
            stations = emptyMap()
        )

        // Act
        val dbResponse = dataMapping.fromWeatherResponse(response)

        // Assert
        assertNull(dbResponse.stationLatitude)
        assertNull(dbResponse.stationLongitude)
    }

    @Test
    fun testFromWeatherResponse_stationCoordinatesNegativeValues() {
        // Arrange: test with negative coordinates (Southern Hemisphere / Western Hemisphere)
        val station = Station(
            id = "KJFK",
            name = "JFK International",
            distance = 5.0,
            latitude = -40.6413,  // South
            longitude = -73.7781,  // West
            useCount = 10,
            quality = 100,
            contribution = 1.0
        )
        val response = WeatherResponse(
            resolvedAddress = "New York",
            address = "New York",
            queryCost = 1,
            latitude = -40.6,
            longitude = -73.8,
            timezone = "America/New_York",
            tzoffset = -5.0,
            days = emptyList(),
            stations = mapOf("KJFK" to station)
        )

        // Act
        val dbResponse = dataMapping.fromWeatherResponse(response)

        // Assert
        assertEquals(-40.6413, dbResponse.stationLatitude)
        assertEquals(-73.7781, dbResponse.stationLongitude)
    }

    @Test
    fun testFromWeatherResponse_preservesOtherFields() {
        // Arrange
        val station = Station(
            id = "TEST",
            name = "Test Station",
            distance = 10.0,
            latitude = 60.0,
            longitude = 25.0,
            useCount = 5,
            quality = 100,
            contribution = 0.8
        )
        val response = WeatherResponse(
            resolvedAddress = "Helsinki, Finland",
            address = "Helsinki",
            queryCost = 1,
            latitude = 60.17,
            longitude = 24.94,
            timezone = "Europe/Helsinki",
            tzoffset = 2.0,
            days = emptyList(),
            stations = mapOf("TEST" to station)
        )

        // Act
        val dbResponse = dataMapping.fromWeatherResponse(response)

        // Assert: verify other fields are preserved correctly
        assertEquals("Helsinki, Finland", dbResponse.resolvedAddress)
        assertEquals("Helsinki", dbResponse.address)
        assertEquals(1, dbResponse.queryCost)
        assertEquals(60.17, dbResponse.latitude)
        assertEquals(24.94, dbResponse.longitude)
        assertEquals("Europe/Helsinki", dbResponse.timezone)
        assertEquals(2.0, dbResponse.tzoffset)
    }

    // ==================== toWeatherResponse() Tests ====================

    @Test
    fun testToWeatherResponse_withStationCoordinatesReconstructsStationsMap() {
        // Arrange
        val dbResponse = WeatherResponseDb(
            resolvedAddress = "Helsinki, Finland",
            address = "Helsinki",
            queryCost = 1,
            latitude = 60.17,
            longitude = 24.94,
            timezone = "Europe/Helsinki",
            tzoffset = 2.0,
            stationLatitude = 60.32,
            stationLongitude = 25.04
        )

        // Act
        val response = dataMapping.toWeatherResponse(dbResponse, emptyList())

        // Assert: stations map is reconstructed
        assertNotNull(response.stations)
        assertEquals(1, response.stations.size)
        assertTrue(response.stations.containsKey("primary"))

        val primaryStation = response.stations["primary"]
        assertNotNull(primaryStation)
        assertEquals(60.32, primaryStation.latitude)
        assertEquals(25.04, primaryStation.longitude)
        assertEquals("primary", primaryStation.id)
    }

    @Test
    fun testToWeatherResponse_primaryStationHasCorrectStructure() {
        // Arrange
        val dbResponse = WeatherResponseDb(
            resolvedAddress = "Test",
            address = "Test",
            queryCost = 1,
            latitude = 0.0,
            longitude = 0.0,
            timezone = "UTC",
            tzoffset = 0.0,
            stationLatitude = 60.32,
            stationLongitude = 25.04
        )

        // Act
        val response = dataMapping.toWeatherResponse(dbResponse, emptyList())

        // Assert: primary station has expected structure
        val station = response.stations!!["primary"]!!
        assertEquals("primary", station.id)
        assertNull(station.name)
        assertNull(station.distance)
        assertEquals(60.32, station.latitude)
        assertEquals(25.04, station.longitude)
        assertNull(station.useCount)
        assertNull(station.quality)
        assertNull(station.contribution)
    }

    @Test
    fun testToWeatherResponse_nullStationCoordinatesResultsInNullStations() {
        // Arrange
        val dbResponse = WeatherResponseDb(
            resolvedAddress = "Test",
            address = "Test",
            queryCost = 1,
            latitude = 60.0,
            longitude = 25.0,
            timezone = "UTC",
            tzoffset = 0.0,
            stationLatitude = null,
            stationLongitude = null
        )

        // Act
        val response = dataMapping.toWeatherResponse(dbResponse, emptyList())

        // Assert
        assertNull(response.stations)
    }

    @Test
    fun testToWeatherResponse_partialStationCoordinatesResultsInNullStations() {
        // Arrange - only latitude is set, longitude is null
        val dbResponse = WeatherResponseDb(
            resolvedAddress = "Test",
            address = "Test",
            queryCost = 1,
            latitude = 60.0,
            longitude = 25.0,
            timezone = "UTC",
            tzoffset = 0.0,
            stationLatitude = 60.32,
            stationLongitude = null
        )

        // Act
        val response = dataMapping.toWeatherResponse(dbResponse, emptyList())

        // Assert
        assertNull(response.stations)
    }

    @Test
    fun testToWeatherResponse_partialStationCoordinatesLatitudeNull() {
        // Arrange - only longitude is set, latitude is null
        val dbResponse = WeatherResponseDb(
            resolvedAddress = "Test",
            address = "Test",
            queryCost = 1,
            latitude = 60.0,
            longitude = 25.0,
            timezone = "UTC",
            tzoffset = 0.0,
            stationLatitude = null,
            stationLongitude = 25.04
        )

        // Act
        val response = dataMapping.toWeatherResponse(dbResponse, emptyList())

        // Assert
        assertNull(response.stations)
    }

    @Test
    fun testToWeatherResponse_negativeStationCoordinates() {
        // Arrange
        val dbResponse = WeatherResponseDb(
            resolvedAddress = "Test",
            address = "Test",
            queryCost = 1,
            latitude = 0.0,
            longitude = 0.0,
            timezone = "UTC",
            tzoffset = 0.0,
            stationLatitude = -40.6413,
            stationLongitude = -73.7781
        )

        // Act
        val response = dataMapping.toWeatherResponse(dbResponse, emptyList())

        // Assert
        assertNotNull(response.stations)
        val station = response.stations["primary"]!!
        assertEquals(-40.6413, station.latitude)
        assertEquals(-73.7781, station.longitude)
    }

    @Test
    fun testToWeatherResponse_preservesDaysAndOtherFields() {
        // Arrange
        val testDay = Day(
            datetime = "2026-04-01",
            datetimeEpoch = 1743638400,
            tempmax = 15.5,
            tempmin = 8.0,
            temp = 12.0,
            feelslikemax = null,
            feelslikemin = null,
            feelslike = null,
            dew = null,
            humidity = 70.0,
            precip = 2.5,
            precipprob = 50.0,
            precipcover = null,
            preciptype = null,
            snow = null,
            snowdepth = null,
            windgust = 15.0,
            windspeed = 10.0,
            winddir = 270.0,
            pressure = 1013.25,
            cloudcover = 60.0,
            visibility = 10.0,
            solarradiation = null,
            solarenergy = null,
            uvindex = 3.0,
            sunrise = "06:30",
            sunriseEpoch = 1743616200,
            sunset = "19:30",
            sunsetEpoch = 1743660600,
            moonphase = 0.5,
            conditions = "Partly cloudy",
            description = "Partly cloudy with occasional rain",
            icon = "partly-cloudy-rain",
            stations = null,
            source = "API",
            hours = null,
            normal = null
        )
        val dbResponse = WeatherResponseDb(
            resolvedAddress = "Helsinki, Finland",
            address = "Helsinki",
            queryCost = 1,
            latitude = 60.17,
            longitude = 24.94,
            timezone = "Europe/Helsinki",
            tzoffset = 2.0,
            stationLatitude = 60.32,
            stationLongitude = 25.04
        )

        // Act
        val response = dataMapping.toWeatherResponse(dbResponse, listOf(testDay))

        // Assert: other fields are preserved
        assertEquals("Helsinki, Finland", response.resolvedAddress)
        assertEquals("Helsinki", response.address)
        assertEquals(1, response.queryCost)
        assertEquals(60.17, response.latitude)
        assertEquals(24.94, response.longitude)
        assertEquals("Europe/Helsinki", response.timezone)
        assertEquals(2.0, response.tzoffset)
        assertEquals(1, response.days?.size)
    }

    // ==================== Round-trip Tests ====================

    @Test
    fun testRoundTripWithStationCoordinates() {
        // Arrange: API response with station
        val station = Station(
            id = "KORD",
            name = "Chicago O'Hare",
            distance = 15.0,
            latitude = 41.9742,
            longitude = -87.9073,
            useCount = 10,
            quality = 100,
            contribution = 0.95
        )
        val originalResponse = WeatherResponse(
            resolvedAddress = "Chicago, Illinois",
            address = "Chicago",
            queryCost = 2,
            latitude = 41.8,
            longitude = -87.6,
            timezone = "America/Chicago",
            tzoffset = -6.0,
            days = emptyList(),
            stations = mapOf("KORD" to station)
        )

        // Act: convert API -> DB -> API
        val dbResponse = dataMapping.fromWeatherResponse(originalResponse)
        val reconstructedResponse = dataMapping.toWeatherResponse(dbResponse, emptyList())

        // Assert: station coordinates are preserved through round trip
        assertNotNull(reconstructedResponse.stations)
        val reconstructedStation = reconstructedResponse.stations["primary"]!!
        assertEquals(41.9742, reconstructedStation.latitude)
        assertEquals(-87.9073, reconstructedStation.longitude)
    }

    @Test
    fun testRoundTripWithoutStation() {
        // Arrange: API response without stations
        val originalResponse = WeatherResponse(
            resolvedAddress = "Unknown Location",
            address = "Unknown",
            queryCost = 1,
            latitude = 0.0,
            longitude = 0.0,
            timezone = "UTC",
            tzoffset = 0.0,
            days = emptyList(),
            stations = null
        )

        // Act: convert API -> DB -> API
        val dbResponse = dataMapping.fromWeatherResponse(originalResponse)
        val reconstructedResponse = dataMapping.toWeatherResponse(dbResponse, emptyList())

        // Assert: stations remain null
        assertNull(reconstructedResponse.stations)
    }
}
