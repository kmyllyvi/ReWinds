# Format Utils Module

**Last updated:** 2026-06-17 (PR #38 — KIM-302)
**Status:** Active

## Purpose

Provides KMP-safe formatting helpers shared across UI composables. All functions operate on primitive Kotlin types with no dependency on JVM date classes or `String.format`, making them safe for both Android and iOS targets.

## Responsibilities

- Formatting decimal numbers with explicit sign and fixed one-decimal-place output (`formatDecimal`).
- Formatting a `"YYYY-MM-DD"` date string into a short `"Weekday D."` label (e.g. `"Mon 13."`) using Sakamoto's integer weekday algorithm (`shortDayLabel`).
- Formatting a temperature range as `"min–max °C"` with `"--"` placeholders for null values (`formatTemperatureRange`).
- Formatting a wind/gust speed as a whole-number `"N km/h"` string with `"-- km/h"` for null (`formatGust`).
- Mapping an integer month index to a full English month name (`monthName`).

## Dependencies

- No internal module dependencies.
- No external service dependencies.
- `kotlin.math.roundToLong` for whole-number rounding.

## Key interfaces

```kotlin
fun formatDecimal(value: Double): String
fun shortDayLabel(date: String?): String       // "YYYY-MM-DD" → "Mon 13." ; null → "--"
fun formatTemperatureRange(minTemp: Double?, maxTemp: Double?): String  // "12–19 °C"
fun formatGust(speed: Double?): String          // "32 km/h" ; null → "-- km/h"
fun monthName(month: Int): String
```

All functions are top-level in the `core.utils` package (`core/FormatUtils.kt`).

`shortDayLabel` internals:
- Parses via `String.split("-")` — no `java.util.Date` or `kotlinx.datetime`.
- Weekday derived by `mondayBasedWeekday(year, month, day)`, a private Sakamoto-style calculation returning `0=Monday … 6=Sunday`.
- Falls back to the raw input string when unparseable; returns `"--"` for null input.

## Known constraints

- `shortDayLabel` does not validate that the day number is in range for the given month+year (e.g. Feb 30 passes the `1..31` guard but is not calendar-checked). Sakamoto will compute an incorrect weekday for invalid dates.
- Weekday abbreviations are English-only (`Mon…Sun`); no localisation support.
- `formatWhole` rounds via `roundToLong()` — ties round toward positive infinity on both platforms (Kotlin default), which is consistent but may differ from system locale rounding rules.

## Decisions log

- No decision docs. Functions added in KIM-302 to replace inline formatting in `DaySummaryRow`.
