# Plan: Flexible AI Weather Queries (Any Database Field)

## Problem
Currently the AI only understands wind-related questions because tools are hardcoded:
- `get_wind_summary` - only returns wind data
- `get_monthly_stats` - only wind stats
- `get_best_days` - filter by wind/rain only

The database has 30+ fields (visibility, humidity, temperature, pressure, cloud cover, UV index, etc.) but the AI can't access them. User should be able to ask "What was visibility in Feb 26 at Hawaii?" and Claude should understand that `visibility` maps to a database field.

## Solution: Generic Metric-Based Tools

Replace wind-specific tools with flexible ones that work with ANY available metric.

### New Tool: `get_weather_metrics`

```kotlin
object GetWeatherMetrics : Tool() {
    override val name = "get_weather_metrics"
    override val description =
        """Retrieve weather data for a location and date range.
        |Can fetch ANY metric: temperature, visibility, humidity, rainfall,
        |wind, cloud cover, pressure, UV index, solar radiation, and more.
        |
        |Available metrics: temperature (Celsius), visibility (km), humidity (%),
        |rainfall (mm), wind_speed (knots), wind_gust (knots), cloud_cover (%),
        |pressure (hPa), uv_index, solar_energy (MJ/m²), snow (cm), dew_point (°C)
        """

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("location_name") {
                put("type", "string")
                put("description", "Name of the saved place")
            }
            putJsonObject("start_date") {
                put("type", "string")
                put("description", "ISO date YYYY-MM-DD")
            }
            putJsonObject("end_date") {
                put("type", "string")
                put("description", "ISO date YYYY-MM-DD")
            }
            putJsonObject("metrics") {
                put("type", "array")
                put("items", buildJsonObject {
                    put("type", "string")
                })
                put("description",
                    "List of metrics to retrieve: 'temperature', 'visibility', " +
                    "'humidity', 'rainfall', 'wind_speed', 'wind_gust', " +
                    "'cloud_cover', 'pressure', 'uv_index', 'solar_energy', " +
                    "'snow', 'dew_point', 'conditions', 'min_temp', 'max_temp'")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("location_name"))
            add(JsonPrimitive("start_date"))
            add(JsonPrimitive("end_date"))
            add(JsonPrimitive("metrics"))
        }
    }
}
```

### Keep Existing Tools (Specialized)

For backwards compatibility and because they're useful shortcuts:
- `list_saved_places` - unchanged
- `get_monthly_stats` - keep but extend to any metric
- `get_best_days` - keep for filtering by complex criteria

### Data Conversion Layer

Create a `MetricMapper` to translate user-friendly metric names to database fields:

```kotlin
object MetricMapper {
    fun mapMetricName(metricName: String): String = when (metricName.lowercase()) {
        "temperature", "temp" -> "temp"
        "temperature_max", "temp_max" -> "tempmax"
        "temperature_min", "temp_min" -> "tempmin"
        "feels_like", "feelslike" -> "feelslike"
        "dew_point", "dew" -> "dew"
        "humidity" -> "humidity"
        "rainfall", "rain", "precipitation", "precip" -> "precip"
        "rain_probability", "precip_prob" -> "precipprob"
        "snow" -> "snow"
        "wind_speed", "windspeed", "wind" -> "windspeed"
        "wind_gust", "windgust", "gust" -> "windgust"
        "wind_direction", "winddir" -> "winddir"
        "visibility", "visible" -> "visibility"
        "cloud_cover", "cloudcover", "clouds" -> "cloudcover"
        "pressure" -> "pressure"
        "uv_index", "uv" -> "uvindex"
        "solar_energy", "solar_radiation", "solarenergy" -> "solarenergy"
        "conditions" -> "conditions"
        "description" -> "description"
        else -> metricName  // fallback: use as-is (for fields like "moonphase")
    }

    fun getUnits(fieldName: String): String = when (fieldName) {
        "temp", "tempmax", "tempmin", "feelslike", "feelslikemax", "feelslikemin", "dew" -> "°C"
        "humidity", "precipprob", "cloudcover" -> "%"
        "precip", "snow" -> "mm"
        "windspeed", "windgust" -> "knots"
        "pressure" -> "hPa"
        "visibility" -> "km"
        "solarenergy" -> "MJ/m²"
        else -> ""
    }
}
```

## Handler: `handleGetWeatherMetrics`

```kotlin
private suspend fun handleGetWeatherMetrics(
    args: JsonObject,
    repo: WeatherRepository
): String = try {
    val locationName = args["location_name"]?.jsonPrimitive?.content ?: ...
    val startDate = args["start_date"]?.jsonPrimitive?.content ?: ...
    val endDate = args["end_date"]?.jsonPrimitive?.content ?: ...
    val metricsList = args["metrics"]?.jsonArray?.map {
        it.jsonPrimitive.content
    } ?: emptyList()

    // Fetch raw data once
    val weatherResponse = repo.getDaysRange(locationName, startDate, endDate)

    // Transform to requested metrics
    val results = weatherResponse.days?.map { day ->
        buildJsonObject {
            put("date", day.datetime)

            for (metricName in metricsList) {
                val fieldName = MetricMapper.mapMetricName(metricName)
                val value = day.getField(fieldName)  // reflection or when-branch
                val units = MetricMapper.getUnits(fieldName)

                if (value != null) {
                    val displayName = metricName.replace("_", " ")
                    val displayValue = if (fieldName in listOf("windspeed", "windgust")) {
                        (value as Double * 1.944).roundTo(1)  // convert m/s to knots
                    } else {
                        value
                    }
                    put("$metricName$units", displayValue)
                }
            }
        }
    } ?: emptyList()

    buildJsonObject {
        put("place", weatherResponse.resolvedAddress)
        put("date_range", "$startDate to $endDate")
        put("metrics_requested", metricsList)
        putJsonArray("data") {
            results.forEach { add(it) }
        }
    }.toString()
}
```

## Alternative: Even More Flexible

**Option 1** (current proposal): Named metrics with mapping
- Pros: Type-safe, user-friendly ("visibility" not "visibility_km")
- Cons: Need to maintain metric name mappings

**Option 2**: Raw field access
```kotlin
// Claude asks directly: "get_weather_metrics with fields=['visibility', 'temp', 'cloudcover']"
// Pro: No mapping layer needed
// Con: Claude needs to know exact field names from database
```

**Option 3**: Hybrid
```kotlin
// Support both: Claude can use friendly names ("visibility") or raw names ("visibility")
// The mapper tries friendly first, falls back to raw name
fun mapMetricName(input: String): String {
    val mapped = friendlyNameMap[input.lowercase()]
    return mapped ?: input  // use raw field name if not in map
}
```

## Example Conversations

### User: "What was the visibility in Feb 26 at Oahu?"
Claude calls:
```json
{
  "tool": "get_weather_metrics",
  "location_name": "Oahu",
  "start_date": "2026-02-26",
  "end_date": "2026-02-26",
  "metrics": ["visibility"]
}
```
Result: `{"place": "Oahu", "data": [{"date": "2026-02-26", "visibility km": 14.5}]}`

### User: "Compare average temperatures in Jan vs Feb at Hawaii"
Claude calls twice:
```json
{
  "tool": "get_weather_metrics",
  "location_name": "Hawaii",
  "start_date": "2026-01-01",
  "end_date": "2026-01-31",
  "metrics": ["temperature"]
}
```
Then aggregates and reports: "January avg: 23.4°C, February avg: 24.1°C"

### User: "Find days with good wind and low visibility"
Claude calls:
```json
{
  "tool": "get_weather_metrics",
  "location_name": "Maui",
  "start_date": "2026-02-01",
  "end_date": "2026-02-28",
  "metrics": ["wind_speed", "wind_gust", "visibility", "cloud_cover"]
}
```
Then filters in response based on criteria

## Implementation Steps

1. **Create `MetricMapper.kt`** - Map friendly names to database fields
2. **Create `GetWeatherMetrics` tool** - New tool definition with schema
3. **Create `handleGetWeatherMetrics` handler** - Query and transform logic
4. **Update `WeatherTools.allToolSchemas()`** - Add new tool
5. **Update `WeatherTools.handleToolCall()`** - Route to new handler
6. **Simplify `GetMonthlyStats`** - Extend to accept any metric
7. **Test with examples** - "visibility", "humidity", "temperature comparison", etc.

## Benefits

✅ Claude can reason about ANY weather metric in the database
✅ User-friendly metric names (no schema knowledge needed)
✅ Backwards compatible (old tools still work)
✅ Easily extendable (add new metrics to mapper)
✅ Type-safe (mapping prevents invalid field access)

## Files to Modify

| File | Change |
|------|--------|
| `composeApp/src/commonMain/kotlin/ai/WeatherTools.kt` | Add `GetWeatherMetrics`, create `MetricMapper`, add handler |
| (optional) `composeApp/src/commonMain/kotlin/ai/AnthropicClient.kt` | Update system prompt to mention metric flexibility |
