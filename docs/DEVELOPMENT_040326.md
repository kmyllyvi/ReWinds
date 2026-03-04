# Development Session - March 4, 2026

**Date**: March 4, 2026
**Status**: 🎯 Planning Phase - Flexible AI Weather Queries
**Platform**: Android + iOS (design phase)

---

## Session Goal

Make the AI chat feature **flexible and data-aware**: Allow Claude to query ANY weather metric from the database (visibility, humidity, temperature, rainfall, etc.) instead of being limited to wind-specific questions.

### Problem Statement
Currently, the AI tools only understand wind-related queries:
- "What were the wind conditions?" ✅
- "Find best days with 15-25 knots wind" ✅
- "What was the visibility?" ❌ (no tool for this)
- "Compare temperatures in Jan vs Feb" ❌ (no tool for this)

The database contains 30+ weather fields but Claude can only access 4 wind-focused tools.

### Solution
Redesign tools from specific (wind-only) to generic (metric-based):
- **New tool**: `get_weather_metrics` - accepts any metric(s) and date range
- **MetricMapper**: Translates user-friendly names to database fields
- **Smart responses**: Claude reasons about which metrics to query based on user question

---

## Design Documents Created

### 1. **PLAN_iOS_Keychain_API_Key_Storage.md**
iOS-specific plan for secure API key storage. Kept for future implementation.

### 2. **PLAN_Flexible_AI_Weather_Queries.md** ⭐ THIS SESSION
Comprehensive design for flexible metric-based tools:
- Tool schema for `get_weather_metrics`
- MetricMapper for friendly name translation
- Handler logic for fetching and formatting data
- Example conversations (visibility, temperature comparisons, etc.)
- Three implementation approaches (named metrics, raw fields, hybrid)

---

## Implementation Plan

### Phase 1: Core Tool Infrastructure
- [ ] Create `MetricMapper.kt` - Maps "visibility" → "visibility", "rain" → "precip", etc.
- [ ] Add `GetWeatherMetrics` tool to `WeatherTools.kt`
- [ ] Create `handleGetWeatherMetrics()` handler
- [ ] Register new tool in `allToolSchemas()` and `handleToolCall()`

### Phase 2: Keep & Extend Existing Tools
- [ ] Keep `ListSavedPlaces` (unchanged)
- [ ] Extend `GetMonthlyStats` to work with any metric
- [ ] Keep `GetBestDays` for backwards compatibility (optimized filtering)
- [ ] Optional: Deprecate `GetWindSummary` (covered by new generic tool)

### Phase 3: System Prompt Updates
- [ ] Update AnthropicClient system prompt to mention metric flexibility
- [ ] Guide Claude on when to use specialized vs generic tools

### Phase 4: Testing
- [ ] Test: "What was visibility in Feb 26 at Hawaii?"
- [ ] Test: "Compare average temperatures in Jan vs Feb"
- [ ] Test: "Show me humidity, cloud cover, and wind for last week"
- [ ] Test: Backwards compatibility with wind queries

---

## Available Metrics (Database Fields)

The `Day` data class contains:
- **Temperature**: `temp`, `tempmin`, `tempmax`, `feelslike`, `dew`
- **Precipitation**: `precip`, `precipprob`, `precipcover`, `preciptype`, `snow`, `snowdepth`
- **Wind**: `windspeed`, `windgust`, `winddir`
- **Atmosphere**: `humidity`, `pressure`, `visibility`, `cloudcover`
- **Sun/Moon**: `sunrise`, `sunset`, `moonphase`
- **Radiation**: `solarradiation`, `solarenergy`, `uvindex`
- **Conditions**: `conditions`, `description`, `icon`

**Total**: 30+ fields available for querying

---

## Key Design Decisions

1. **Metric Names vs Field Names**
   - User asks: "visibility" (friendly)
   - Claude knows: `visibility` field (from schema description)
   - Handler maps both to database field
   - ✅ Provides flexibility without exposing schema complexity

2. **Single Generic Tool vs Multiple Specialized**
   - ✅ Single `get_weather_metrics` with metric array
   - Keeps tools simple and composable
   - Claude can ask for multiple metrics in one call

3. **Backwards Compatibility**
   - Keep old tools (users might have prompts hardcoded to them)
   - New tool as the "preferred" way forward
   - System prompt guides Claude to use generic tool

4. **Unit Conversions**
   - Wind: m/s → knots (1.944x multiplier)
   - Others: Return in database units with clear labels
   - MetricMapper includes unit information

---

## Files to Create/Modify

| File | Status | Change |
|------|--------|--------|
| `composeApp/src/commonMain/kotlin/ai/MetricMapper.kt` | 📝 NEW | Metric name translation |
| `composeApp/src/commonMain/kotlin/ai/WeatherTools.kt` | ✏️ MODIFY | Add `GetWeatherMetrics`, handler, mapper integration |
| `composeApp/src/commonMain/kotlin/ai/AnthropicClient.kt` | ✏️ MODIFY | Update system prompt (optional) |

---

## Next Steps

1. **Implement MetricMapper** - Name translation logic
2. **Add GetWeatherMetrics tool** - Schema and handler
3. **Test with Claude** - Verify flexibility
4. **Monitor token usage** - Metric queries might be more efficient
5. **Gather feedback** - User reactions to new query flexibility

---

## Notes for Implementation

- Keep MetricMapper extensible (easy to add new metrics)
- Consider caching repeated queries (same location/date)
- Error handling: Unknown metrics should return helpful message
- Consider pagination if date ranges are very large (30+ years of data)
- Unit labels should be clear in responses ("14.5 km", not just "14.5")

---

## Status

- ✅ Planning complete
- 📝 Design document created: `PLAN_Flexible_AI_Weather_Queries.md`
- ⏳ Ready for implementation phase
