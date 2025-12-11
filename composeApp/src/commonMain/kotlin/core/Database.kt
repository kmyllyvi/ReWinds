package core

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
// kotlinx.coroutines.CoroutineScope removed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO // Keep for Dispatchers.IO
// kotlinx.coroutines.launch removed
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import kotlinx.coroutines.withContext // Added import
import kotlin.collections.addAll

interface Database {
    // get all saved places
    suspend fun getAllSavedPlaces(): List<String>
    // get full data for a specific place
    suspend fun getSavedPlaceFull(place: String): WeatherResponse?
    // save new data
    suspend fun saveWeatherResponse(weatherResponse: WeatherResponse) // MODIFIED: now suspend
    // get data for a specific place and date
    fun getWeatherDataFor(placeName: String, date: String): WeatherResponseDb?
    // delete a place
    suspend fun deletePlace(placeName: String)
}

class RealmDatabase : Database {

    val dataMapping = DataMapping()

    private val realm: Realm by lazy {
        // define complete schema here
        val configuration = RealmConfiguration.Builder(schema = setOf(WeatherResponseDb::class, DayDb::class, HourDb::class, StationDb::class))
            .schemaVersion(1) // Set the initial schema version
            // WARN! REMOVE WHEN PROD AND MIGRATE DB
            .deleteRealmIfMigrationNeeded() // Delete the Realm if a migration is needed
            .build()// RealmConfiguration.create(schema = setOf(WeatherResponseDb::class, DayDb::class, HourDb::class, StationDb::class))
        Realm.open(configuration)
    }

    override suspend fun getAllSavedPlaces(): List<String> {
            return realm.query<WeatherResponseDb>()
                .distinct("resolvedAddress")
                .find()
                .asSequence()
                .map { it.resolvedAddress }
                .toList()
    }

    override suspend fun getSavedPlaceFull(place: String): WeatherResponse? {
        return getExistingPlace(place)?.let { dataMapping.toWeatherResponse(it) }
    }

    override suspend fun saveWeatherResponse(weatherResponse: WeatherResponse) { // MODIFIED: now suspend
        val weatherResponseDb = dataMapping.fromWeatherResponse(weatherResponse)
        // MODIFIED: Use withContext to ensure the block completes before the function returns
        withContext(Dispatchers.IO) { // Switch to IO dispatcher for database operations
            realm.write { // realm.write is often a suspend function or should be treated as blocking
                // this : MutableRealm
                val existingPlace = getExistingPlace(weatherResponse.resolvedAddress, this)
                if (existingPlace != null) {
                    Log.d("WeatherRepository - Updating existing place: ${existingPlace.resolvedAddress} - first new day: ${weatherResponseDb.days.firstOrNull()?.datetime}")
                    // Update existing place with new days, prevent adding an existing date
                    val existingDateTimes = existingPlace.days.map { it.datetime }.toSet() // Use a Set for faster lookups
                    val newDaysToAdd = weatherResponseDb.days.filter { it.datetime !in existingDateTimes }
                    
                    if (newDaysToAdd.isNotEmpty()) {
                        Log.d("old days count for ${existingPlace.resolvedAddress}: ${existingPlace.days.count()}")
                        existingPlace.days.addAll(newDaysToAdd)
                        Log.d("new days count for ${existingPlace.resolvedAddress}: ${existingPlace.days.count()}")
                    } else {
                        Log.d("No new days to add for ${existingPlace.resolvedAddress}")
                    }
                } else {
                    Log.d("WeatherRepository - Copying new place: ${weatherResponseDb.resolvedAddress}")
                    copyToRealm(weatherResponseDb)
                }
            }
            Log.d("WeatherRepository - Saved weather response for: ${weatherResponseDb.resolvedAddress}")
        }
    }

    override fun getWeatherDataFor(placeName: String, date: String): WeatherResponseDb? {
            return realm.query<WeatherResponseDb>("address == $0", placeName)
                .first()
                .find()
                ?.let { weatherResponseDb ->
                    if (weatherResponseDb.days.any { it.datetime == date }) {
                        weatherResponseDb
                    } else {
                        null
                    }
                }
    }

    override suspend fun deletePlace(placeName: String) {
        withContext(Dispatchers.IO) {
            realm.write {
                val placeToDelete = query<WeatherResponseDb>("resolvedAddress == $0", placeName).first().find()
                placeToDelete?.also { findLatest(it)?.also { delete(it) } }
            }
        }
    }

    // Helper function to get existing place. MutableRealm is needed when updating existing place.
    private fun getExistingPlace(resolvedAddress: String, mutableRealm: MutableRealm? = null): WeatherResponseDb? {
        val realm = mutableRealm ?: this.realm
        return realm.query<WeatherResponseDb>("resolvedAddress == $0", resolvedAddress).first().find()
    }
}

// Extension function to convert a List to RealmList
fun <T> List<T>.toRealmList(): RealmList<T> {
    return realmListOf<T>().also { it.addAll(this) }
}
