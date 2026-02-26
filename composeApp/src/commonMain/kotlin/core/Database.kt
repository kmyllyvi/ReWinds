package core

import app.cash.sqldelight.db.SqlDriver
import com.km.rewinds.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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

interface Database {
    // get all saved places
    suspend fun getAllSavedPlaces(): List<String>

    // get full data for a specific place
    suspend fun getSavedPlaceFull(place: String): WeatherResponse?

    // save new data
    suspend fun saveWeatherResponse(weatherResponse: WeatherResponse)

    // get data for a specific place and date
    suspend fun getWeatherDataFor(placeName: String, date: String): WeatherResponse?

    // delete a place
    suspend fun deletePlace(placeName: String)

    // cleanup forecast data: remove any days after yesterday
    suspend fun cleanupForecastDays()
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

            dataMapping.toWeatherResponse(dbResponse, days)
        }
    }

    override suspend fun saveWeatherResponse(weatherResponse: WeatherResponse) {
        withContext(Dispatchers.IO) {
            dbQuery.transaction {
                val dbResponse = dataMapping.fromWeatherResponse(weatherResponse)
                val existing = dbQuery.getWeatherResponseByResolvedAddress(dbResponse.resolvedAddress).executeAsOneOrNull()

                if (existing == null) {
                    // New place, insert everything
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
                    // Existing place, only add new days
                    val existingDays = dbQuery.getDaysForWeatherResponse(dbResponse.resolvedAddress).executeAsList()
                    val existingDateTimes = existingDays.map { it.datetime }.toSet()
                    val newDays = weatherResponse.days?.filter { it.datetime !in existingDateTimes } ?: emptyList()

                    newDays.forEach { day ->
                        insertDayAndHours(day, dbResponse.resolvedAddress)
                    }
                }
            }
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