package core

import com.km.rewinds.db.Day as DayDb
import com.km.rewinds.db.Hour as HourDb
import com.km.rewinds.db.WeatherResponse as WeatherResponseDb
import com.km.rewinds.db.WeatherStation as WeatherStationDb

// For mapping API data to DB model and vice versa
class DataMapping {

    // mapper from API response to db model
    fun fromWeatherResponse(response: WeatherResponse): WeatherResponseDb {
        val resolvedAddress = response.resolvedAddress ?: response.address
            ?: throw IllegalArgumentException("Both resolvedAddress and address are null")

        return WeatherResponseDb(
            resolvedAddress = resolvedAddress,
            queryCost = response.queryCost?.toLong(),
            latitude = response.latitude ?: Double.NaN,
            longitude = response.longitude ?: Double.NaN,
            address = response.address,
            timezone = response.timezone,
            tzoffset = response.tzoffset,
            // A freshly-mapped response is an active place; archiving is a separate DB update (KIM-364).
            archivedAt = null
        )
    }

    /**
     * Map DB row back to the API model, assembling the stations map from the joined WeatherStation rows.
     * Passing an empty list produces a null stations map (consistent with "no station data").
     */
    fun toWeatherResponse(
        dbResponse: WeatherResponseDb,
        days: List<Day>,
        stations: List<Station> = emptyList()
    ): WeatherResponse {
        val stationsMap: Map<String, Station>? = if (stations.isEmpty()) {
            null
        } else {
            stations.associateBy { it.id ?: it.name ?: "station_${stations.indexOf(it)}" }
        }

        return WeatherResponse(
            queryCost = dbResponse.queryCost?.toInt(),
            latitude = if (dbResponse.latitude == Double.NaN) null else dbResponse.latitude,
            longitude = if (dbResponse.longitude == Double.NaN) null else dbResponse.longitude,
            resolvedAddress = dbResponse.resolvedAddress,
            address = dbResponse.address,
            timezone = dbResponse.timezone,
            tzoffset = dbResponse.tzoffset,
            days = days,
            stations = stationsMap
        )
    }

    fun mapDbStationToStation(dbStation: WeatherStationDb): Station {
        return Station(
            id = dbStation.stationId,
            name = dbStation.name,
            latitude = dbStation.latitude,
            longitude = dbStation.longitude,
            distance = dbStation.distance,
            quality = dbStation.quality?.toInt(),
            useCount = dbStation.useCount?.toInt(),
            contribution = dbStation.contribution
        )
    }

    fun mapApiDayToDayDb(day: Day, weatherResponseResolvedAddress: String): DayDb {
        return DayDb(
            id = 0, // id is autoincremented
            weatherResponseResolvedAddress = weatherResponseResolvedAddress,
            datetime = day.datetime,
            datetimeEpoch = day.datetimeEpoch ?: 0,
            tempmax = day.tempmax,
            tempmin = day.tempmin,
            temp = day.temp,
            feelslikemax = day.feelslikemax,
            feelslikemin = day.feelslikemin,
            feelslike = day.feelslike,
            dew = day.dew,
            humidity = day.humidity,
            precip = day.precip,
            precipprob = day.precipprob,
            precipcover = day.precipcover,
            preciptype = day.preciptype,
            snow = day.snow,
            snowdepth = day.snowdepth,
            windgust = day.windgust,
            windspeed = day.windspeed,
            winddir = day.winddir,
            pressure = day.pressure,
            cloudcover = day.cloudcover,
            visibility = day.visibility,
            solarradiation = day.solarradiation,
            solarenergy = day.solarenergy,
            uvindex = day.uvindex,
            sunrise = day.sunrise,
            sunriseEpoch = day.sunriseEpoch,
            sunset = day.sunset,
            sunsetEpoch = day.sunsetEpoch,
            moonphase = day.moonphase,
            conditions = day.conditions,
            description = day.description,
            icon = day.icon
        )
    }

    fun mapDayDbToApiDay(dayDb: DayDb, hours: List<Hour>): Day {
        // ... mapping logic for DayDbType to Day
        return Day(
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
            icon = dayDb.icon,
            hours = hours,
            // skipping stations for now
            stations = null,
            source = null,
            normal = null
        )
    }

    fun mapHourDbToApiHour(hourDb: HourDb): Hour {
        return Hour(
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
            stations = null
        )
    }

    fun mapApiHourToHourDb(hour: Hour, dayId: Long): HourDb {
        return HourDb(
            id = 0, // id is autoincremented
            dayId = dayId,
            datetime = hour.datetime,
            datetimeEpoch = hour.datetimeEpoch,
            temp = hour.temp,
            feelslike = hour.feelslike,
            humidity = hour.humidity,
            dew = hour.dew,
            precip = hour.precip,
            precipprob = hour.precipprob,
            snow = hour.snow,
            snowdepth = hour.snowdepth,
            preciptype = hour.preciptype,
            windgust = hour.windgust,
            windspeed = hour.windspeed,
            winddir = hour.winddir,
            pressure = hour.pressure,
            visibility = hour.visibility,
            cloudcover = hour.cloudcover,
            solarradiation = hour.solarradiation,
            solarenergy = hour.solarenergy,
            uvindex = hour.uvindex,
            conditions = hour.conditions,
            icon = hour.icon,
            source = hour.source,
            stations = null // Keep it simple for now
        )
    }
}
