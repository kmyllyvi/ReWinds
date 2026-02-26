# ReWinds Codebase Architecture Memory

## Key Architecture Decisions
- Compose Multiplatform (iOS + Android) with shared Kotlin in `composeApp/src/commonMain`
- Pattern: MV* with Koin DI, ViewModel per screen, unidirectional state flow
- Navigation: custom back-stack (`mutableStateListOf<NavRoute>`) in `Router.kt`, no Compose Navigation library
- Database: SQLDelight with `resolvedAddress TEXT NOT NULL PRIMARY KEY` as the place identity key

## Critical: Place Identity Design
Places have NO numeric ID. The primary key for a place across all layers is the `resolvedAddress` string.
- DB table: `WeatherResponse.resolvedAddress` (PRIMARY KEY)
- All DB queries, navigation routes, and API calls use this string as the place identifier
- This is the source of the "Helsinki Airport" vs Spanish place bug risk

## Place Selection -> API Call Data Flow
See `place-data-flow.md` for the full trace.

## Key File Locations
- Navigation routes: `core/NavigationRoutes.kt`
- Router/nav: `core/Router.kt`, `core/NavigatorImpl.kt`
- DI setup: `DI.kt` (root level)
- DB schema: `composeApp/src/commonMain/sqldelight/com/km/rewinds/db/AppDatabase.sq`
- Place addition: `WeatherRepository.addPlaceFromSearch()` in `core/WeatherRepository.kt`
- Download trigger: `PlaceSummaryViewModel.onDownloadFullMonth()` in `place/PlaceSummaryViewModel.kt`
- API URL construction: `WeatherRepositoryImpl.fetchWeatherFromNetwork()` in `core/WeatherRepository.kt`
