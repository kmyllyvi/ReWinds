This is a Kotlin Multiplatform project targeting Android, iOS.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.


About the app


== Method

The application architecture is built around Kotlin Multiplatform (KMP), enabling shared business logic across Android and iOS. It leverages Visual Crossing as the external provider for historical weather data, and Realm DB for local persistence of queries and results.

=== Architecture Overview

[plantuml]
----
@startuml
package "Shared KMP Layer" {
[WeatherRepository] --> [VisualCrossingService]
[WeatherRepository] --> [RealmWeatherStorage]
[SavedPlacesRepository] --> [RealmPlaceStorage]
}

package "Android/iOS" {
[SearchViewModel]
[SavedPlacesViewModel]
[SearchViewModel] --> [WeatherRepository]
[SavedPlacesViewModel] --> [SavedPlacesRepository]
}

[VisualCrossingService] --> [Visual Crossing API]
@enduml
----

- **SearchViewModel / SavedPlacesViewModel**: Platform-specific ViewModels that bind to UI.
- **WeatherRepository**: Core logic to query, cache, and normalize weather data.
- **RealmWeatherStorage**: Caches weather responses.
- **SavedPlacesRepository**: Manages user-defined saved places and time ranges.

=== Database Schema (Realm)

```kotlin
class Place : RealmObject {
    var id: String = UUID.randomUUID().toString()
    var name: String = ""
    var lat: Double = 0.0
    var lon: Double = 0.0
    var savedQueries: RealmList<SavedQuery> = RealmList()
}

class SavedQuery : RealmObject {
    var startDate: String = "" // ISO 8601 format
    var endDate: String = ""
    var weatherSnapshots: RealmList<WeatherSnapshot> = RealmList()
}

class WeatherSnapshot : RealmObject {
    var date: String = ""
    var tempMin: Double = 0.0
    var tempMax: Double = 0.0
    var precipitation: Double = 0.0
    var conditions: String = ""
}