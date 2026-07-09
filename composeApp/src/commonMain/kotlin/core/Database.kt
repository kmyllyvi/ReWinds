package core

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.km.rewinds.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A factory for creating a platform-specific SQLDriver.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

/**
 * Creates a new AppDatabase instance.
 */
fun createDatabase(driverFactory: DatabaseDriverFactory): AppDatabase {
    val driver = driverFactory.createDriver()
    return AppDatabase(
        driver = driver,
        DayAdapter = com.km.rewinds.db.Day.Adapter(
            preciptypeAdapter = listOfStringAdapter
        ),
        HourAdapter = com.km.rewinds.db.Hour.Adapter(
            preciptypeAdapter = listOfStringAdapter
        )
    )
}

/**
 * The archive state of a place, as distinguished by the repository when deciding whether an
 * "add place" should un-archive an existing row or fetch a brand-new one (KIM-364).
 */
enum class PlaceArchiveState {
    /** No WeatherResponse row exists for this place — treat as brand new. */
    ABSENT,

    /** A row exists and is active (archivedAt IS NULL). */
    ACTIVE,

    /** A row exists and is archived (archivedAt IS NOT NULL) — a candidate for un-archiving. */
    ARCHIVED
}

interface Database {
    // get all saved places
    suspend fun getAllSavedPlaces(): List<String>

    // get stored-day counts per place in a single GROUP BY query (no day/hour loading)
    suspend fun getPlaceDayCounts(): Map<String, Long>

    // distinct YYYY-MM months that have at least one stored Day for a place (no day loading).
    // Default empty so unrelated test doubles need not override it (KIM-321).
    suspend fun getDownloadedMonths(place: String): Set<String> = emptySet()

    // reactive variant: re-emits whenever the Day table changes for any reason
    // (SQLDelight query invalidation). Used to keep the chat's month-set current.
    fun observeDownloadedMonths(place: String): Flow<Set<String>> =
        kotlinx.coroutines.flow.flowOf(emptySet())

    // get full data for a specific place
    suspend fun getSavedPlaceFull(place: String): WeatherResponse?

    // save new data
    suspend fun saveWeatherResponse(weatherResponse: WeatherResponse)

    // get data for a specific place and date
    suspend fun getWeatherDataFor(placeName: String, date: String): WeatherResponse?

    // delete a place
    suspend fun deletePlace(placeName: String)

    // Soft-delete: hide a place from the Home list without deleting its Day/Hour/Station rows.
    // Default no-op so unrelated test doubles need not override it (KIM-364).
    suspend fun archivePlace(placeName: String, archivedAt: Long) {}

    // Un-archive a previously archived place; its downloaded days reappear (KIM-364).
    suspend fun unarchivePlace(placeName: String) {}

    // Archive state of a place — lets the repository tell "brand-new" (no row) from "archived"
    // (row present, archivedAt non-null). Default ABSENT so unrelated test doubles need not
    // override it (KIM-364).
    suspend fun getArchiveState(placeName: String): PlaceArchiveState = PlaceArchiveState.ABSENT

    // cleanup forecast data: remove any days after yesterday
    suspend fun cleanupForecastDays()

    // Replace all WeatherStation rows for a place within a single transaction.
    suspend fun upsertStations(place: String, stations: List<Station>)

    // Return all persisted WeatherStation rows for a place (no network call).
    suspend fun getStationsForPlace(place: String): List<Station>
}

class SqlDelightDatabase(
    private val database: AppDatabase
) : Database {

    private val dataMapping = DataMapping()
    private val dbQuery = database.appDatabaseQueries

    override suspend fun getAllSavedPlaces(): List<String> {
        return withContext(Dispatchers.IO) {
            dbQuery.getAllWeatherResponseResolvedAddresses().executeAsList()
        }
    }

    override suspend fun getPlaceDayCounts(): Map<String, Long> {
        return withContext(Dispatchers.IO) {
            dbQuery.getAllPlaceDayCounts().executeAsList()
                .associate { it.weatherResponseResolvedAddress to it.dayCount }
        }
    }

    override suspend fun getDownloadedMonths(place: String): Set<String> {
        return withContext(Dispatchers.IO) {
            dbQuery.getDownloadedMonthsForPlace(place).executeAsList().toSet()
        }
    }

    override fun observeDownloadedMonths(place: String): Flow<Set<String>> {
        return dbQuery.getDownloadedMonthsForPlace(place)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.toSet() }
    }

    override suspend fun getSavedPlaceFull(place: String): WeatherResponse? {
        return withContext(Dispatchers.IO) {
            val dbResponse = dbQuery.getWeatherResponseByResolvedAddress(place).executeAsOneOrNull()
                ?: return@withContext null

            val dbDays = dbQuery.getDaysForWeatherResponse(place).executeAsList()
            val days = dbDays.map { dbDay ->
                val dbHours = dbQuery.getHoursForDay(dbDay.id).executeAsList()
                val hours = dbHours.map { dataMapping.mapHourDbToApiHour(it) }
                dataMapping.mapDayDbToApiDay(dbDay, hours)
            }

            val stations = dbQuery.getStationsForPlace(place).executeAsList()
                .map { dataMapping.mapDbStationToStation(it) }

            dataMapping.toWeatherResponse(dbResponse, days, stations)
        }
    }

    override suspend fun saveWeatherResponse(weatherResponse: WeatherResponse) {
        withContext(Dispatchers.IO) {
            dbQuery.transaction {
                val dbResponse = dataMapping.fromWeatherResponse(weatherResponse)
                val existing = dbQuery.getWeatherResponseByResolvedAddress(dbResponse.resolvedAddress).executeAsOneOrNull()

                if (existing == null) {
                    // New place — insert the WeatherResponse row and all days/hours
                    dbQuery.insertWeatherResponse(
                        resolvedAddress = dbResponse.resolvedAddress,
                        queryCost = dbResponse.queryCost,
                        latitude = dbResponse.latitude,
                        longitude = dbResponse.longitude,
                        address = dbResponse.address,
                        timezone = dbResponse.timezone,
                        tzoffset = dbResponse.tzoffset
                    )

                    weatherResponse.days?.forEach { day ->
                        insertDayAndHours(day, dbResponse.resolvedAddress)
                    }
                } else {
                    // Existing place — only add days not already stored
                    val existingDays = dbQuery.getDaysForWeatherResponse(dbResponse.resolvedAddress).executeAsList()
                    val existingDateTimes = existingDays.map { it.datetime }.toSet()
                    val newDays = weatherResponse.days?.filter { it.datetime !in existingDateTimes } ?: emptyList()

                    newDays.forEach { day ->
                        insertDayAndHours(day, dbResponse.resolvedAddress)
                    }
                }

                // Persist stations from the API response when present.
                // addPlaceFromSearch includes stations in the response; day-range downloads do not.
                val validStations = weatherResponse.stations?.values
                    ?.filter { it.latitude != null && it.longitude != null }
                    ?: emptyList()
                if (validStations.isNotEmpty()) {
                    dbQuery.deleteStationsForPlace(dbResponse.resolvedAddress)
                    validStations.forEach { station ->
                        dbQuery.insertWeatherStation(
                            resolvedAddress = dbResponse.resolvedAddress,
                            stationId = station.id,
                            name = station.name,
                            latitude = station.latitude!!,
                            longitude = station.longitude!!,
                            distance = station.distance,
                            quality = station.quality?.toLong(),
                            useCount = station.useCount?.toLong(),
                            contribution = station.contribution
                        )
                    }
                }
            }
        }
    }

    override suspend fun upsertStations(place: String, stations: List<Station>) {
        withContext(Dispatchers.IO) {
            dbQuery.transaction {
                dbQuery.deleteStationsForPlace(place)
                stations.forEach { station ->
                    dbQuery.insertWeatherStation(
                        resolvedAddress = place,
                        stationId = station.id,
                        name = station.name,
                        latitude = station.latitude!!,
                        longitude = station.longitude!!,
                        distance = station.distance,
                        quality = station.quality?.toLong(),
                        useCount = station.useCount?.toLong(),
                        contribution = station.contribution
                    )
                }
            }
        }
    }

    override suspend fun getStationsForPlace(place: String): List<Station> {
        return withContext(Dispatchers.IO) {
            dbQuery.getStationsForPlace(place).executeAsList()
                .map { dataMapping.mapDbStationToStation(it) }
        }
    }

    private fun insertDayAndHours(day: Day, resolvedAddress: String) {
        val dayDb = dataMapping.mapApiDayToDayDb(day, resolvedAddress)
        dbQuery.insertDay(
            weatherResponseResolvedAddress = dayDb.weatherResponseResolvedAddress,
            datetime = dayDb.datetime,
            datetimeEpoch = dayDb.datetimeEpoch,
            tempmax = dayDb.tempmax,
            tempmin = dayDb.tempmin,
            temp = dayDb.temp,
            feelslikemax = dayDb.feelslikemax,
            feelslikemin = dayDb.feelslikemin,
            feelslike = dayDb.feelslike,
            dew = dayDb.dew,
            humidity = dayDb.humidity,
            precip = dayDb.precip,
            precipprob = dayDb.precipprob,
            precipcover = dayDb.precipcover,
            preciptype = dayDb.preciptype,
            snow = dayDb.snow,
            snowdepth = dayDb.snowdepth,
            windgust = dayDb.windgust,
            windspeed = dayDb.windspeed,
            winddir = dayDb.winddir,
            pressure = dayDb.pressure,
            cloudcover = dayDb.cloudcover,
            visibility = dayDb.visibility,
            solarradiation = dayDb.solarradiation,
            solarenergy = dayDb.solarenergy,
            uvindex = dayDb.uvindex,
            sunrise = dayDb.sunrise,
            sunriseEpoch = dayDb.sunriseEpoch,
            sunset = dayDb.sunset,
            sunsetEpoch = dayDb.sunsetEpoch,
            moonphase = dayDb.moonphase,
            conditions = dayDb.conditions,
            description = dayDb.description,
            icon = dayDb.icon
        )

        val dayId = dbQuery.lastInsertRowId().executeAsOne()
        day.hours?.forEach { hour ->
            val hourDb = dataMapping.mapApiHourToHourDb(hour, dayId)
            dbQuery.insertHour(
                dayId = hourDb.dayId,
                datetime = hourDb.datetime,
                datetimeEpoch = hourDb.datetimeEpoch,
                temp = hourDb.temp,
                feelslike = hourDb.feelslike,
                humidity = hourDb.humidity,
                dew = hourDb.dew,
                precip = hourDb.precip,
                precipprob = hourDb.precipprob,
                snow = hourDb.snow,
                snowdepth = hourDb.snowdepth,
                preciptype = hourDb.preciptype,
                windgust = hourDb.windgust,
                windspeed = hourDb.windspeed,
                winddir = hourDb.winddir,
                pressure = hourDb.pressure,
                visibility = hourDb.visibility,
                cloudcover = hourDb.cloudcover,
                solarradiation = hourDb.solarradiation,
                solarenergy = hourDb.solarenergy,
                uvindex = hourDb.uvindex,
                conditions = hourDb.conditions,
                icon = hourDb.icon,
                source = hourDb.source,
                stations = hourDb.stations
            )
        }
    }


    override suspend fun getWeatherDataFor(placeName: String, date: String): WeatherResponse? {
        return withContext(Dispatchers.IO) {
            val dbResponse = dbQuery.getWeatherResponseByResolvedAddress(placeName).executeAsOneOrNull() ?: return@withContext null
            val dbDays = dbQuery.getDaysForWeatherResponse(placeName).executeAsList().filter { it.datetime == date }

            if (dbDays.isEmpty()) {
                return@withContext null
            }

            val days = dbDays.map { dbDay ->
                val dbHours = dbQuery.getHoursForDay(dbDay.id).executeAsList()
                val hours = dbHours.map { dataMapping.mapHourDbToApiHour(it) }
                dataMapping.mapDayDbToApiDay(dbDay, hours)
            }
            dataMapping.toWeatherResponse(dbResponse, days)
        }
    }

    override suspend fun deletePlace(placeName: String) {
        withContext(Dispatchers.IO) {
            dbQuery.deleteWeatherResponseByResolvedAddress(placeName)
        }
    }

    override suspend fun archivePlace(placeName: String, archivedAt: Long) {
        withContext(Dispatchers.IO) {
            dbQuery.archivePlace(archivedAt = archivedAt, resolvedAddress = placeName)
        }
    }

    override suspend fun unarchivePlace(placeName: String) {
        withContext(Dispatchers.IO) {
            dbQuery.unarchivePlace(placeName)
        }
    }

    override suspend fun getArchiveState(placeName: String): PlaceArchiveState {
        return withContext(Dispatchers.IO) {
            // executeAsOneOrNull() is null when no row exists; the row's value is a nullable Long.
            val row = dbQuery.getArchivedAtForPlace(placeName).executeAsOneOrNull()
                ?: return@withContext PlaceArchiveState.ABSENT
            if (row.archivedAt == null) PlaceArchiveState.ACTIVE else PlaceArchiveState.ARCHIVED
        }
    }

    override suspend fun cleanupForecastDays() {
        withContext(Dispatchers.IO) {
            val yesterday = LocalDate.parse(Clock.System.now().toLocalDateTime(TimeZone.UTC).date.toString()).minus(1, DateTimeUnit.DAY).toString()
            val places = getAllSavedPlaces()
            places.forEach { place ->
                dbQuery.deleteDaysAfterDate(place, yesterday)
                Log.d("Database - Cleaned up forecast days after $yesterday for $place")
            }
        }
    }
}