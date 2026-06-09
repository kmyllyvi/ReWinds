# Gap-fill Caching Strategy for Weather Data

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. Decision reconstructed from codebase only. -->

**Date:** 2026-06-09
**PR:** unknown (bootstrap)
**Ticket:** KIM-278 (referenced in code comments)
**Status:** Active

---

## Context

Weather data is expensive to fetch (Visual Crossing API costs per query-cost unit). Users frequently revisit the same places and date ranges. Loading all days and hours for a place on every Home screen visit was too slow as data accumulated.

## Decision

Two-level caching strategy:

1. **Session-lifetime in-memory cache** (`savedDataCache: HashMap<String, WeatherResponse>` in `WeatherRepositoryImpl`). Scoped to the Koin singleton (app session). Stores the full `WeatherResponse` for each place after first DB load. Invalidated on every write via `persistAndInvalidate()`.

2. **Gap-fill queries**: `getDaysRange` checks which dates are already in the DB before making any network calls. Only consecutive missing date ranges (gaps) are fetched. Each gap is a separate network request, minimising API usage.

3. **Home screen lightweight query**: The Home screen uses `getPlaceDayCounts()` (a single `GROUP BY` SQL query) instead of loading full day/hour rows. This eliminates the bottleneck that previously caused the home screen to be slow with many saved places.

## Rationale

- Full-table loads per place on the home screen were identified as the primary performance problem (KIM-278).
- The session cache avoids repeated DB reads for places the user navigates to frequently within a session.
- Gap-fill reduces API calls to only what is necessary, controlling cost and latency.

## Consequences

- The in-memory cache must be invalidated on every write or stale data will be served. All write paths go through `persistAndInvalidate()`, which enforces this.
- `getPreviousDays` always fetches from the network (no DB-first check). This is intentional for the "recent data" use case but means it always costs an API call.
- Memory usage grows with the number of places and days stored; no eviction policy exists beyond app restart.
