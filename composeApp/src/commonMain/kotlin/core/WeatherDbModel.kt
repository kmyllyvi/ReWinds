// NOTE: there's a way to use Serialization with RealmObject
// https://medium.com/realm/realm-kotlin-1-9-51cd32493b4c - but it looks at least as complicated as writing mappers API-DB-API
// Full list of custom serializers. Serializers for types
// not used in the model class can be safely removed.
//@file:UseSerializers(
//    MutableRealmIntKSerializer::class,
//    RealmAnyKSerializer::class,
//    RealmDictionaryKSerializer::class,
//    RealmInstantKSerializer::class,
//    RealmListKSerializer::class,
//    RealmSetKSerializer::class,
//    RealmUUIDKSerializer::class
//)

package core
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmObject

import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId


class WeatherResponseDb : RealmObject {

    // using this as unique ID, for example "Konstanz, Baden-Württemberg, Deutschland"
    @PrimaryKey
    var resolvedAddress: String = ""
    var queryCost: Int? = null
    var latitude: Double = Double.NaN
    var longitude: Double = Double.NaN

    var address: String? = null
    var timezone: String? = null
    var tzoffset: Double? = null
    var days: RealmList<DayDb> = realmListOf()
}

class DayDb : EmbeddedRealmObject {
    // @PrimaryKey
    // var id: ObjectId = ObjectId() // Unique ID for each Day object
    var datetime: String = ""
    var datetimeEpoch: Long = 0
    var tempmax: Double = Double.NaN
    var tempmin: Double = Double.NaN
    var temp: Double = Double.NaN
    var feelslikemax: Double = Double.NaN
    var feelslikemin: Double = Double.NaN
    var feelslike: Double = Double.NaN
    var dew: Double = Double.NaN
    var humidity: Double = Double.NaN
    var precip: Double = Double.NaN
    var precipprob: Double = Double.NaN
    var precipcover: Double = Double.NaN
    var preciptype: RealmList<String> = realmListOf()
    var snow: Double = Double.NaN
    var snowdepth: Double = Double.NaN
    var windgust: Double = Double.NaN
    var windspeed: Double = Double.NaN
    var winddir: Double = Double.NaN
    var pressure: Double = Double.NaN
    var cloudcover: Double = Double.NaN
    var visibility: Double = Double.NaN
    var solarradiation: Double = Double.NaN
    var solarenergy: Double = Double.NaN
    var uvindex: Double = Double.NaN
    var sunrise: String? = null
    var sunriseEpoch: Long = 0
    var sunset: String? = null
    var sunsetEpoch: Long = 0
    var moonphase: Double = Double.NaN
    var conditions: String? = null
    var description: String? = null
    var icon: String? = null
    // ... (rest of the fields from your Day data class)
    var hours: RealmList<HourDb>? = realmListOf()
}

class HourDb : EmbeddedRealmObject {
    // @PrimaryKey
    // var id: ObjectId = ObjectId()
    var datetime: String = ""
    var datetimeEpoch: Long? = null
    var temp: Double = Double.NaN
    var feelslike: Double = Double.NaN
    var humidity: Double = Double.NaN
    var dew: Double = Double.NaN
    var precip: Double = Double.NaN
    var precipprob: Double = Double.NaN
    var snow: Double = Double.NaN
    var snowdepth: Double = Double.NaN
    var windgust: Double = Double.NaN
    var windspeed: Double = Double.NaN
    var winddir: Double = Double.NaN
    var pressure: Double = Double.NaN
    var visibility: Double = Double.NaN
    var cloudcover: Double = Double.NaN
    var solarradiation: Double = Double.NaN
    var solarenergy: Double = Double.NaN
    var uvindex: Double = Double.NaN
    var conditions: String? = null
    var icon: String? = null
    var source: String? = null
    // ... (rest of the fields from your Hour data class)
    var preciptype: RealmList<String> = realmListOf() // Handle List<String>
    var stations: RealmList<StationDb> = realmListOf() // Handle List<String>
}

// stations can belong to many db objects (hence not EmbeddedRealmObject)
class StationDb : RealmObject {
    @PrimaryKey
    // var id: String = "" // Assuming 'id' is the unique identifier for stations
    var name: String? = ""
    var distance: Double? = null
    var latitude: Double? = null
    var longitude: Double? = null
    // ... (rest of the fields from your Station data class)
}