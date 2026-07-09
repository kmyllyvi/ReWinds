package core

import com.km.rewinds.db.WeatherResponse as WeatherResponseDb
import core.DataMapping
import core.Station
import core.WeatherResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for DataMapping.fromWeatherResponse() and toWeatherResponse()
 * focusing on station data handling after KIM-258 migration.
 *
 * Prior to KIM-258, a single primary station was extracted and stored as
 * stationLatitude/stationLongitude on the WeatherResponse row. Those columns
 * are gone — stations are now persisted in a separate WeatherStation table and
 * joined when loading. This test file validates the new behaviour.
 */
class DataMappingKIM149Test {

    private val dataMapping = DataMapping()

    // ==================== fromWeatherResponse() Tests ====================

    @Test
    fun testFromWeatherResponse_doesNotWriteStationCoordinates() {
        // fromWeatherResponse now only maps the WeatherResponse-level fields.
        // Station data is handled separately via Database.upsertStations.
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

        val dbResponse = dataMapping.fromWeatherResponse(response)

        // WeatherResponseDb no longer has stationLatitude/stationLongitude columns.
        // The mapping should produce a clean row with only WeatherResponse-level fields.
        assertEquals("Key West, Florida", dbResponse.resolvedAddress)
        assertEquals("Key West", dbResponse.address)
        assertEquals(1, dbResponse.queryCost)
        assertEquals(24.5, dbResponse.latitude)
        assertEquals(-81.8, dbResponse.longitude)
        assertEquals("America/Chicago", dbResponse.timezone)
        assertEquals(-6.0, dbResponse.tzoffset)
    }

    @Test
    fun testFromWeatherResponse_nullStationsProducesCleanRow() {
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

        val dbResponse = dataMapping.fromWeatherResponse(response)

        // No crash, no extra fields — just the core columns.
        assertEquals("Test Location", dbResponse.resolvedAddress)
        assertEquals(60.5, dbResponse.latitude)
        assertEquals(25.5, dbResponse.longitude)
    }

    @Test
    fun testFromWeatherResponse_preservesOtherFields() {
        val response = WeatherResponse(
            resolvedAddress = "Helsinki, Finland",
            address = "Helsinki",
            queryCost = 1,
            latitude = 60.17,
            longitude = 24.94,
            timezone = "Europe/Helsinki",
            tzoffset = 2.0,
            days = emptyList(),
            stations = null
        )

        val dbResponse = dataMapping.fromWeatherResponse(response)

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
    fun testToWeatherResponse_emptyStationsListProducesNullStationsMap() {
        // When no WeatherStation rows are available, stations should be null.
        val dbResponse = WeatherResponseDb(
            resolvedAddress = "Test",
            address = "Test",
            queryCost = 1,
            latitude = 60.0,
            longitude = 25.0,
            timezone = "UTC",
            tzoffset = 0.0,
            archivedAt = null
        )

        val response = dataMapping.toWeatherResponse(dbResponse, emptyList(), emptyList())

        assertNull(response.stations)
    }

    @Test
    fun testToWeatherResponse_singleStationProducesMapWithOneEntry() {
        val dbResponse = WeatherResponseDb(
            resolvedAddress = "Helsinki, Finland",
            address = "Helsinki",
            queryCost = 1,
            latitude = 60.17,
            longitude = 24.94,
            timezone = "Europe/Helsinki",
            tzoffset = 2.0,
            archivedAt = null
        )
        val station = Station(
            id = "EFHK",
            name = "Helsinki-Vantaa",
            latitude = 60.32,
            longitude = 25.04,
            distance = 12.0,
            quality = 50,
            useCount = 100,
            contribution = 0.9
        )

        val response = dataMapping.toWeatherResponse(dbResponse, emptyList(), listOf(station))

        assertNotNull(response.stations)
        assertEquals(1, response.stations.size)
        assertTrue(response.stations.containsKey("EFHK"))
        val mapped = response.stations["EFHK"]!!
        assertEquals(60.32, mapped.latitude)
        assertEquals(25.04, mapped.longitude)
        assertEquals("Helsinki-Vantaa", mapped.name)
    }

    @Test
    fun testToWeatherResponse_multipleStationsPreservedInMap() {
        val dbResponse = WeatherResponseDb(
            resolvedAddress = "Test",
            address = "Test",
            queryCost = 0,
            latitude = 0.0,
            longitude = 0.0,
            timezone = "UTC",
            tzoffset = 0.0,
            archivedAt = null
        )
        val stations = listOf(
            Station(id = "S1", name = "Station 1", latitude = 60.0, longitude = 25.0,
                distance = null, quality = null, useCount = 2, contribution = null),
            Station(id = "S2", name = "Station 2", latitude = 61.0, longitude = 26.0,
                distance = null, quality = null, useCount = 8, contribution = null),
            Station(id = "S3", name = "Station 3", latitude = 62.0, longitude = 27.0,
                distance = null, quality = null, useCount = 3, contribution = null)
        )

        val response = dataMapping.toWeatherResponse(dbResponse, emptyList(), stations)

        assertNotNull(response.stations)
        assertEquals(3, response.stations.size)
        assertTrue(response.stations.containsKey("S1"))
        assertTrue(response.stations.containsKey("S2"))
        assertTrue(response.stations.containsKey("S3"))
    }

    @Test
    fun testToWeatherResponse_stationsKeyedByIdWhenIdNotNull() {
        val dbResponse = WeatherResponseDb(
            resolvedAddress = "Test",
            address = "Test",
            queryCost = 0,
            latitude = 0.0,
            longitude = 0.0,
            timezone = "UTC",
            tzoffset = 0.0,
            archivedAt = null
        )
        val station = Station(
            id = "KORD",
            name = "Chicago O'Hare",
            latitude = 41.97,
            longitude = -87.91,
            distance = 15.0,
            quality = null,
            useCount = null,
            contribution = null
        )

        val response = dataMapping.toWeatherResponse(dbResponse, emptyList(), listOf(station))

        assertNotNull(response.stations)
        assertTrue(response.stations.containsKey("KORD"), "Station should be keyed by its id")
    }

    @Test
    fun testToWeatherResponse_negativeCoordinatesRoundTrip() {
        // Southern/Western Hemisphere coordinates should be preserved exactly.
        val dbResponse = WeatherResponseDb(
            resolvedAddress = "New York",
            address = "New York",
            queryCost = 1,
            latitude = -40.6,
            longitude = -73.8,
            timezone = "America/New_York",
            tzoffset = -5.0,
            archivedAt = null
        )
        val station = Station(
            id = "KJFK",
            name = "JFK International",
            latitude = -40.6413,
            longitude = -73.7781,
            distance = 5.0,
            quality = 100,
            useCount = 10,
            contribution = 1.0
        )

        val response = dataMapping.toWeatherResponse(dbResponse, emptyList(), listOf(station))

        assertNotNull(response.stations)
        val mapped = response.stations["KJFK"]!!
        assertEquals(-40.6413, mapped.latitude)
        assertEquals(-73.7781, mapped.longitude)
    }

    @Test
    fun testToWeatherResponse_preservesWeatherResponseFields() {
        val dbResponse = WeatherResponseDb(
            resolvedAddress = "Helsinki, Finland",
            address = "Helsinki",
            queryCost = 1,
            latitude = 60.17,
            longitude = 24.94,
            timezone = "Europe/Helsinki",
            tzoffset = 2.0,
            archivedAt = null
        )

        val response = dataMapping.toWeatherResponse(dbResponse, emptyList())

        assertEquals("Helsinki, Finland", response.resolvedAddress)
        assertEquals("Helsinki", response.address)
        assertEquals(1, response.queryCost)
        assertEquals(60.17, response.latitude)
        assertEquals(24.94, response.longitude)
        assertEquals("Europe/Helsinki", response.timezone)
        assertEquals(2.0, response.tzoffset)
    }

    // ==================== Round-trip Tests ====================

    @Test
    fun testRoundTripWithoutStation() {
        // No station data in either direction.
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

        val dbResponse = dataMapping.fromWeatherResponse(originalResponse)
        val reconstructedResponse = dataMapping.toWeatherResponse(dbResponse, emptyList())

        // No stations passed → stations remain null.
        assertNull(reconstructedResponse.stations)
    }

    @Test
    fun testRoundTripStationsReconstructedFromPersistedList() {
        // Simulate the new flow: stations are fetched separately and passed in when loading.
        val originalResponse = WeatherResponse(
            resolvedAddress = "Chicago, Illinois",
            address = "Chicago",
            queryCost = 2,
            latitude = 41.8,
            longitude = -87.6,
            timezone = "America/Chicago",
            tzoffset = -6.0,
            days = emptyList(),
            stations = null // stations are persisted independently now
        )

        val dbResponse = dataMapping.fromWeatherResponse(originalResponse)

        // Simulate what comes back from the WeatherStation table join.
        val persistedStation = Station(
            id = "KORD",
            name = "Chicago O'Hare",
            distance = 15.0,
            latitude = 41.9742,
            longitude = -87.9073,
            useCount = 10,
            quality = 100,
            contribution = 0.95
        )

        val reconstructedResponse = dataMapping.toWeatherResponse(
            dbResponse, emptyList(), listOf(persistedStation)
        )

        assertNotNull(reconstructedResponse.stations)
        val reconstructedStation = reconstructedResponse.stations["KORD"]!!
        assertEquals(41.9742, reconstructedStation.latitude)
        assertEquals(-87.9073, reconstructedStation.longitude)
    }
}
