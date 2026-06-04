package core

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Regression test for KIM-259: Visual Crossing returns `stations` as a JSON **object** keyed by
 * station ID, not a JSON array. This test feeds a trimmed real-shape payload (from Ermatingen,
 * with 9 stations) through the actual kotlinx-serialization decoder to guard against any
 * future model change that would silently produce an empty or null stations map.
 *
 * The test gap that let the bug through: all existing tests used fake repositories that
 * handed pre-built Station lists directly to the ViewModel, never touching the JSON layer.
 */
class StationsJsonDeserializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Trimmed real-shape payload from Visual Crossing for Ermatingen (lat 47.659, lon 9.08).
     * Stations object is keyed by station ID — the shape the API always returns.
     * Nine stations are included so the test asserts count = 9 without relying on a specific ordering.
     */
    private val ermatigenResponseJson = """
        {
          "queryCost": 0,
          "latitude": 47.659,
          "longitude": 9.08,
          "resolvedAddress": "Ermatingen, Thurgau, Switzerland",
          "address": "Ermatingen",
          "timezone": "Europe/Zurich",
          "tzoffset": 2.0,
          "days": [],
          "stations": {
            "06258": {
              "distance": 26666.0,
              "latitude": 47.685,
              "longitude": 9.441,
              "useCount": 0,
              "id": "06258",
              "name": "Friedrichshafen-Unterraderach",
              "quality": 100,
              "contribution": 0.0
            },
            "06263": {
              "distance": 22856.0,
              "latitude": 47.774,
              "longitude": 8.822,
              "useCount": 0,
              "id": "06263",
              "name": "Singen",
              "quality": 100,
              "contribution": 0.0
            },
            "03927": {
              "distance": 32997.0,
              "latitude": 47.934,
              "longitude": 9.287,
              "useCount": 0,
              "id": "03927",
              "name": "Pfullendorf",
              "quality": 100,
              "contribution": 0.0
            },
            "06272": {
              "distance": 9705.0,
              "latitude": 47.689,
              "longitude": 9.185,
              "useCount": 0,
              "id": "06272",
              "name": "Konstanz",
              "quality": 50,
              "contribution": 0.0
            },
            "06283": {
              "distance": 20048.0,
              "latitude": 47.671,
              "longitude": 8.885,
              "useCount": 0,
              "id": "06283",
              "name": "Schaffhausen",
              "quality": 50,
              "contribution": 0.0
            },
            "LSZR": {
              "distance": 16723.0,
              "latitude": 47.484,
              "longitude": 9.561,
              "useCount": 0,
              "id": "LSZR",
              "name": "St Gallen-Altenrhein",
              "quality": 50,
              "contribution": 0.0
            },
            "LSZI": {
              "distance": 24012.0,
              "latitude": 47.486,
              "longitude": 8.877,
              "useCount": 0,
              "id": "LSZI",
              "name": "Winterthur",
              "quality": 50,
              "contribution": 0.0
            },
            "LFSB": {
              "distance": 94000.0,
              "latitude": 47.600,
              "longitude": 7.527,
              "useCount": 0,
              "id": "LFSB",
              "name": "Basel-Mulhouse",
              "quality": 50,
              "contribution": 0.0
            },
            "AT546": {
              "distance": 3995.0,
              "latitude": 47.652,
              "longitude": 9.132,
              "useCount": 0,
              "id": "AT546",
              "name": "HB9KNR Tagerwilen CH",
              "quality": 0,
              "contribution": 0.0
            }
          }
        }
    """.trimIndent()

    @Test
    fun stationsObjectDeserializesToMapWithNineEntries() {
        val response = json.decodeFromString<WeatherResponse>(ermatigenResponseJson)

        assertNotNull(response.stations, "stations must not be null — the JSON object must decode to Map<String, Station>")
        assertEquals(9, response.stations.size, "all 9 station entries must be decoded")
    }

    @Test
    fun stationFieldsAreCorrectlyMapped() {
        val response = json.decodeFromString<WeatherResponse>(ermatigenResponseJson)
        val konstanz = response.stations?.get("06272")

        assertNotNull(konstanz, "station keyed '06272' (Konstanz) must be present")
        assertEquals("06272", konstanz.id)
        assertEquals("Konstanz", konstanz.name)
        assertEquals(47.689, konstanz.latitude)
        assertEquals(9.185, konstanz.longitude)
        assertEquals(9705.0, konstanz.distance)
        assertEquals(50, konstanz.quality)
    }

    @Test
    fun filteringByNonNullLatLonKeepsAllNineStations() {
        // All 9 stations in the test payload have explicit lat/lon, so none should be dropped.
        val response = json.decodeFromString<WeatherResponse>(ermatigenResponseJson)
        val validStations = response.stations?.values
            ?.filter { it.latitude != null && it.longitude != null }
            ?: emptyList()

        assertEquals(9, validStations.size, "all 9 stations have coordinates and should survive the lat/lon filter")
    }

    @Test
    fun stationsAreNullWhenKeyAbsentFromResponse() {
        // A response without the stations key must not crash — stations should be null.
        val noStationsJson = """
            {
              "queryCost": 0,
              "latitude": 47.659,
              "longitude": 9.08,
              "resolvedAddress": "Ermatingen, Thurgau, Switzerland",
              "timezone": "Europe/Zurich",
              "tzoffset": 2.0,
              "days": []
            }
        """.trimIndent()

        val response = json.decodeFromString<WeatherResponse>(noStationsJson)
        assertEquals(null, response.stations)
    }
}
