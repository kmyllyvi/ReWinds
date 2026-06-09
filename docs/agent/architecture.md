# ReWinds — System Architecture

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. All sections reflect codebase structure only. -->

**Last updated:** 2026-06-09 (bootstrap)
**Status:** Active

---

## Overview

ReWinds is a Compose Multiplatform application (iOS + Android) that lets wind-sports enthusiasts (kitesurfers, windsurfers) save locations, store historical weather data from Visual Crossing, and query that data through an embedded Claude AI chat assistant.

---

## Top-level component map

```
App (App.kt)
 └── AppContent (AppViewModel)
      └── Navigation (Router.kt)           — three-tab scaffold
           ├── Places tab
           │    ├── HomeView (HomeViewModel)
           │    └── [push] PlaceSummaryView (PlaceSummaryViewModel)
           │              └── [push] MonthlyStatisticsView (MonthlyStatisticsViewModel)
           ├── Chat tab
           │    └── ChatView (ChatViewModel)
           └── Settings tab
                └── SettingsView (SettingsViewModel)
```

---

## Major modules

| Module | Package(s) | Role |
|---|---|---|
| AI Chat | `ai/` | Claude API integration, agentic tool loop, chat persistence |
| Weather Data | `core/` (WeatherRepository, WeatherData, Database) | Visual Crossing API, SQLDelight DB, caching |
| Navigation | `core/` (Navigator, NavigationRoutes, Router) | Tab-bar + push stack navigation |
| DI | `DI.kt`, Koin | Dependency wiring, ViewModel registration |
| Settings | `settings/` | API key management, days-of-interest filter |
| Home | `home/` | Place list, geo-search, DB export/import |
| Place Summary | `place/` (PlaceSummaryViewModel) | Per-place day grid, station map |
| Monthly Statistics | `place/` (MonthlyStatisticsViewModel) | Month-level stats, wind chart, filter application |
| iOS Platform | `iosMain/`, `iosApp/` | Keychain bridges, SQLite driver, Swift entry point |
| Android Platform | `androidMain/` | Android SQLite driver, MainActivity |
| UI Theme | `ui/theme/` | Midnight Blue dark-only design system |
| Core Utilities | `core/` (AppConstants, Logger, NetworkService, FormatUtils) | Shared plumbing |

---

## Data flow — weather data lifecycle

```
User searches location
  → HomeViewModel.onSearchResultSelected()
  → WeatherRepository.addPlaceFromSearch()         (Open-Meteo geocoding → Visual Crossing)
  → SqlDelightDatabase.saveWeatherResponse()       (persisted to SQLite)
  → HomeViewModel.loadSavedPlaces()                (GROUP BY count query, no full load)

User opens a place
  → PlaceSummaryViewModel.loadWeatherData()
  → WeatherRepository.getSavedDataFor()            (in-memory cache → SQLite full load)
  → [auto-backfill] WeatherRepository.fetchAndPersistStations()

User downloads a month
  → PlaceSummaryViewModel.onDownloadFullMonth()
  → WeatherRepository.downloadFullMonth()
  → WeatherRepository.getDaysRange()               (gap-fill logic: DB check → network per gap)
  → SqlDelightDatabase.saveWeatherResponse()       (upsert-days semantics)
```

## Data flow — AI chat

```
User sends message
  → ChatViewModel.sendMessage()
  → AiRepository.sendMessage()                    (agentic loop, max 20 turns)
  → AnthropicClient.sendMessage()                 (POST /v1/messages, claude-haiku-4-5)
  → [if tool_use] WeatherTools.handleToolCall()
  → WeatherRepository.getDaysRange() / getSavedPlaceNames() / etc.
  → [if data missing] permission_required response → user confirms → fetch → retry
  → ChatRepository.saveMessage()                  (persisted per session in SQLite)
```

---

## External API dependencies

| API | Purpose | Key storage |
|---|---|---|
| Visual Crossing (`weather.visualcrossing.com`) | Historical weather data + station data | iOS: Keychain (`visual_crossing_api_key`); Android: BuildConfig |
| Open-Meteo geocoding (`geocoding-api.open-meteo.com`) | Location search | No key required |
| Anthropic API (`api.anthropic.com`) | Claude AI chat | iOS: Keychain (`anthropic_api_key`) + optional build config; Android: BuildConfig |

---

## Build system topology

- **Kotlin Multiplatform** — `commonMain`, `androidMain`, `iosMain` source sets.
- **SQLDelight** — schema at `commonMain/sqldelight`, generates `AppDatabase` and typed queries.
- **CocoaPods** — manages `sqlite3` native dependency for iOS.
- **Koin** — DI; single `appModule` in `DI.kt`, initialised from platform entry points.
- **Default Gradle build** (`./gradlew build`) — Android only. iOS targets require `-PincludeAllTargets=true` or Xcode.

---

## Architectural conventions

See `/docs/agent/ARCHITECTURE-RULES.md` for the full rule set. Key points:

- **MV* pattern** enforced: no business logic in composables; all state decisions made in ViewModels.
- **Navigation** uses a per-tab back stack (`mutableStateListOf<NavRoute>`); `NavigatorImpl` wraps it.
- **Platform expect/actual** used for: `DatabaseDriverFactory`, `DatabaseExportImport`, `httpClient`, `getAnthropicApiKey`, `getVisualCrossingApiKey`, `saveApiKeyPlatform`, `deleteApiKeyPlatform`, language preference persistence.
- **UI theme** — single dark-only Midnight Blue scheme; no light-mode support.

---

## Decisions log

- See `/docs/agent/decisions/` for individual architectural decision records.
