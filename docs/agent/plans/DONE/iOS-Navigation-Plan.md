# iOS Navigation Support Implementation Plan

**Status:** Approved & Ready for Implementation
**Estimated Duration:** 4-5 hours
**Created:** February 2026

## Overview

This document outlines the step-by-step plan to add iOS support by migrating from Android-only Navigation 2.x to multiplatform Navigation 3 (officially supported in Compose Multiplatform 1.10.0).

## Current State

- **Navigation Library:** `androidx.navigation.compose` 2.9.1 (Android-only)
- **Location:** All navigation code in `commonMain` (incompatible with iOS)
- **3 Screens:**
  - Home (place listing)
  - PlaceSummary (year/month selection)
  - MonthlyStatistics (detailed stats)
- **Existing Abstractions:** Navigator interface exists but is unused
- **ViewModel Pattern:** Already using multiplatform NavigationEvent pattern ✓

## Target State

- Navigation 3 with full multiplatform support (Android & iOS)
- Type-safe navigation using Kotlin serialization
- Navigator interface fully utilized
- Zero Android-specific APIs in commonMain
- Native iOS back swipe gesture support
- Test coverage for navigation logic

## Why Navigation 3?

**Official JetBrains Solution**
- Part of Compose Multiplatform 1.10.0 (already using)
- Native iOS gestures built-in
- Type-safe with Kotlin serialization
- Future-proof for web/desktop

**Why Not Alternatives?**
- ❌ Custom expect/actual: More maintenance, missing iOS gestures
- ❌ Voyager/Decompose: Added dependency, different patterns

## Implementation Steps

### Phase 1: Dependency Management (5 minutes)

#### Step 1: Update Navigation Version
**File:** `gradle/libs.versions.toml`
- Change `navigationCompose = "2.9.1"` to `navigationCompose = "3.0.0"` (or latest 3.x)

**Why:** Navigation 3 is the multiplatform-compatible version.

---

### Phase 2: Create Navigation Abstractions (30 minutes)

#### Step 2: Create Serializable Routes
**New File:** `composeApp/src/commonMain/kotlin/core/NavigationRoutes.kt`

```kotlin
package core

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute

@Serializable
data object HomeRoute : NavRoute

@Serializable
data class PlaceSummaryRoute(val placeName: String) : NavRoute

@Serializable
data class MonthlyStatisticsRoute(
    val placeName: String,
    val year: Int,
    val month: Int
) : NavRoute
```

**Why:** Navigation 3 uses serialization for type-safe routing instead of string-based routes. This eliminates URL encoding/decoding.

#### Step 3: Extract Navigator Interface
**New File:** `composeApp/src/commonMain/kotlin/core/Navigator.kt`

```kotlin
package core

interface Navigator {
    fun navigateToHome()
    fun navigateToPlaceSummary(placeName: String)
    fun navigateToMonthlyStatistics(placeName: String, year: Int, month: Int)
    fun navigateBack()
    fun canNavigateBack(): Boolean
}
```

**Why:** Clean abstraction that both views and tests can use.

#### Step 4: Implement Navigator
**New File:** `composeApp/src/commonMain/kotlin/core/NavigatorImpl.kt`

```kotlin
package core

import androidx.compose.runtime.snapshots.SnapshotStateList

class NavigatorImpl(
    private val backStack: SnapshotStateList<NavRoute>
) : Navigator {

    override fun navigateToHome() {
        backStack.clear()
        backStack.add(HomeRoute)
    }

    override fun navigateToPlaceSummary(placeName: String) {
        backStack.add(PlaceSummaryRoute(placeName))
    }

    override fun navigateToMonthlyStatistics(placeName: String, year: Int, month: Int) {
        backStack.add(MonthlyStatisticsRoute(placeName, year, month))
    }

    override fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeLast()
        }
    }

    override fun canNavigateBack(): Boolean = backStack.size > 1
}
```

**Why:** Wraps Navigation 3's back stack with our Navigator interface.

---

### Phase 3: Refactor Core Navigation (45 minutes)

#### Step 5: Refactor Router.kt
**File:** `composeApp/src/commonMain/kotlin/core/Router.kt`

Complete rewrite to use Navigation 3 API:

```kotlin
package core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.*
import home.HomeView
import place.PlaceSummaryView
import place.MonthlyStatisticsView

@Composable
fun Navigation() {
    // Create multiplatform navigation back stack
    val backStack = rememberNavBackStack<NavRoute>(HomeRoute)

    // Create navigator instance
    val navigator = remember(backStack) {
        NavigatorImpl(backStack)
    }

    // Display current route
    NavDisplay(backStack = backStack) { route ->
        when (route) {
            is HomeRoute -> {
                HomeView(navigator = navigator)
            }
            is PlaceSummaryRoute -> {
                PlaceSummaryView(
                    onBackClick = { navigator.navigateBack() },
                    navigator = navigator
                )
            }
            is MonthlyStatisticsRoute -> {
                MonthlyStatisticsView(
                    placeName = route.placeName,
                    year = route.year,
                    month = route.month,
                    onBackClick = { navigator.navigateBack() }
                )
            }
        }
    }
}
```

**Remove:**
- Old `Screen` sealed class (lines 32-49)
- `AppNavigator` class (lines 117-136)
- All androidx.navigation imports except compose utils

**Why:** Navigation 3 uses different APIs for back stack management and route navigation.

---

### Phase 4: Update Views (30 minutes)

#### Step 6: Update HomeView
**File:** `composeApp/src/commonMain/kotlin/home/HomeView.kt`

**Change signature (around line 33):**
```kotlin
// Before:
fun HomeView(vm: HomeViewModel = koinViewModel(), navController: NavController)

// After:
fun HomeView(vm: HomeViewModel = koinViewModel(), navigator: Navigator)
```

**Update navigation event handler:**
```kotlin
LaunchedEffect(Unit) {
    vm.navigationEvent.collect { event ->
        when (event) {
            is NavigationEvent.ToPlaceSummary -> {
                navigator.navigateToPlaceSummary(event.placeName)
            }
        }
    }
}
```

**Remove imports:**
- `import androidx.navigation.NavController`

#### Step 7: Update PlaceSummaryView
**File:** `composeApp/src/commonMain/kotlin/place/PlaceSummaryView.kt`

**Change signature (around line 78):**
```kotlin
// Before:
fun PlaceSummaryView(onBackClick: () -> Unit, navController: NavController, vm: PlaceSummaryViewModel = koinViewModel())

// After:
fun PlaceSummaryView(onBackClick: () -> Unit, navigator: Navigator, vm: PlaceSummaryViewModel = koinViewModel())
```

**Update navigation event handler:**
```kotlin
LaunchedEffect(Unit) {
    vm.navigationEvent.collect { event ->
        when (event) {
            is NavigationEvent.ToMonthlySummary -> {
                navigator.navigateToMonthlyStatistics(
                    event.placeName,
                    event.year,
                    event.month
                )
            }
        }
    }
}
```

**Remove imports:**
- `import androidx.navigation.NavController`

---

### Phase 5: Update ViewModel Layer (20 minutes)

#### Step 8: Update PlaceSummaryViewModel
**File:** `composeApp/src/commonMain/kotlin/place/PlaceSummaryViewModel.kt`

**Change constructor (around lines 48-54):**
```kotlin
// Before:
class PlaceSummaryViewModel(
    savedStateHandle: SavedStateHandle,
    private val weatherRepository: WeatherRepository
) : ViewModel() {
    val placeName: String = savedStateHandle.get<String>("placeName")
        ?: throw IllegalArgumentException("placeNameArg not found in SavedStateHandle")

// After:
class PlaceSummaryViewModel(
    route: PlaceSummaryRoute,
    private val weatherRepository: WeatherRepository
) : ViewModel() {
    val placeName: String = route.placeName
```

**Why:** Navigation 3 provides route objects directly to ViewModels.

#### Step 9: Update MonthlyStatisticsViewModel
**File:** `composeApp/src/commonMain/kotlin/place/MonthlyStatisticsViewModel.kt`

**Change constructor (around lines 48-56):**
```kotlin
// Before:
class MonthlyStatisticsViewModel(
    savedStateHandle: SavedStateHandle,
    private val weatherRepository: WeatherRepository
) : ViewModel() {
    val placeName: String? = savedStateHandle.get<String>("placeName")
    val year: Int? = savedStateHandle.get<Int>("year")
    val month: Int? = savedStateHandle.get<Int>("month")

// After:
class MonthlyStatisticsViewModel(
    route: MonthlyStatisticsRoute,
    private val weatherRepository: WeatherRepository
) : ViewModel() {
    val placeName: String = route.placeName
    val year: Int = route.year
    val month: Int = route.month
```

---

### Phase 6: Update Dependency Injection (10 minutes)

#### Step 10: Update DI.kt
**File:** `composeApp/src/commonMain/kotlin/DI.kt`

**Remove:**
```kotlin
factory<Navigator> { (navController: NavHostController) -> AppNavigator(navController) }
```

**Why:** Navigator is now created in Router.kt composable, not through DI.

**Keep unchanged:**
- All ViewModel registrations (Koin handles route injection automatically)
- Database, networking, and repository setup

---

### Phase 7: Add Tests (30 minutes)

#### Step 11: Create NavigatorTest
**New File:** `composeApp/src/commonTest/kotlin/core/NavigatorTest.kt`

```kotlin
package core

import androidx.compose.runtime.mutableStateListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class NavigatorTest {

    @Test
    fun `initial state has home route`() {
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        assertEquals(1, backStack.size)
        assertEquals(HomeRoute, backStack[0])
        assertFalse(navigator.canNavigateBack())
    }

    @Test
    fun `navigateToPlaceSummary adds route to back stack`() {
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        navigator.navigateToPlaceSummary("Test Place")

        assertEquals(2, backStack.size)
        assertEquals(HomeRoute, backStack[0])
        assertTrue(backStack[1] is PlaceSummaryRoute)
        assertEquals("Test Place", (backStack[1] as PlaceSummaryRoute).placeName)
        assertTrue(navigator.canNavigateBack())
    }

    @Test
    fun `navigateToMonthlyStatistics preserves all parameters`() {
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        navigator.navigateToMonthlyStatistics("Test", 2024, 5)

        assertEquals(2, backStack.size)
        val route = backStack[1] as MonthlyStatisticsRoute
        assertEquals("Test", route.placeName)
        assertEquals(2024, route.year)
        assertEquals(5, route.month)
    }

    @Test
    fun `navigateBack removes last route`() {
        val backStack = mutableStateListOf<NavRoute>(
            HomeRoute,
            PlaceSummaryRoute("Test")
        )
        val navigator = NavigatorImpl(backStack)

        navigator.navigateBack()

        assertEquals(1, backStack.size)
        assertEquals(HomeRoute, backStack[0])
        assertFalse(navigator.canNavigateBack())
    }

    @Test
    fun `navigateBack does nothing when only home remains`() {
        val backStack = mutableStateListOf<NavRoute>(HomeRoute)
        val navigator = NavigatorImpl(backStack)

        navigator.navigateBack()

        assertEquals(1, backStack.size)
        assertEquals(HomeRoute, backStack[0])
    }

    @Test
    fun `navigateToHome clears back stack`() {
        val backStack = mutableStateListOf<NavRoute>(
            HomeRoute,
            PlaceSummaryRoute("Test1"),
            MonthlyStatisticsRoute("Test2", 2024, 5)
        )
        val navigator = NavigatorImpl(backStack)

        navigator.navigateToHome()

        assertEquals(1, backStack.size)
        assertEquals(HomeRoute, backStack[0])
        assertFalse(navigator.canNavigateBack())
    }
}
```

---

## File Summary

### New Files to Create (4)
1. `composeApp/src/commonMain/kotlin/core/NavigationRoutes.kt`
2. `composeApp/src/commonMain/kotlin/core/Navigator.kt`
3. `composeApp/src/commonMain/kotlin/core/NavigatorImpl.kt`
4. `composeApp/src/commonTest/kotlin/core/NavigatorTest.kt`

### Files to Modify (7)
1. `gradle/libs.versions.toml` - Update navigation version
2. `composeApp/src/commonMain/kotlin/core/Router.kt` - Complete refactor
3. `composeApp/src/commonMain/kotlin/home/HomeView.kt` - Remove NavController
4. `composeApp/src/commonMain/kotlin/place/PlaceSummaryView.kt` - Remove NavController
5. `composeApp/src/commonMain/kotlin/place/PlaceSummaryViewModel.kt` - Use route object
6. `composeApp/src/commonMain/kotlin/place/MonthlyStatisticsViewModel.kt` - Use route object
7. `composeApp/src/commonMain/kotlin/DI.kt` - Remove NavHostController factory

### Files Unchanged
- All ViewModel navigation events (already multiplatform)
- `place/MonthlyStatisticsView.kt`
- All expect/actual implementations
- iOS/Android entry points

---

## Verification Checklist

### 1. Compilation ✓
```bash
./gradlew compileCommonMainKotlinMetadata --no-daemon
```

### 2. Unit Tests ✓
```bash
./gradlew commonTest --no-daemon
```
Verify all NavigatorTest cases pass.

### 3. Android Build & Test ✓
```bash
./gradlew :composeApp:assembleDebug --no-daemon
```
- Navigate: Home → select place → PlaceSummary → select month → MonthlyStatistics
- Test back button at each level

### 4. iOS Build & Test ✓
```bash
./gradlew :composeApp:linkDebugFrameworkIosArm64 --no-daemon
```
- Same navigation flow as Android
- Swipe back gesture should work
- Native iOS animations

### 5. Edge Cases ✓
- Deep navigation → navigate to Home
- Rapid navigation (multiple taps)
- Back when already at Home
- Special characters in place names

---

## Risk Assessment

### Low Risk ✓
- ViewModels don't change event logic
- Existing expect/actual patterns untouched
- No business logic changes

### Medium Risk ⚠️
- **Navigation 3 API differences**
  - Mitigation: Follow official docs, thorough testing
- **Serialization configuration**
  - Mitigation: Test all routes, use sealed interface pattern
- **ViewModel injection**
  - Mitigation: Test with Koin, verify route injection

### High Risk ⚠️
- **Breaking change from Navigation 2.x**
  - Mitigation: Feature branch, extensive testing before merge
  - Rollback: Revert commit if issues found
  - Timeline: 1-2 days testing on both platforms

---

## Implementation Timeline

| Phase | Duration | Tasks |
|-------|----------|-------|
| 1. Dependencies | 5 min | Update version in gradle |
| 2. Abstractions | 30 min | Create route definitions, Navigator interface, implementation |
| 3. Router | 45 min | Refactor Router.kt to Navigation 3 API |
| 4. Views | 30 min | Update HomeView, PlaceSummaryView |
| 5. ViewModels | 20 min | Update parameter injection in ViewModels |
| 6. DI | 10 min | Remove NavHostController factory |
| 7. Tests | 30 min | Create NavigatorTest with full coverage |
| **Verification** | **1-2 hours** | **Compile, test, build on both platforms** |
| **Total** | **4-5 hours** | |

---

## Key Points

✓ Navigation 3 officially supports iOS in Compose Multiplatform 1.10.0
✓ No platform-specific code needed
✓ Back swipe gesture works automatically on iOS
✓ Type-safe navigation with serialization
✓ Follows existing project patterns (expect/actual, Koin, etc.)
✓ No changes to business logic or data layer
✓ ViewModels' NavigationEvent pattern unchanged (already multiplatform)

---

## Reference

- [Compose Multiplatform 1.10.0 Release](https://blog.jetbrains.com/kotlin/2026/01/compose-multiplatform-1-10-0/)
- [Navigation 3 Documentation](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html)
- [Kotlin Serialization Guide](https://kotlinlang.org/docs/serialization.html)
