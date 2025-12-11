package core

import io.realm.kotlin.ext.realmListOf
import org.mongodb.kbson.ObjectId

// For mapping API data to DB model and vice versa
class DataMapping {

    // mapper from API response to db model
    fun fromWeatherResponse(response: WeatherResponse): WeatherResponseDb {
        // Ensure that the required fields are not null
        val resolvedAddress = response.resolvedAddress ?: response.address
        ?: throw IllegalArgumentException("Both resolvedAddress and address are null")

        return WeatherResponseDb().apply {
            queryCost = response.queryCost
            latitude = response.latitude ?: Double.NaN
            longitude = response.longitude ?: Double.NaN
            this.resolvedAddress = resolvedAddress
            address = response.address
            timezone = response.timezone
            days = response.days?.map { mapApiDayToDayDbType(it) }?.toRealmList() ?: realmListOf()
        }
    }
    // mapper from db model to API response

    fun toWeatherResponse(dbResponse: WeatherResponseDb): WeatherResponse {
        return WeatherResponse(
            queryCost = dbResponse.queryCost,
            latitude = if (dbResponse.latitude == Double.NaN) null else dbResponse.latitude,
            longitude = if (dbResponse.longitude == Double.NaN) null else dbResponse.longitude,
            resolvedAddress = dbResponse.resolvedAddress,
            address = dbResponse.address,
            timezone = dbResponse.timezone,
            tzoffset = dbResponse.tzoffset,
            days = dbResponse.days.map { mapDayDbTypeToApiDay(it) },
            stations= null // Not included in the API response
        )
    }

    private fun mapApiDayToDayDbType(day: Day): DayDb {
        return DayDb().apply {
            // id = ObjectId() // Generate a unique ID if needed
            datetime = day.datetime
            datetimeEpoch = day.datetimeEpoch ?: 0
            tempmax = day.tempmax ?: Double.NaN
            tempmin = day.tempmin ?: Double.NaN
            temp = day.temp ?: Double.NaN
            feelslikemax = day.feelslikemax ?: Double.NaN
            feelslikemin = day.feelslikemin ?: Double.NaN
            feelslike = day.feelslike ?: Double.NaN
            dew = day.dew ?: Double.NaN
            humidity = day.humidity ?: Double.NaN
            precip = day.precip ?: Double.NaN
            precipprob = day.precipprob ?: Double.NaN
            precipcover = day.precipcover ?: Double.NaN
            preciptype = day.preciptype?.toRealmList() ?: realmListOf()
            snow = day.snow ?: Double.NaN
            snowdepth = day.snowdepth ?: Double.NaN
            windgust = day.windgust ?: Double.NaN
            windspeed = day.windspeed ?: Double.NaN
            winddir = day.winddir ?: Double.NaN
            pressure = day.pressure ?: Double.NaN
            cloudcover = day.cloudcover ?: Double.NaN
            visibility = day.visibility ?: Double.NaN
            solarradiation = day.solarradiation ?: Double.NaN
            solarenergy = day.solarenergy ?: Double.NaN
            uvindex = day.uvindex ?: Double.NaN
            sunrise = day.sunrise
            sunriseEpoch = day.sunriseEpoch ?: 0
            sunset = day.sunset
            sunsetEpoch = day.sunsetEpoch ?: 0
            moonphase = day.moonphase ?: Double.NaN
            conditions = day.conditions
            description = day.description
            icon = day.icon
            // ... map other properties
            hours = day.hours?.map { mapApiHourToHourDbType(it) }?.toRealmList() ?: realmListOf()
        }
    }

    private fun mapDayDbTypeToApiDay(dayDbType: DayDb): Day {
        // ... mapping logic for DayDbType to Day
        return Day(
            datetime = dayDbType.datetime,
            datetimeEpoch = dayDbType.datetimeEpoch,
            tempmax = dayDbType.tempmax,
            tempmin = dayDbType.tempmin,
            temp = dayDbType.temp,
            feelslikemax = dayDbType.feelslikemax,
            feelslikemin = dayDbType.feelslikemin,
            feelslike = dayDbType.feelslike,
            dew = dayDbType.dew,
            humidity = dayDbType.humidity,
            precip = dayDbType.precip,
            precipprob = dayDbType.precipprob,
            precipcover = dayDbType.precipcover,
            preciptype = dayDbType.preciptype?.toList(),
            snow = dayDbType.snow,
            snowdepth = dayDbType.snowdepth,
            windgust = dayDbType.windgust,
            windspeed = dayDbType.windspeed,
            winddir = dayDbType.winddir,
            pressure = dayDbType.pressure,
            cloudcover = dayDbType.cloudcover,
            visibility = dayDbType.visibility,
            solarradiation = dayDbType.solarradiation,
            solarenergy = dayDbType.solarenergy,
            uvindex = dayDbType.uvindex,
            sunrise = dayDbType.sunrise,
            sunriseEpoch = dayDbType.sunriseEpoch,
            sunset = dayDbType.sunset,
            sunsetEpoch = dayDbType.sunsetEpoch,
            moonphase = dayDbType.moonphase,
            conditions = dayDbType.conditions,
            description = dayDbType.description,
            icon = dayDbType.icon,
            hours = dayDbType.hours?.map { mapHourDbTypeToApiHour(it) },
            // skipping stations for now
            stations = null,
            source = null,
            normal = null
        )
    }

    private fun mapHourDbTypeToApiHour(hourDbType: HourDb): Hour {
        return Hour(
            datetime = hourDbType.datetime,
            datetimeEpoch = hourDbType.datetimeEpoch,
            temp = hourDbType.temp,
            feelslike = hourDbType.feelslike,
            humidity = hourDbType.humidity,
            dew = hourDbType.dew,
            precip = hourDbType.precip,
            precipprob = hourDbType.precipprob,
            snow = hourDbType.snow,
            snowdepth = hourDbType.snowdepth,
            preciptype = hourDbType.preciptype.toList(),
            windgust = hourDbType.windgust,
            windspeed = hourDbType.windspeed,
            winddir = hourDbType.winddir,
            pressure = hourDbType.pressure,
            visibility = hourDbType.visibility,
            cloudcover = hourDbType.cloudcover,
            solarradiation = hourDbType.solarradiation,
            solarenergy = hourDbType.solarenergy,
            uvindex = hourDbType.uvindex,
            conditions = hourDbType.conditions,
            icon = hourDbType.icon,
            source = hourDbType.source,
            stations = null
        )
    }

    private fun mapApiHourToHourDbType(hour: Hour): HourDb {
        return HourDb().apply {
            // id = ObjectId()
            datetime = hour.datetime
            datetimeEpoch = hour.datetimeEpoch
            temp = hour.temp ?: Double.NaN
            feelslike = hour.feelslike ?: Double.NaN
            humidity = hour.humidity ?: Double.NaN
            dew = hour.dew ?: Double.NaN
            precip = hour.precip ?: Double.NaN
            precipprob = hour.precipprob ?: Double.NaN
            snow = hour.snow ?: Double.NaN
            snowdepth = hour.snowdepth ?: Double.NaN
            windgust = hour.windgust ?: Double.NaN
            windspeed = hour.windspeed ?: Double.NaN
            winddir = hour.winddir ?: Double.NaN
            pressure = hour.pressure ?: Double.NaN
            visibility = hour.visibility ?: Double.NaN
            cloudcover = hour.cloudcover ?: Double.NaN
            solarradiation = hour.solarradiation ?: Double.NaN
            solarenergy = hour.solarenergy ?: Double.NaN
            uvindex = hour.uvindex ?: Double.NaN
            conditions = hour.conditions
            icon = hour.icon
            source = hour.source
            preciptype = hour.preciptype?.toRealmList() ?: realmListOf()
            // skipping stations for now
            // stations = hour.stations.values.map { mapApiStationToStationDbType(it) }.toRealmList()
            stations = realmListOf()
        }
    }

    private fun mapApiStationToStationDbType(station: Station): StationDb {
        return StationDb().apply {
            // id = station.id.toString()
            name = station.name
            distance = station.distance
            latitude = station.latitude
            // ... map other properties
        }
    }
}