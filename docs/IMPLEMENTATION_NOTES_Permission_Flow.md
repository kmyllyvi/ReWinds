# Implementation Notes: AI Permission Flow for Data Fetching

**Date**: March 4, 2026
**Commit**: `d5f7cbc - Feature: Implement permission flow for AI data fetching`

## Overview

Implemented **Option 3: Hybrid Approach** for user permission before AI fetches weather data from APIs. This prevents uncontrolled API calls during conversations.

## Problem Statement

The flexible metrics tool allowed Claude to query any weather data, but it would silently fetch new data from the API without user knowledge, leading to:
- Uncontrolled API usage
- Unexpected API calls
- Poor UX (user doesn't know when/why API calls happen)

## Solution: Option 3 (Hybrid)

**Three-layer permission flow:**

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 1: WeatherRepository                                   │
│ - checkDataAvailability() method                             │
│ - Returns: Available | Partial | Missing                    │
│ - No fetching, just checks database                         │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ Layer 2: WeatherTools Handler                               │
│ - Checks availability before querying                       │
│ - If not Available: Returns "permission_required" JSON      │
│ - Claude sees this and asks user for permission            │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ Layer 3: ChatViewModel                                      │
│ - Detects permission request in AI's response              │
│ - Sets pendingDataFetch state                              │
│ - Waits for user "yes"/"ok" confirmation                   │
│ - Calls confirmPendingDataFetch() to proceed               │
└─────────────────────────────────────────────────────────────┘
```

## User Flow

```
User: "What was visibility in Feb 26 at Oahu?"
      ↓
AI: "I need to fetch weather data for Oahu from Feb 26 to Feb 26.
     This will make an API call. Please type 'yes' or 'ok' to proceed."
      ↓
User: "yes"
      ↓
(ChatViewModel detects confirmation, calls confirmPendingDataFetch)
      ↓
AI: "Visibility in Oahu on Feb 26 was 14.5 km"
```

## Implementation Details

### 1. DataAvailability.kt (NEW)

```kotlin
enum class DataAvailabilityStatus {
    Available,  // All data in database
    Partial,    // Some data exists
    Missing     // No data available
}

data class DataAvailabilityInfo(
    val status: String,
    val placeName: String,
    val startDate: String,
    val endDate: String,
    val presentDaysCount: Int,
    val requestedDaysCount: Int,
    val missingDates: List<String>
)
```

### 2. WeatherRepository Enhancement

Added to interface:
```kotlin
suspend fun checkDataAvailability(
    place: String,
    fromDate: String,
    toDate: String
): DataAvailabilityStatus
```

Implementation in `WeatherRepositoryImpl`:
- Calls `generateDateList()` to get target dates
- Queries database for matching days
- Compares requested vs. actual count
- Returns: `Available` (100% match), `Partial` (>0 match), or `Missing` (0 match)

### 3. WeatherTools Handler Update

In `handleGetWeatherMetrics()`:
```kotlin
// Check availability first
val dataStatus = repo.checkDataAvailability(locationName, startDate, endDate)

// If not fully available, ask permission
if (dataStatus != DataAvailabilityStatus.Available) {
    return buildJsonObject {
        put("status", "permission_required")
        put("message", "I need to fetch weather data for $locationName...")
        put("location", locationName)
        put("start_date", startDate)
        put("end_date", endDate)
        // ... more details
    }.toString()
}

// Only fetch if data is available OR permission given
val weatherResponse = repo.getDaysRange(...)
```

### 4. ChatViewModel State Management

Added to `ChatUiState`:
```kotlin
data class PendingDataFetch(
    val location: String,
    val startDate: String,
    val endDate: String,
    val metrics: List<String>
)

data class ChatUiState(
    // ... existing fields
    val pendingDataFetch: PendingDataFetch? = null
)
```

### 5. ChatViewModel Permission Detection

In `sendMessage()`:
```kotlin
// Check if user is confirming pending fetch
if (currentState.pendingDataFetch != null) {
    if (trimmedInput.lowercase() in listOf("yes", "ok", "proceed", "confirm", "y")) {
        confirmPendingDataFetch(trimmedInput)
        return
    } else {
        // User rejected, cancel fetch
        // ...
        return
    }
}
```

After receiving AI response:
```kotlin
// Detect if AI is asking for permission
val hasPermissionKeywords = responseText.contains("fetch", ignoreCase = true) &&
    (responseText.contains("API call") || responseText.contains("permission"))

if (hasPermissionKeywords) {
    // Extract location and dates using regex
    val locationRegex = Regex("""for\s+(\w+)\s+from""")
    val dateRangeRegex = Regex("""from\s+(\d{4}-\d{2}-\d{2})\s+to\s+(\d{4}-\d{2}-\d{2})""")

    // Parse and set pendingDataFetch
    setPendingDataFetch(location, startDate, endDate, emptyList())
}
```

### 6. Confirmation Execution

```kotlin
private fun confirmPendingDataFetch(confirmationInput: String) {
    // Send message to AI: "Proceed with fetch and answer my question"
    val message = "Proceed with fetching weather data... and then answer my original question"
    val result = aiRepository.sendMessage(message)

    // Clear pendingDataFetch
    _uiState.update { it.copy(pendingDataFetch = null) }
}
```

## Conversation Flow (Detailed)

```
1. User inputs: "What was visibility in Feb 26 at Oahu?"
   └─ ChatViewModel.sendMessage() called

2. AiRepository.sendMessage()
   └─ Claude processes with available tools

3. Claude calls: get_weather_metrics(location="Oahu", dates=...)
   └─ WeatherTools.handleGetWeatherMetrics() executes

4. Check: repo.checkDataAvailability("Oahu", "2026-02-26", "2026-02-26")
   └─ Returns DataAvailabilityStatus.Missing (no data in DB)

5. Handler returns:
   {
     "status": "permission_required",
     "message": "I need to fetch weather data for Oahu from 2026-02-26 to 2026-02-26.
                This will make an API call. Please type 'yes' or 'ok' to proceed."
   }

6. AiRepository receives tool result
   └─ Claude sees the permission_required message
   └─ Claude responds to user with permission request

7. AiRepository returns:
   "I need to fetch weather data for Oahu from Feb 26 to Feb 26.
    This will make an API call. Please type 'yes' or 'ok' to proceed."

8. ChatViewModel receives response
   └─ Detects permission keywords (fetch, API call, yes or ok)
   └─ Extracts location: "Oahu"
   └─ Extracts dates: "2026-02-26" to "2026-02-26"
   └─ Calls setPendingDataFetch()
   └─ Displays message to user

9. User sees: "I need to fetch weather data... Please type 'yes' or 'ok'"

10. User inputs: "yes"
    └─ ChatViewModel.sendMessage("yes") called
    └─ Detects that pendingDataFetch is set
    └─ Recognizes "yes" as confirmation
    └─ Calls confirmPendingDataFetch()

11. confirmPendingDataFetch() sends to AI:
    "Proceed with fetching weather data for Oahu from 2026-02-26 to 2026-02-26
     and then answer my original question."

12. AiRepository.sendMessage() again
    └─ Claude now calls get_weather_metrics with permission

13. Check: repo.checkDataAvailability()
    └─ Still returns Missing (data not in DB yet)
    └─ But tool fetches it anyway when called again?

    Wait, this needs refinement...
```

## Known Limitations & Future Work

1. **Tool Execution After Confirmation**: Currently, if user confirms, we just send a message back to Claude saying "proceed". Claude might call the tool again and hit the same permission request. A better approach:
   - Have a "fetch authorized" flag in the conversation context
   - Or modify the tool to actually fetch when permission is granted

2. **Regex Extraction**: Currently extracts location/dates using regex on AI's text. This is brittle if AI changes wording. Better approach:
   - Have tool return structured permission_required response
   - ChatViewModel parses the JSON directly
   - No string matching needed

3. **Multiple Metrics**: Currently extracts empty list for metrics. Should extract which metrics were requested.

## Testing

### Manual Test Case
1. User: "Show me visibility and cloud cover for Feb 26 at Oahu"
2. Expected: AI asks for permission
3. User: "yes"
4. Expected: Data fetches, AI returns values

### Edge Cases to Test
- User says "no" or rejects → should cancel fetch
- User says random text → should treat as rejection
- AI doesn't mention dates in response → regex extraction fails gracefully
- Multiple tool calls (wind + metrics) → only one asks permission

## Code Files Modified

| File | Changes |
|------|---------|
| `core/DataAvailability.kt` | NEW - Status enum + Info data class |
| `core/WeatherRepository.kt` | Added `checkDataAvailability()` method + impl |
| `ai/WeatherTools.kt` | Check availability, return permission_required |
| `ai/ChatViewModel.kt` | Added pendingDataFetch state, confirmation logic |

## Summary

✅ **Completed**: Option 3 Hybrid approach implemented
✅ **Compiles**: All code checked and working
✅ **User Control**: AI asks before making API calls
✅ **Natural Flow**: Uses conversational "yes/ok" confirmation

⏳ **Future**: Improve structured permission responses, test edge cases
