# Three-Layer AI Data Fetch Permission Gate

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. Decision reconstructed from codebase only. -->

**Date:** 2026-06-09
**PR:** unknown (bootstrap)
**Ticket:** unknown (bootstrap)
**Status:** Active

---

## Context

The AI chat can request weather data fetches on behalf of the user. Fetching triggers a Visual Crossing API call (which has a cost and takes time). Users should be informed before the app makes external calls on their behalf.

## Decision

A three-layer permission flow is implemented in `WeatherTools.handleGetWeatherMetrics`:

1. **Check availability**: Before fetching, call `WeatherRepository.checkDataAvailability()` to determine if data is already present (`Available`, `Partial`, or `Missing`).
2. **Permission request**: If data is not fully `Available`, return a structured JSON response with `"status": "permission_required"` instead of fetching. This response includes the location, date range, and a natural-language message asking the user to confirm.
3. **User confirmation**: `ChatViewModel` detects the permission response, surfaces it to the user, and waits for a confirmation word (`yes`, `ok`, `proceed`, `confirm`, `y`). On confirmation, it calls `WeatherRepository.getDaysRange()` directly, then asks Claude to retry the original query.

## Rationale

- Prevents unexpected API charges when the user asks a question that would require fetching months of data.
- Keeps the data-fetch decision visible and user-controlled.

## Consequences

- The permission detection in `ChatViewModel` uses both a structured `permission_required` JSON check and a regex fallback on the response text. The regex approach is fragile if the AI's phrasing changes.
- If the data is already `Available`, there is no permission gate — the fetch is transparent.
- Context chips (visible in the chat UI) do not currently constrain which data Claude can access; they are UI-only state as of bootstrap.
- The `get_wind_summary`, `get_monthly_stats`, and `get_best_days` tools do not have the permission gate — they call `getDaysRange` directly. Only `get_weather_metrics` implements the full three-layer flow.
