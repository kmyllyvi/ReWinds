---
name: schema-places-identity
description: How "places" are identified in the SQLDelight schema — no integer Places table exists
metadata:
  type: project
---

There is no dedicated `Places` table in `composeApp/src/commonMain/sqldelight/com/km/rewinds/db/AppDatabase.sq`.
Places are identified by `WeatherResponse.resolvedAddress` (`TEXT NOT NULL PRIMARY KEY`).
`WeatherRepository.getSavedPlaceNames()` returns `List<String>` of these resolved-address/name strings — not integer ids.

**Why**: came up reviewing KIM-285 (multiple chat sessions) — the spec proposed a `placeId INTEGER` column on `ChatSession`, but that doesn't match any real id in the schema.

**How to apply**: any future ticket proposing a `placeId`/place foreign key must use `TEXT` matching `resolvedAddress`, not an integer surrogate key, unless/until a real Places table is introduced. Flag this during spec review for new tickets that reference "place id".

Migration history check (as of 2026-06-12): latest migration file is `4.sqm` (schema v4→5, WeatherStation refactor); `AppDatabase.sq` reflects v5. Next migration should be `5.sqm` (v5→6).
