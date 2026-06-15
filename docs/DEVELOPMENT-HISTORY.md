# ReWinds Development History (Consolidated)

This file is a chronological summary of past development sessions, consolidated from individual
`DEVELOPMENT-*.md` session logs for easier navigation. Each entry covers what was tackled, what
was learned, and what was implemented/resolved.

---

## Feb 12, 2026 — SQLite3 iOS CocoaPods Integration
*(from DEVELOPMENT-120226.md)*

- **Goal**: Add sqlite3 support for iOS builds via CocoaPods.
- Fixed Gradle cache corruption, Kotlin Hierarchy Template conflicts
  (`kotlin.mpp.applyDefaultHierarchyTemplate=false`), and an `embedAndSign`/CocoaPods conflict.
- Fixed jansi arm64/x86_64 native library mismatch.
- Configured `pod("sqlite3")` in build.gradle.kts; cinterop compiles successfully.
- **iOS compile OOM**: Disabled devirtualization (`-Xno-devirtualization`) and LTO; increased
  Gradle heap 2GB → 6GB. Simulator builds work; device (iosArm64) builds still OOM.
- Resolved a 428 duplicate-symbol linker error: removed stale `shared/build/xcode-frameworks`
  search path and manual `-framework ComposeApp` linker flag — CocoaPods' `vendored_frameworks`
  handles linkage. **Key learning**: never manually add `-framework` for Pod-managed frameworks.
- Manual `pod install` performed by user; must always use `iosApp.xcworkspace`, not `.xcodeproj`.
- Outcome: iOS Simulator builds work with sqlite3; device builds remain OOM (future work).

---

## Feb 16, 2026 — Database Export/Import Feature & Architecture Refactoring
*(from DEVELOPMENT-160226.md and DEVELOPMENT-160226-2.md)*

- **Goal**: Export DB from Android emulator, import on iOS device for data portability.
- Designed export format (raw `.db` file) and transfer/import approach (debug menu, developer-only).
- **Phase 1 (Android)**: `DatabaseExportImport.android.kt` exports `app.db` to
  `Documents/ReWinds/rewinds_backup_[timestamp].db`. Fully working.
- **Phase 2 (iOS)**: Placeholder implementation (avoided complex NSFileManager interop initially).
- Common `DatabaseExportImport` expect/actual interface with `exportDatabase()`,
  `importDatabase()`, `listBackups()`, wired into Koin DI.
- Added `DebugMenu.kt` composable + HomeView integration with show/hide toggle and result messages.
- **Architecture milestone**: Established and documented the **MV* pattern** in new
  `docs/ARCHITECTURE-RULES.md` — all logic in ViewModels, Views purely presentational. Refactored
  debug menu logic into `HomeViewModel`.
- Both Android and iOS compile successfully (iOS with warnings only).
- Next steps identified: test export on emulator, transfer file to iOS, implement real iOS
  NSFileManager import.

---

## Feb 17, 2026 — iOS Database Import Implementation & Build Optimization
*(from DEVELOPMENT-170226.md)*

- Implemented real NSFileManager-based `importDatabase()` on iOS (`@OptIn(ExperimentalForeignApi::class)`),
  with error handling (check file exists, remove old db, copy new one).
- Added `HomeViewModel` methods (`onImportFilePathChange`, `importDatabase()`) and debug UI
  (text field + import button) — following MV* pattern.
- Kotlin/commonMain compiles successfully; all C-interop annotations correct.
- **Blocker discovered**: Full iOS build OOMs (exit 137) during Kotlin/Native linking —
  both device (DevirtualizationAnalysis) and simulator (code generation) builds fail even at 6GB heap.
- Documented build memory mitigation options: build only what's needed, split Android/iOS builds,
  use CI/CD with more RAM, modularize app.
- Status at end of session: code complete, but testing blocked on build OOM.

---

## Feb 18, 2026 — iOS Build Resolution & Feature Platform Gating
*(from DEVELOPMENT-180226.md)*

- Confirmed Kotlin/Native OOM during DevirtualizationAnalysis persists even at 10GB heap with
  LTO/devirtualization/daemon all disabled — fundamental Kotlin/Native memory limitation on this
  KMP project size.
- **Decision**: Platform-gate the import/export feature off iOS entirely to reduce iOS compile
  complexity. Added `isAndroid()`/`isIOS()` expect/actual functions in `Platform.kt` +
  platform implementations; wrapped import/export UI in `HomeView.kt` with `if (isAndroid())`.
- Android verified fully working (assembleDebug succeeds, full test suite passes).
- Cleaned build caches (`iosApp/Pods/`, `Podfile.lock`, `.gradle/`).
- Result: iOS now compiles successfully (minimal feature set); Android remains primary platform.
- **Post-session discovery**: New iOS runtime crash —
  `KoinApplicationAlreadyStartedException` during Compose scene init. Root cause: the
  `koinInitialized` guard flag in `MainViewController()` doesn't reliably persist across
  recompositions. Documented several candidate fixes (check `GlobalContext.getOrNull()?.isStarted()`,
  try/catch, or stop/restart Koin) for the next session.

---

## Feb 19, 2026 (Evening) — iOS Runtime Debugging & Build Performance
*(from DEVELOPMENT-190226.md)*

- **Crash**: `InstanceCreationException` creating `HomeViewModel` via Koin on iOS launch.
- Investigated; an earlier commit (`ada1600`, "absolute path for database driver") had introduced
  broken `NSString` → Kotlin `String` conversion, producing an invalid `/app.db` path, which broke
  database init → broke Koin's `WeatherRepository` resolution → broke `HomeViewModel` creation.
- An initial fix attempt (changing the cast) was committed **without simulator testing** — user
  pushed back hard on this ("tests passing doesn't mean it runs"). This became a key process lesson.
- **Resolution**: Reverted `ada1600`'s absolute-path logic back to the simple relative
  `"app.db"` path. Kept a separate valid commit (`HomeViewTest` fix for missing
  `DatabaseExportImport` constructor param).
- App restored to stable, launches without crash; 32/32 Android tests pass.
- Reviewed iOS build performance optimizations already in place (buildAndroidOnly task, parallel
  Gradle builds, config cache, LTO/devirtualization disabled, 8GB heap) — concluded current setup
  is reasonably optimized; first iOS build ~44 min is expected.
- **Known unresolved issue carried forward**: iOS database **import** shows a success message but
  imported data doesn't actually persist/load after restart (export works fine). Root cause unclear
  — deferred as low priority.
- Key process lessons: never commit untested iOS runtime changes; compile success ≠ runtime
  correctness; revert quickly rather than "fixing" untested code further.

---

## Feb 22, 2026 — HomeView Design Refinement & Safe-Area Padding Fix
*(from DEVELOPMENT-220226.md)*

- **Problem**: iOS notch cut off the HomeView header (no spacing), while Android showed unwanted
  horizontal padding on the edges — needed a fix that works cleanly on both platforms.
- Tried platform-specific safe-area expect/actual values (iOS 47pt/34pt, Android 0pt) and a fixed
  48dp top margin — both reverted, didn't fully solve the Android padding issue.
- **Root cause found**: `.background()` colors on root containers (`Column`, `LazyColumn`,
  `HomeHeader`) were constraining layout to the safe-area boundary on Android, conflicting with
  SwiftUI's `.ignoresSafeArea(.all)` on iOS.
- **Fix (commit d585d22)**: Removed `.background()` from the main `Column`, `LazyColumn`, and
  `HomeHeader` in `HomeView.kt`, letting the system background show through.
- Result: clean edge-to-edge rendering on both platforms with no platform-specific code needed.
- **New general UI rule established**: avoid `.background()` on page-level views/headers; only
  use it on self-contained boxed components (e.g. ChatInputArea).

---

## Feb 27, 2026 — Data Accuracy & Display Fixes
*(from DEVELOPMENT-270226.md)*

Multiple fixes bundled in one session:

1. **Places mapping to wrong API locations** (`b223061`) — Subsequent downloads used place-name
   strings (re-geocoded by Visual Crossing, sometimes wrong), while initial adds used lat/lon.
   Added `resolveLocationString()` to reuse stored coordinates for *all* API calls, ensuring
   correct location and no duplicate place records.

2. **iOS build failure: `String.format` unavailable** (`24c71ed`) — Replaced 9 usages of
   `String.format("%.1f", value)` in `WeatherTools.kt` with a new multiplatform `roundTo(decimals)`
   extension using `kotlin.math.round()`.

3. **Forecast data mixed into historical data** (`56259f3`) — Added `truncateToYesterday()` to cap
   date ranges, preventing forecast days from being saved as if historical.

4. **Cleanup of previously-saved forecast data** (`327f0ed`) — Added `cleanupForecastDays()` /
   `deleteDaysAfterDate()`, run automatically on app startup via `HomeViewModel.init`.

5. **Month display labels** (`bd31891`) — Changed from "Missing data" to "X/Y days" with color
   coding (green = full, tan = partial, gray = empty); partial months now show data on click
   rather than prompting download. Added `getDaysInMonth()` for leap-year accuracy.

6. **Empty month labels** (`a42434b`) — Changed "0/31 days" to clearer "No stored days".

7. **Code cleanup** (`049c1b5`) — Removed 6 unnecessary `?: locationName` Elvis operators in
   `WeatherTools.kt` where `resolvedAddress` is non-nullable.

All fixes verified to compile for Android + iOS metadata; ready for cross-platform testing.

---

## Mar 1, 2026 — "Download More Days" Feature for Incomplete Months
*(from DEVELOPMENT-010326.md)*

- **Problem 1**: Could only download missing days from `PlaceSummaryView`, not from
  `MonthlyStatisticsView` where the gap is visible.
  - **Fix (`a32e39e`)**: Moved download logic into `WeatherRepository` as
    `downloadFullMonth(place, year, month)` for reuse by both ViewModels. Added a button in
    `MonthlyStatisticsView` showing "Download X missing days" with loading spinner, auto-hidden
    once complete.

- **Problem 2**: After downloading from `MonthlyStatisticsView` and navigating back,
  `PlaceSummaryView`'s month card still showed stale partial-data status.
  - **Fix (`3f80eb7`)**: Added `refreshData()` to `PlaceSummaryViewModel`, called on back
    navigation to reload cached data — month cards now reflect updated completion immediately.

- Result: download workflow accessible from both views, shared repository logic, consistent state
  after navigation. All checklist items (compile, button visibility, loading state, refresh,
  no duplication) verified.

---

## Mar 3, 2026 — AI Chat Feature Working End-to-End (Android)
*(from DEVELOPMENT_030326.md)*

- **Goal**: Get the Claude-powered AI chat working on Android.
- Added an API key configuration modal (shown when `ANTHROPIC_API_KEY` not set), instructing
  `export ANTHROPIC_API_KEY=sk-ant-<key>`.
- Moved API key to `gradle.properties` → exposed via `BuildConfig.ANTHROPIC_API_KEY` (more
  reliable than env vars for Android).
- **Serialization crisis**: `AnthropicContent` sealed class conflicted with kotlinx.serialization's
  automatic JSON class discriminator (`type` field collision). Resolved by removing
  `@Serializable` from the sealed class itself, changing `AnthropicMessage.content` to
  `JsonElement`, and building content JSON manually via `buildJsonArray` +
  `AnthropicContentSerializer`.
- **Critical fix**: Anthropic API returned `400: "max_tokens: Field required"` because
  kotlinx.serialization skips default-valued fields by default. Fixed by adding
  `encodeDefaults = true` to the `Json {}` config in `Platform.android.kt`.
- **Model selection**: `claude-3-5-sonnet-20241022` and `claude-sonnet-4-5-20250514` both 404'd;
  settled on `claude-haiku-4-5` (cheaper, $1/$5 per 1M tokens, works reliably).
- Result: chat UI works end-to-end on Android emulator, all 4 weather tools usable by Claude.
  iOS testing and chat persistence deferred to future sessions.

---

## Mar 4, 2026 — Flexible AI Weather Queries (Planning + Implementation)
*(from DEVELOPMENT_040326.md)*

- **Goal**: Let Claude query *any* of the 30+ weather metrics (visibility, humidity, temperature,
  etc.), not just wind.
- Designed and implemented `MetricMapper.kt` (160 lines, 50+ friendly metric name aliases, unit
  conversion/formatting/display-name logic).
- Added a new generic `get_weather_metrics` tool to `WeatherTools.kt` with
  `handleGetWeatherMetrics()` — validates location/dates/metrics, maps to DB fields, returns
  formatted results with units (e.g. wind m/s → knots).
- Registered the tool in `allToolSchemas()` and `handleToolCall()`; kept old wind-specific tools
  for backwards compatibility, with the new tool given priority.
- Added 100+ unit tests across `MetricMapperTest.kt` and `WeatherToolsMetricsTest.kt` covering all
  metric categories, unit conversion, date ranges, and error handling.
- CommonMain compiles cleanly; committed as `d470fc7`. Ready for integration testing with Claude
  and on iOS.

---

## Mar 5, 2026 — iOS Keychain API Key Storage Fixed & Build-Time Config
*(from DEVELOPMENT_050326.md)*

- **Duplicate symbol linker error (528 symbols)**: `ComposeApp.framework` was being linked twice
  (directly via Xcode project + via CocoaPods). Removed the manual framework reference from
  `iosApp.xcodeproj/project.pbxproj`, letting CocoaPods (`Pods-iosApp.debug.xcconfig`) handle it.
- **iOS Keychain save was a stub**: infrastructure (`KeychainHelper.swift`, `ApiKeyManager.kt`,
  `KeychainBridge.kt`) existed but wasn't wired up.
  - Created `IosKeychain.kt` wrapper functions (`setApiKeyFromKeychain`, `registerKeychainCallbacks`)
    in `iosMain` — needed because Kotlin objects in `commonMain` aren't reliably exposed to Swift;
    top-level `iosMain` functions are exposed as `IosKeychainKt.functionName()`.
  - Updated `iOSApp.swift` to load the key from Keychain before `initKoin()` and register
    save/delete callbacks.
  - Implemented `getAnthropicApiKey()`, `saveApiKeyPlatform()`, `deleteApiKeyPlatform()` in
    `Platform.apple.kt` to use `ApiKeyManager` + `KeychainBridge`.
- **Build-time config support added** (parallel to Android's gradle.properties): new gitignored
  `iosApp/Config.local.xcconfig`, included from `Config.xcconfig`, exposed via `Info.plist`
  (`ANTHROPIC_API_KEY`). `iOSApp.swift` priority: build config → Keychain → placeholder.
- Result: iOS Keychain-based API key storage fully working, with optional build-time override;
  no duplicate symbol errors.

---

## Mar 6, 2026 (Part 3) — Station Map Modal (OpenStreetMap)
*(from DEVELOPMENT_060326.md)*

- **Goal**: Tapping the info button on `PlaceSummaryView` opens a modal showing the weather
  station location on an interactive map.
- New `StationMapModal.kt` composable using `compose-webview-multiplatform` (1.9.40) +
  Leaflet.js + OpenStreetMap tiles (no API key needed). HTML embedded as a base64 data URI.
- `PlaceSummaryViewModel` extended with `latitude`/`longitude` in its Success state, extracted
  from `WeatherResponse.stations` (first station, with fallback to geosearch coordinates).
- Added `WeatherRepository.verifyPlaceAndGetStations(place)` — a separate single-day request with
  `&include=stations`, called once per new place, so bulk fetches no longer need the stations
  parameter (efficiency).
- **iOS WebView sandbox issue**: URL-encoded data URIs triggered
  `Unable to hide query parameters from script` errors. Fixed by switching to proper base64 data
  URIs (`data:text/html;base64,...`), implemented with a custom platform-independent base64
  encoder (no `java.util.Base64`).
- Result: map modal with pinned actual station location works on both Android and iOS
  (6 commits, ending `8677f62`). Future ideas noted: persist station coords, show multiple
  stations, quality indicators.

---

## Mar 9-11, 2026 — UI Improvements, Bug Fixes, CI/CD Setup
*(from DEVELOPMENT_09-110326.md)*

**Features**:
- **KIM-90**: Added "Let's talk about the weather!" welcome message on chat init/reset (`f817271`).
- **KIM-91**: Keyboard dismisses when tapping outside chat input or on send (`e8f5f9d`).
- **KIM-100**: Home screen search keyboard now dismisses on location selection or tap-outside
  (`b7c69e9`).

**Bug fix**:
- **KIM-101**: Chat header was pushed off-screen when keyboard appeared. Fixed by applying
  `imePadding()` to the root `Box` in `ChatView` (final commit `30226b9`, after several
  iterations/reverts: `e18deb3`, `9f11cbd`, `48be319`, `d67b4fa`, `fcbef2e`). Key learning: chat
  screen needs to be pushed up by `imePadding()`, while Home instead dismisses the keyboard via
  `LocalFocusManager.clearFocus()` — different patterns per screen.

**CI/CD (Epic KIM-92)**:
- **KIM-93**: New `.github/workflows/ci.yml` — parallel Android (ubuntu) + iOS (macos) jobs
  (`a59b697`).
- **KIM-94/95**: Validated both jobs on real GitHub Actions runs (Android ~1m39s, iOS ~19m6s).
  Fixed 10 issues along the way: Gradle action v3→v4, `includeAllTargets` flag timing, missing
  `BuildConfig.ANTHROPIC_API_KEY` field, CocoaPods needing a dummy framework first, missing Xcode
  scheme, `xcpretty` install, no XCTest targets (switched to build-only), optional
  `Config.xcconfig` include, missing JDK in iOS job, timeout increased to 90 min.
- **KIM-97**: Documented step-by-step branch protection setup (CI checks required, PR reviews,
  auto-delete branches, etc.) — ready for manual GitHub configuration by user.

**QA**:
- Fixed 2 failing tests (tool-count mismatch, days-count mismatch) — all 134 tests passing
  (`fa447a0`).
- Added `ChatViewTest.kt` (3 layout tests for KIM-101) (`255ec27`).

**Status at end of session**: KIM-90, 91, 93, 94, 95, 100, 101 done; KIM-96 blocked on KIM-97
(manual step); KIM-97 ready for user action.

---

## Mar 18, 2026 (Part 2) — KIM-57 Localization Framework Setup
*(from CURRENT-SESSION.md)*

- **Goal**: Set up Compose Multiplatform resource-based localization infrastructure (Phase 1 of
  KIM-57), following on from KIM-115 (code coverage, completed earlier the same day — see
  `memory/completed_kim115_coverage.md`).
- Created `composeResources/values/strings.xml` with 90+ English UI strings, organized by feature;
  created `composeResources/values-de/strings.xml` as an English placeholder for future German
  translations.
- Replaced all hardcoded strings across 9 view files (HomeView, ChatView, SettingsView,
  PlaceSummaryView, MonthlyStatisticsView, WeatherCards, DaySummaryRow, CalendarSelectors,
  AppHeader) with `stringResource()` calls, including parameterized and plural strings, plus
  helper functions for month names.
- Android debug build verified compiling successfully. Commit: `724d21c` (Feature: Implement
  localization framework setup (KIM-57)), plus `53f98c3` (docs: session summary for KIM-115).
- **Next planned work** (per this log): KIM-57 Phase 2 — translate `values-de/strings.xml` to
  German and verify in German locale; then resume KIM-97/KIM-96 (GitHub branch protection / PR
  workflow).

---

## Notes on superseded/later state

Several issues raised in these logs were later resolved per `CLAUDE.md` and project memory
(as of mid-2026):
- The iOS database export/import feature is now reported as fully working on both platforms
  (Database export/import feature fully working on BOTH iOS and Android ✅).
- AI Chat with flexible metrics querying and the three-layer permission flow are complete.
- iOS Keychain-based API key storage with a Settings UI is complete.
- Gradle iOS build tasks remain unreliable; Xcode is the standard iOS build tool
  (`open iosApp/iosApp.xcworkspace`).
