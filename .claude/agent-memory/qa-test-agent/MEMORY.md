# QA Test Agent Memory - ReWinds Project

## Test Suite Status (Feb 19, 2026)

### Android Unit Tests
- Command: `./gradlew :composeApp:testDebugUnitTest --no-daemon`
- Results: 32 tests, 23 pass (71.9%), 9 fail (28.1%)
- Duration: ~34-37 seconds

#### Passing Suites (100% pass rate)
1. AppViewModelTest (4 tests) - NEW - validates MV* pattern for showContent StateFlow
2. AppStartupTest (3 tests) - app initialization
3. NavigatorBackStackTest (5 tests) - back stack navigation
4. PlaceSummaryViewModelTest (6 tests) - place summary state management

#### Failing Suites (pre-existing issues)
1. HomeViewModelTest (8/8 fail) - Missing Dispatchers.setMain() for ViewModel tests using viewModelScope
2. NavigatorTest (1/6 fail) - SnapshotStateList.removeLast() unavailable at runtime; use removeAt(size-1)

### iOS Tests
- Command: `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64 --no-daemon`
- Result: BUILD SUCCESSFUL (1m 18s)
- Tests compile: PlatformFunctionsTest, DatabaseDriverFactoryTest, CoroutineDispatchersTest, DatabaseIntegrationTest

## Recent Architectural Changes Validated

### Changes Verified (Feb 19 commits)
- ✅ AppViewModel with StateFlow(showContent) - MV* compliant
- ✅ App.kt uses koinViewModel() for AppViewModel - proper pattern
- ✅ Defensive Koin initialization with exception handling
- ✅ No mutableState in Composable files
- ✅ Platform-specific code isolated in iOS/Android main

### New Tests Added
- AppViewModelTest.kt (4 tests) - 100% pass rate
  - Validates showContent StateFlow behavior
  - Tests state transitions (true/false)
  - Confirms MV* pattern enforcement in UI layer

## Known Issues & Fixes Needed

### Issue 1: HomeViewModelTest Dispatcher Setup
- Cause: HomeViewModel uses viewModelScope without Main dispatcher
- Fix: Add `Dispatchers.setMain(Dispatchers.Unconfined)` in @BeforeTest
- Impact: 8 tests in HomeViewModelTest

### Issue 2: NavigatorImpl.navigateBack()
- Cause: SnapshotStateList.removeLast() not available in JVM test runtime
- Fix: Replace with `backStack.removeAt(backStack.size - 1)`
- Impact: 1 test in NavigatorTest

## Test Patterns Observed

### ViewModel Testing Pattern
- Common tests use direct instantiation: `HomeViewModel(mockRepository)`
- StateFlow values accessed via `.value` property (synchronous in tests)
- No need for `runTest` or coroutine scopes for simple state reads

### Coroutine Testing
- ViewModel tests fail when they use viewModelScope without Dispatchers setup
- Solution: Use `Dispatchers.setMain(Dispatchers.Unconfined)` in setup
- For async tests, use `runTest { }` with proper collectors

## Build Configuration Notes
- Gradle Heap: 6GB (gradle.properties)
- Kotlin/Native: -Xno-devirtualization flag
- iOS: cocoapods manages sqlite3
- Test compilation (iOS): ~78 seconds

## Architecture Compliance
- MV* Pattern: ✅ Enforced - AppViewModel manages state, Composables are stateless
- DI/Koin: ✅ Defensive initialization with exception handling
- Platform Separation: ✅ iOS/Android code properly isolated
- Database: ✅ SQLDelight with sqlite3 driver
