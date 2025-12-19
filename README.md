This is a Kotlin Multiplatform project targeting Android, iOS.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.


## About ReWinds

**ReWinds** is a weather history tracking application that allows users to search for locations and view detailed historical weather data. The app is designed with wind sports enthusiasts in mind (kitesurfing/windsurfing), featuring advanced wind metrics including sustained wind speed calculations.

### Tech Stack

- **Kotlin Multiplatform (KMP)** - Shared business logic across Android and iOS
- **Jetpack Compose Multiplatform** - Modern declarative UI framework
- **SQLDelight** - Type-safe SQL database for local persistence
- **Ktor** - HTTP client for API networking
- **Koin** - Dependency injection framework
- **Visual Crossing API** - Weather data provider
- **Navigation Compose** - Type-safe navigation
- **Kotlinx Serialization** - JSON serialization/deserialization
- **Kotlinx DateTime** - Multiplatform date/time handling

### Key Features

1. **Location Search** - Search and add locations via geolocation search
2. **Saved Places** - Manage multiple saved locations with persistent weather data
3. **Weather Summaries** - View detailed day-by-day weather summaries for each place
4. **Monthly Statistics** - Comprehensive monthly weather breakdowns and analytics
5. **Historical Data Download** - Fetch complete month ranges of historical weather data
6. **Rich Weather Metrics**:
   - Temperature (min/max/average)
   - Wind data (average speed, gusts, sustained wind speed)
   - Precipitation and precipitation type
   - Humidity, dew point, pressure
   - Cloud cover, visibility
   - UV index, solar radiation
   - Sunrise/sunset times, moon phase

### Architecture Overview

The application follows the MVVM (Model-View-ViewModel) pattern:

```
┌─────────────────────────────────────────┐
│          Compose UI Layer               │
│  (HomeView, PlaceSummaryView, etc.)     │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         ViewModel Layer                 │
│  (HomeViewModel, PlaceSummaryViewModel) │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│        Repository Layer                 │
│      (WeatherRepository)                │
└────────┬──────────────────────┬─────────┘
         │                      │
┌────────▼─────────┐   ┌────────▼─────────┐
│  NetworkService  │   │  SQLDelight DB   │
│  (Ktor Client)   │   │   (Local Cache)  │
└────────┬─────────┘   └──────────────────┘
         │
┌────────▼─────────┐
│ Visual Crossing  │
│      API         │
└──────────────────┘
```

**Key Components**:
- **HomeViewModel**: Manages location search and saved places list
- **PlaceSummaryViewModel**: Displays weather summary for a specific location
- **MonthlyStatisticsViewModel**: Provides monthly weather analytics
- **WeatherRepository**: Orchestrates data fetching, caching, and retrieval
- **NetworkService**: Handles HTTP communication with Visual Crossing API
- **SQLDelight Database**: Local persistence layer

### Database Schema (SQLDelight)

The app uses three main tables with cascading relationships:

**WeatherResponse** - Stores location metadata
- resolvedAddress (PRIMARY KEY)
- latitude, longitude
- address, timezone, tzoffset
- queryCost

**Day** - Daily weather summaries
- weatherResponseResolvedAddress (FOREIGN KEY)
- datetime, datetimeEpoch
- Temperature data (tempmax, tempmin, temp, feels-like values)
- Wind data (windspeed, windgust, winddir)
- Precipitation (precip, precipprob, precipcover, preciptype, snow)
- Atmospheric data (humidity, pressure, dew, cloudcover, visibility)
- Solar data (solarradiation, solarenergy, uvindex)
- Sunrise/sunset times and moon phase
- conditions, description, icon

**Hour** - Hourly weather data for detailed analysis
- dayId (FOREIGN KEY)
- datetime, datetimeEpoch
- Temperature and feels-like
- Wind, precipitation, and atmospheric metrics
- Solar radiation and UV index
- conditions, icon