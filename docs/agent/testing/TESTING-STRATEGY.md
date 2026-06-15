# ReWinds Comprehensive Testing Strategy

**Document Date**: Feb 19, 2026
**Current Test Status**: 32 Android unit tests + 4 iOS platform tests
**Target Coverage**: 50% (Phase 1) → 65% (Phase 2) → 75% (Long-term)

---

## Executive Summary

**Current State:**
- **32 Android Unit Tests** passing (NavigatorTest, HomeViewModelTest, AppViewModelTest, AppStartupTest, NavigatorBackStackTest, PlaceSummaryViewModelTest)
- **4 iOS Platform Tests** compiling successfully (PlatformFunctionsTest, DatabaseDriverFactoryTest, CoroutineDispatchersTest, DatabaseIntegrationTest)
- **Test Infrastructure**: Kotlin Multiplatform test framework with Coroutines support
- **Coverage Gap**: 55 source files, but only ~11 test files covering key logic

**Strategic Approach:** Prioritize high-value unit tests for ViewModels (easily testable) while building platform-specific tests for iOS/Android boundary conditions.

---

## Layered UI testing strategy

> The unit/integration plan in the rest of this document predates the UI testing
> epic (**KIM-289**). The sections below remain the plan for **logic** coverage.
> For **UI** coverage, the epic defines a three-layer model — deterministic gates
> on the inside, exploratory discovery on the outside:

| Layer | Suite                                    | Scope                                      | When it runs                     | Gates? | Reference / location                                  |
| ----- | ---------------------------------------- | ------------------------------------------ | -------------------------------- | ------ | ----------------------------------------------------- |
| **1** | **Compose semantic UI tests** (KIM-293)  | Single-screen behaviour, Android           | **Manual / `connectedAndroidTest`** | Not yet (see note) | `composeApp/src/androidInstrumentedTest/kotlin/` (instrumented, `AndroidJUnitRunner`) |
| **2** | **Maestro E2E smoke suite** (KIM-294)    | End-to-end critical journeys, Android + iOS| **Manual / release to master**   | Yes (release) | `.maestro/flows/`, [`.maestro/README.md`](../../../.maestro/README.md) |
| **3** | **Agentic exploratory testing** (KIM-296)| Unscripted "click around like a human"     | **Periodic, manual**             | **No** | [`AGENTIC-EXPLORATORY-TESTING.md`](./AGENTIC-EXPLORATORY-TESTING.md) |

> **Note on Layer 1 CI gating.** The KIM-293 Compose semantic tests are
> _instrumented_ Android tests (`AndroidJUnitRunner`, not Robolectric). The
> per-PR CI workflow (`ci.yml`) currently runs only
> `:composeApp:testDebugUnitTest`, **not** `connectedAndroidTest`, so Layer 1 is
> **not yet gated per-PR** — it is run manually / on demand. Wiring Layer 1 into
> per-PR CI (which needs an emulator on the runner) is the still-open KIM-293
> follow-up parked for Kimmo.

**How the layers relate.** Layers 1 and 2 lock down journeys we already know
about (the 5–10 critical journeys defined in **KIM-291**). Layer 3 — a
human-triggered vision/computer-use agent exercising the running app without a
script — is for finding the broken states we _haven't_ thought of yet. Its
findings feed back **inward**: a confirmed bug becomes a Linear ticket, and a
recurring or critical-path bug is promoted into a Maestro flow (Layer 2) or a
Compose semantic test (Layer 1) so it can never silently regress. Layer 3 is
explicitly **non-CI-gating** — it never blocks a PR or a release. See
[`AGENTIC-EXPLORATORY-TESTING.md`](./AGENTIC-EXPLORATORY-TESTING.md) for the full
runbook, seed exploration goals, cadence, feedback loop, and cost budget — the
direct answer to **KIM-288**.

---

## 1. Current Test Coverage Analysis

### 1.1 Existing Test Inventory

#### Common Tests (Android + iOS)
| Test File | Type | Count | Coverage |
|-----------|------|-------|----------|
| `AppViewModelTest.kt` | Unit | 4 | AppViewModel state management |
| `HomeViewModelTest.kt` | Unit | 9 | HomeViewModel UI state + search |
| `NavigatorTest.kt` | Unit | 6 | Navigation stack management |
| `NavigatorBackStackTest.kt` | Unit | 5 | Navigation initialization |
| `AppStartupTest.kt` | Integration | 3 | App startup sequence |
| `PlaceSummaryViewModelTest.kt` | Unit | 6 | Monthly temp calculations |

**Subtotal: 33 tests** across 6 files

#### iOS-Specific Tests
| Test File | Type | Count | Focus |
|-----------|------|-------|-------|
| `PlatformFunctionsTest.kt` | Unit | 2 | Platform detection (isAndroid/isIOS) |
| `DatabaseDriverFactoryTest.kt` | Unit | 1 | Driver creation |
| `DatabaseIntegrationTest.kt` | Integration | 3 | Database operations |
| `CoroutineDispatchersTest.kt` | Integration | ? | Threading (not yet reviewed) |

**Subtotal: 6-10 tests** across 4 files

### 1.2 Coverage Gaps Identified

#### Critical Gaps (High Priority)

1. **WeatherRepositoryImpl** (170+ lines, many complex methods)
   - No tests for `getDaysRange()` date gap calculation
   - No tests for date parsing and validation
   - No tests for network error handling
   - No tests for database caching logic

2. **MonthlyStatisticsViewModel** (100+ lines)
   - No tests for statistics calculation logic
   - No tests for date filtering by year/month
   - No tests for state transitions (Loading → Success → Error)
   - No tests for error states and recovery

3. **PlaceSummaryViewModel** (100+ lines)
   - No tests for weather data loading
   - No tests for monthly average temperature calculations
   - No tests for navigation events
   - No tests for year/month selection logic

4. **Database Layer** (SqlDelightDatabase)
   - No tests for saveWeatherResponse()
   - No tests for data consistency
   - No tests for getAllSavedPlaces()
   - No tests for transaction handling

5. **Network Layer** (NetworkService)
   - No tests for API error handling
   - No tests for serialization/deserialization
   - No tests for logging behavior
   - No tests for retry logic

---

## 2. Phase 1 Testing Plan (Next 3 Months)

### 2.1 High-Priority Unit Tests (Weeks 1-4)

#### 1. WeatherRepositoryImpl.generateDateList() Tests
- **Effort**: 2 hours | **Priority**: Critical
- **File**: `/Users/km/DEV/src/_sandbox/ReWinds/composeApp/src/commonTest/kotlin/core/WeatherRepositoryTest.kt`
- **Test Cases**:
  - `generateDateList_withValidRange_returnsAllDates()`
  - `generateDateList_withSingleDay_returnsOneDate()`
  - `generateDateList_withInvertedDates_returnsEmpty()`
  - `generateDateList_withInvalidFormat_returnsEmpty()`
  - `generateDateList_withYearBoundary_returnsCorrectDates()`

#### 2. WeatherRepositoryImpl.calculateDateGaps() Tests
- **Effort**: 2 hours | **Priority**: Critical
- **File**: `core/WeatherRepositoryTest.kt`
- **Test Cases**:
  - `calculateDateGaps_withConsecutiveDates_returnsOneGap()`
  - `calculateDateGaps_withMultipleGaps_returnsSeparateGaps()`
  - `calculateDateGaps_withSingleDate_returnsOneGap()`
  - `calculateDateGaps_withEmptyList_returnsEmpty()`
  - `calculateDateGaps_withUnsortedDates_returnsCorrectGaps()`

#### 3. MonthlyStatisticsViewModel.calculateStats() Tests
- **Effort**: 3 hours | **Priority**: Critical
- **File**: `/Users/km/DEV/src/_sandbox/ReWinds/composeApp/src/commonTest/kotlin/place/MonthlyStatisticsViewModelTest.kt` (NEW)
- **Test Cases**:
  - `calculateStats_withValidData_returnsCorrectAverages()`
  - `calculateStats_withMissingData_ignoresNulls()`
  - `calculateStats_withKiteableDays_countsCorrectly()`
  - `calculateStats_withEmptyData_returnsZeros()`
  - `calculateStats_withSingleDay_returnsMinMaxAsValue()`
  - `calculateStats_withNegativeTemps_handlesCorrectly()`

#### 4. MonthlyStatisticsViewModel.filterAndMapDaysForMonth() Tests
- **Effort**: 2 hours | **Priority**: High
- **File**: `place/MonthlyStatisticsViewModelTest.kt`
- **Test Cases**:
  - `filterAndMapDaysForMonth_withExactMonthData_returnsDays()`
  - `filterAndMapDaysForMonth_withMultipleYears_filtersCorrectly()`
  - `filterAndMapDaysForMonth_withNoDataForMonth_returnsEmpty()`
  - `filterAndMapDaysForMonth_withMonthBoundary_handlesCorrectly()`
  - `filterAndMapDaysForMonth_mapsToSummaryCorrectly()`

#### 5. PlaceSummaryViewModel State Transitions Tests
- **Effort**: 2.5 hours | **Priority**: High
- **File**: `/Users/km/DEV/src/_sandbox/ReWinds/composeApp/src/commonTest/kotlin/place/PlaceSummaryViewModelTest.kt` (NEW)
- **Test Cases**:
  - `uiState_initiallyLoading()`
  - `uiState_afterLoadSuccess_showsData()`
  - `uiState_afterLoadError_showsError()`
  - `selectedYear_initiallyMinValue()`
  - `selectedYear_afterSetYear_updatesState()`
  - `monthlyAverageTemps_updatesWhenYearChanges()`

#### 6. HomeViewModel Search Flow Tests
- **Effort**: 2 hours | **Priority**: High
- **File**: `/Users/km/DEV/src/_sandbox/ReWinds/composeApp/src/commonTest/kotlin/home/HomeViewModelSearchTest.kt` (NEW)
- **Test Cases**:
  - `search_withEmptyText_clearsResults()`
  - `search_withValidLocation_populatesResults()`
  - `search_withNetworkError_showsError()`
  - `search_debounceWorks()`
  - `addPlaceFromSearch_updatesPlaceList()`
  - `search_andAddPlace_removesSearchResults()`

#### 7. HomeViewModel Delete Flow Tests
- **Effort**: 1.5 hours | **Priority**: High
- **File**: `/Users/km/DEV/src/_sandbox/ReWinds/composeApp/src/commonTest/kotlin/home/HomeViewModelDeleteTest.kt` (NEW)
- **Test Cases**:
  - `deleteRequest_showsConfirmation()`
  - `deleteCancellation_hidesConfirmation()`
  - `deleteConfirmed_removesPlace()`
  - `deleteConfirmed_callsRepository()`
  - `deleteNonexistent_doesNotCrash()`

### 2.2 Secondary Unit Tests (Weeks 5-8)

#### 8. SqlDelightDatabase.saveWeatherResponse() Tests
- **Effort**: 3 hours | **Priority**: High
- **File**: `/Users/km/DEV/src/_sandbox/ReWinds/composeApp/src/commonTest/kotlin/core/SqlDelightDatabaseTest.kt` (NEW)
- **Coverage**: Data persistence, merging, hierarchy preservation

#### 9. SqlDelightDatabase Query Tests
- **Effort**: 2.5 hours | **Priority**: High
- **File**: `core/SqlDelightDatabaseTest.kt`
- **Coverage**: getAllSavedPlaces, getSavedPlaceFull, deletePlace operations

#### 10. NetworkService Error Handling Tests
- **Effort**: 2 hours | **Priority**: High
- **File**: `/Users/km/DEV/src/_sandbox/ReWinds/composeApp/src/commonTest/kotlin/core/NetworkServiceTest.kt` (NEW)
- **Coverage**: HTTP errors, connection failures, malformed responses

#### 11. Koin DI Module Tests
- **Effort**: 2 hours | **Priority**: Medium
- **File**: `/Users/km/DEV/src/_sandbox/ReWinds/composeApp/src/commonTest/kotlin/core/KoinModuleTest.kt` (NEW)
- **Coverage**: Module provision, singleton verification, service resolution

#### 12. DataMapping Tests
- **Effort**: 2 hours | **Priority**: Medium
- **File**: `/Users/km/DEV/src/_sandbox/ReWinds/composeApp/src/commonTest/kotlin/core/DataMappingTest.kt` (NEW)
- **Coverage**: Model conversion, null handling, consistency

### 2.3 Integration Tests (Phase 1)

#### 13. Database ↔ Repository Round-Trip
- **Effort**: 2 hours
- **File**: `/Users/km/DEV/src/_sandbox/ReWinds/composeApp/src/commonTest/kotlin/integration/DatabaseRepositoryIntegrationTest.kt` (NEW)
- **Scenario**: Save → Query → Verify → Update → Re-verify

#### 14. Repository with Mocked Network
- **Effort**: 2.5 hours
- **File**: `/Users/km/DEV/src/_sandbox/ReWinds/composeApp/src/commonTest/kotlin/integration/RepositoryNetworkIntegrationTest.kt` (NEW)
- **Scenario**: Caching + Network Fallback + Merge

#### 15. Full Search-to-Save Flow
- **Effort**: 2 hours
- **File**: `/Users/km/DEV/src/_sandbox/ReWinds/composeApp/src/commonTest/kotlin/integration/SearchToSaveIntegrationTest.kt` (NEW)
- **Scenario**: User search → add place → verify in list

### 2.4 Phase 1 Summary
- **Total Tests**: 67 tests (33 existing + 34 new)
- **Total Effort**: ~40 hours over 3 months
- **Target Coverage**: 50% line coverage, 70% critical path
- **Files to Create**: 12 new test files

---

## 3. Phase 2 Testing Plan (Months 4-6)

### 3.1 Goals
- **Total Tests**: 115 tests
- **Target Coverage**: 65% line, 85% critical path
- **Focus**: UI tests, platform-specific testing, edge cases

### 3.2 Key Additions
1. **HomeView Compose Tests** (3 hours) - Android UI
2. **PlaceSummaryView Tests** (2.5 hours) - Android UI
3. **Navigation UI Tests** (2 hours) - E2E
4. **iOS Manual UI Tests** (Setup) - In Xcode
5. **Large Dataset Tests** (1 hour) - Performance baseline
6. **Transaction Consistency Tests** (1.5 hours) - Database

---

## 4. Phase 3 Long-Term Goals (Months 7+)

### 4.1 Targets
- **Total Tests**: 165+ tests
- **Target Coverage**: 75% line, 90% critical path
- **Focus**: Performance, edge cases, all UI flows

### 4.2 Additions
1. **iOS UI Tests** (10 tests) - In Xcode
2. **Performance Baseline Tests** (5 tests)
3. **Edge Case Coverage** (comprehensive)
4. **Documentation & Patterns** (test guide)

---

## 5. Platform-Specific Considerations

### 5.1 iOS Testing

**Challenges & Solutions**:
- **Kotlin/Native Compilation**: 40+ minutes first run
  - Solution: Use conditional test targets, iOS subset only
  - Status: ✅ Already implemented

- **Memory Constraints**: 6GB heap limitation
  - Solution: Keep iOS tests minimal, feature gating
  - Status: ✅ Existing approach working

- **Simulator-Only Testing**: Device builds fail
  - Solution: Use arm64 simulator
  - Command: `./gradlew :composeApp:iosSimulatorArm64Test`

- **No Xcode Integration in Gradle Tests**
  - Solution: Separate Gradle tests (unit/integration) from Xcode tests (UI)

**Recommended iOS Test Structure**:
```
iosTest/kotlin/                    (Gradle tests - 5-10 tests)
├── core/
│   ├── PlatformFunctionsTest.kt    ✅ Exists
│   └── DatabaseDriverFactoryTest.kt ✅ Exists
└── integration/
    ├── DatabaseIntegrationTest.kt   ✅ Exists
    └── CoroutineDispatchersTest.kt  ✅ Exists

iosApp/UITests/                    (Xcode tests - manual)
├── AppLaunchUITest.swift
├── NavigationUITest.swift
└── DataPersistenceUITest.swift
```

### 5.2 Android Testing

**Advantages**: Full compilation support, no memory constraints

**Key Tests Unique to Android**:
1. Permissions handling (database file access)
2. Activity lifecycle integration
3. Android-specific database driver
4. Compose UI tests
5. File system operations

---

## 6. Implementation Roadmap

### Week 1-2: Foundation (Unit Tests for Core Logic)
**Effort**: 8 hours
1. WeatherRepositoryImpl.generateDateList() Tests (2 hrs)
2. WeatherRepositoryImpl.calculateDateGaps() Tests (2 hrs)
3. MonthlyStatisticsViewModel.calculateStats() Tests (2 hrs)
4. Koin DI Module Tests (2 hrs)

### Week 3-4: Search & Delete Flows (ViewModel Tests)
**Effort**: 5 hours
5. HomeViewModel.search() Flow Tests (2 hrs)
6. HomeViewModel.delete() Flow Tests (1.5 hrs)
7. PlaceSummaryViewModel State Transitions Tests (1.5 hrs)

### Week 5-6: Database & Network (Integration Tests)
**Effort**: 8 hours
8. SqlDelightDatabase Tests (3 hrs)
9. NetworkService Error Handling Tests (2 hrs)
10. Database ↔ Repository Integration (2 hrs)
11. Repository Network Integration (1 hr)

### Week 7-8: Advanced Integration (Complex Flows)
**Effort**: 6 hours
12. Full Search-to-Save Flow (2 hrs)
13. Navigation State Persistence (1.5 hrs)
14. Data Mapping Tests (1.5 hrs)
15. Large Dataset Handling (1 hr)

### Month 2-3: UI Tests (Android Focus)
**Effort**: 8 hours
16. HomeView Compose Tests (3 hrs)
17. PlaceSummaryView Tests (2.5 hrs)
18. Navigation UI Tests (2 hrs)
19. iOS Manual UI Tests (Setup only)

### Month 3: Polish & Optimization
**Effort**: 5 hours
20. Edge Case Tests (2 hrs)
21. Performance Baseline Tests (2 hrs)
22. Test Documentation (1 hr)

---

## 7. Coverage Targets by Phase

### Phase 1 (3 Months)
| Category | Target | Rationale |
|----------|--------|-----------|
| Unit Tests | 50 tests | Add repository, viewmodel, network tests |
| Integration Tests | 12 tests | Database, network, DI integration |
| E2E Tests | 5 tests | Critical user flows |
| **Total** | **67 tests** | +30 tests, 80% increase |

**Lines of Code Coverage**:
- Critical layers (ViewModels, Repositories): **80%**
- Database layer: **70%**
- Network layer: **70%**
- Utility/Helper functions: **60%**

### Phase 2 (6 Months)
| Category | Target |
|----------|--------|
| Unit Tests | 75 |
| Integration Tests | 20 |
| E2E Tests | 12 |
| UI Tests (Android) | 8 |
| **Total** | **115** |

**Lines of Code Coverage**:
- Critical layers: **85%**
- Database: **80%**
- Network: **80%**
- UI: **40%** (Compose + Android specific)

### Phase 3 (Long-term)
| Category | Target |
|----------|--------|
| Unit Tests | 90 |
| Integration Tests | 25 |
| E2E Tests | 20 |
| UI Tests (Android) | 15 |
| UI Tests (iOS) | 10 |
| Performance Tests | 5 |
| **Total** | **165** |

**Lines of Code Coverage**: **75% overall, 90% critical**

---

## 8. CI/CD Integration

### 8.1 Recommended CI Pipeline

```yaml
# Test Android (fast, reliable)
- Run: ./gradlew test buildAndroidOnly          # 5-10 minutes
  When: Every commit
  Required: Yes

# Test iOS (slow, optional in CI)
- Run: ./gradlew :composeApp:iosSimulatorArm64Test  # 40+ minutes
  When: Main branch only
  Required: No

# Coverage Report
- Run: ./gradlew test jacocoTestReport         # Generate coverage HTML
  When: Daily
  Required: No (alert if <45%)
```

### 8.2 Local Development Testing

```bash
# Before committing
./gradlew buildAndroidOnly test          # 5 min - must pass

# Before pushing to main
./gradlew build --no-daemon               # 10 min - full build

# Optional local iOS test
./gradlew :composeApp:iosSimulatorArm64Test  # 40+ min
```

---

## 9. Dependencies & Configuration

### 9.1 Test Dependencies

Already configured in `build.gradle.kts`:
```gradle
commonTest.dependencies {
    implementation(kotlin("test"))
    implementation(libs.kotlinx.coroutines.test)
}
```

To add for Phase 2:
```gradle
commonTest.dependencies {
    implementation(libs.mockk)  // For mocking
}

androidTest.dependencies {
    implementation(libs.androidx.compose.ui.test)
    implementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

### 9.2 Gradle Configuration

Add to `gradle.properties`:
```properties
# Disable devirtualization to speed up iOS test compilation
kotlin.native.disableDevirtualization=true

# Increase timeout for Gradle Test tasks
gradle.test.timeout=1200000  # 20 minutes for iOS tests
```

---

## 10. Success Metrics

### Phase 1 Success Criteria
- [ ] 50 unit tests passing
- [ ] 45%+ line coverage
- [ ] Zero regressions in existing tests
- [ ] All critical ViewModels tested
- [ ] Database CRUD operations tested

### Phase 2 Success Criteria
- [ ] 115 tests passing
- [ ] 60%+ line coverage
- [ ] 8+ Android UI tests
- [ ] iOS tests runnable
- [ ] No critical paths untested

### Phase 3 Success Criteria
- [ ] 165+ tests passing
- [ ] 75%+ line coverage
- [ ] 25+ UI tests (Android + iOS)
- [ ] Performance baseline established
- [ ] Test guide documented

---

## 11. Test File Organization

### Directory Structure

```
composeApp/src/commonTest/kotlin/
├── core/
│   ├── WeatherRepositoryTest.kt           (NEW - Phase 1)
│   ├── SqlDelightDatabaseTest.kt          (NEW - Phase 1)
│   ├── NetworkServiceTest.kt              (NEW - Phase 1)
│   ├── KoinModuleTest.kt                  (NEW - Phase 1)
│   ├── DataMappingTest.kt                 (NEW - Phase 1)
│   ├── PlatformFunctionsTest.kt           ✅ Exists
│   └── DatabaseDriverFactoryTest.kt       ✅ Exists
├── home/
│   ├── HomeViewTest.kt                    ✅ Exists
│   ├── HomeViewModelSearchTest.kt         (NEW - Phase 1)
│   └── HomeViewModelDeleteTest.kt         (NEW - Phase 1)
├── place/
│   ├── PlaceSummaryViewModelTest.kt       ✅ Exists
│   ├── MonthlyStatisticsViewModelTest.kt  (NEW - Phase 1)
│   └── PlaceSummaryViewModelTest.kt       (NEW - Phase 1)
├── integration/
│   ├── DatabaseRepositoryIntegrationTest.kt      (NEW - Phase 1)
│   ├── RepositoryNetworkIntegrationTest.kt       (NEW - Phase 1)
│   ├── SearchToSaveIntegrationTest.kt            (NEW - Phase 1)
│   ├── DatabaseIntegrationTest.kt                ✅ Exists
│   └── CoroutineDispatchersTest.kt               ✅ Exists
└── e2e/
    ├── AppStartupE2ETest.kt               (NEW - Phase 1)
    └── AppIntegrationE2ETest.kt           (NEW - Phase 2)

composeApp/src/androidTest/kotlin/         (NEW - Phase 2)
├── home/
│   └── HomeViewAndroidTest.kt
├── place/
│   └── PlaceSummaryViewAndroidTest.kt
└── integration/
    └── AppIntegrationAndroidTest.kt

composeApp/src/iosTest/kotlin/             ✅ Already configured
├── core/
│   ├── PlatformFunctionsTest.kt
│   └── DatabaseDriverFactoryTest.kt
└── integration/
    ├── DatabaseIntegrationTest.kt
    └── CoroutineDispatchersTest.kt
```

---

## 12. Quick Reference

### Run Tests
```bash
# Android only (fast)
./gradlew buildAndroidOnly test

# iOS simulator (slow)
./gradlew :composeApp:iosSimulatorArm64Test

# Coverage report
./gradlew test jacocoTestReport
# View: build/reports/jacoco/test/html/index.html
```

### Current Test Status
- **Total Tests**: 33 existing + 34 Phase 1 planned
- **Current Coverage**: ~30% (estimate)
- **Target Coverage**: 50% → 65% → 75%
- **Implementation Timeline**: 3 + 6 + ongoing months

---

## 13. References

- **Test Framework**: Kotlin `kotlin.test`, `kotlinx.coroutines.test`
- **Database**: SQLDelight with iOS sqlite3
- **Networking**: Ktor client
- **DI**: Koin 4.1.1
- **State Management**: StateFlow + Flow
- **Architecture**: MV* pattern (all logic in ViewModels)

