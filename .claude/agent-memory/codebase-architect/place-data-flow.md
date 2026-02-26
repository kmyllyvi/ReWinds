# Place Selection to API Call: Full Data Flow

## 1. Adding a Place (Search -> Save)

```
HomeView
  -> user types in OutlinedTextField
  -> HomeViewModel.onSearchTextChange(text)
  -> debounced 500ms -> WeatherRepository.searchForLocations(query)
     -> GET https://geocoding-api.open-meteo.com/v1/search?name={query}
     -> returns List<GeoSearchResult> (id: Int, name, latitude, longitude, country, region)

  -> user taps suggestion
  -> HomeViewModel.onSearchResultSelected(place: GeoSearchResult)
     -> WeatherRepository.addPlaceFromSearch(place)
        -> locationString = "${place.latitude}%2C${place.longitude}"   // KEY: uses lat/lon
        -> fetchWeatherFromNetwork(locationString, 0)
           -> URL: https://weather.visualcrossing.com/.../60.3172%2C24.9633/last0days?...
        -> weatherResponse comes back with API-assigned resolvedAddress (e.g. "Helsinki, Finland")
        -> correctedResponse = weatherResponse.copy(
               address = place.name,        // overwritten with search result name
               resolvedAddress = place.name // overwritten with search result name (e.g. "Helsinki Airport")
           )
        -> database.saveWeatherResponse(correctedResponse)
           -> stored with resolvedAddress = place.name (e.g. "Helsinki Airport")
```

## 2. Navigating to Place Summary

```
HomeView PlaceCell.onClick
  -> HomeViewModel.onSavedPlaceSelected(placeName: String)
     -> sends NavigationEvent.ToPlaceSummary(placeName)
     -> HomeView LaunchedEffect collects it
     -> navigator.navigateToPlaceSummary(event.placeName)
        -> backStack.add(PlaceSummaryRoute(placeName))

Router.kt
  -> currentRoute is PlaceSummaryRoute
  -> PlaceSummaryView(route = currentRoute, ...)
     -> koinViewModel(key = route.placeName) { parametersOf(route) }
        -> creates PlaceSummaryViewModel(route, weatherRepository)
           -> placeName = route.placeName  // String passed through route
```

## 3. Downloading Missing Month Data

```
PlaceSummaryView
  -> user taps a grey (missing data) month card
  -> onPromptForMissingDays(year, month, missingDays) sets dialog state
  -> user taps "Download" in dialog
  -> viewModel.onDownloadFullMonth(yearToDownloadForDialog!!, monthToDownloadForDialog!!)

PlaceSummaryViewModel.onDownloadFullMonth(year, month)
  -> weatherRepository.getDaysRange(placeName, startDateString, endDateString)
     // placeName is vm.placeName = route.placeName = the string from DB

WeatherRepositoryImpl.getDaysRange(place: String, fromDate, toDate)
  -> fetchWeatherFromNetwork(place, gap.startDate, gap.endDate)
     -> requestUrl = "$visualcrossingUrl$place/$fromDate/$toDate$apiQuery"
     // place IS the resolvedAddress string used directly in URL
     -> networkService.fetchWeatherData(requestUrl)
```

## The Critical Bug: Where Wrong Place Data Gets Fetched

The entire system uses `resolvedAddress` (a plain string) as the place identity.
When downloading, that string is inserted directly into the Visual Crossing API URL path.

Visual Crossing API performs its own geocoding on the location string in the URL.

Example of the bug:
- User added "Helsinki Airport" via geo-search (lat: 60.3172, lon: 24.9633)
- Saved with resolvedAddress = "Helsinki Airport"
- When downloading, URL becomes: `.../Helsinki Airport/2024-01-01/2024-01-31?...`
- Visual Crossing geocodes "Helsinki Airport" as a text query
- It may resolve to a Spanish place that also has "Helsinki" in its name, or to a different location
- The initial add used lat/lon coordinates (correct), but subsequent downloads use the name string (incorrect)

## The Asymmetry (Root Cause)

| Operation | Location Sent to API | How |
|-----------|---------------------|-----|
| addPlaceFromSearch (first add) | `"60.3172%2C24.9633"` (lat/lon) | From GeoSearchResult fields |
| getDaysRange (download) | `"Helsinki Airport"` (name string) | From DB resolvedAddress |

The fix would be to store lat/lon coordinates alongside the place and use them in download requests
instead of the name string. The WeatherResponse DB already has `latitude` and `longitude` columns,
so the data is available - it just is not used when constructing download URLs.
