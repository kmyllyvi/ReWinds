# ReWinds Architecture & Coding Rules

## 🏛️ CRITICAL: Keep All Logic in ViewModels (MV* Pattern)

**RULE**: ALL business logic, state management, and conditional rendering belongs in **ViewModels**, NEVER in Composables/Views.

### Why This Matters
- ✅ **Testability**: ViewModels are easy to unit test
- ✅ **Reusability**: Logic can be used across multiple Views
- ✅ **Separation of Concerns**: Views are purely presentational
- ✅ **Maintainability**: Clear boundaries make code easier to understand

### The Pattern

**❌ ANTI-PATTERN - Logic in View (DON'T DO THIS)**
```kotlin
@Composable
fun HomeView() {
    var showMenu by remember { mutableStateOf(false) }           // ❌ WRONG
    var debugMessage by remember { mutableStateOf("") }          // ❌ WRONG

    // Conditional rendering logic in View ❌
    if (showDebugMenu) {
        DebugMenu(...)
    }

    // Event handling with side effects ❌
    Button(onClick = {
        showDebugMenu = !showDebugMenu  // ❌ State mutation in View
    })
}
```

**✅ CORRECT PATTERN - Logic in ViewModel**
```kotlin
// ViewModel (HomeViewModel.kt)
class HomeViewModel : ViewModel() {
    private val _showDebugMenu = MutableStateFlow(false)
    val showDebugMenu: StateFlow<Boolean> = _showDebugMenu.asStateFlow()

    fun toggleDebugMenu() {
        _showDebugMenu.value = !_showDebugMenu.value
    }

    suspend fun exportDatabase(): Result<String> {
        // All business logic here
    }
}

// View (HomeView.kt)
@Composable
fun HomeView(vm: HomeViewModel = koinViewModel()) {
    val showDebugMenu by vm.showDebugMenu.collectAsState()  // ✅ Collect state

    Column {
        // Just render based on state
        if (showDebugMenu) {
            DebugMenu(...)
        }

        Button(onClick = { vm.toggleDebugMenu() }) {  // ✅ Call VM method
            Text("Toggle Debug Menu")
        }
    }
}
```

### Key Rules

1. **Views collect state from VMs**: Use `collectAsState()` or similar
2. **Views call ViewModel methods**: Events → VM methods → state updates
3. **Views NEVER create mutable state**: No `remember { mutableStateOf(...) }`
4. **Views NEVER contain**:
   - Business logic
   - Conditional state management
   - Database operations
   - Network calls
   - Calculations (except simple UI-only ones)

5. **ViewModels expose state as**: `StateFlow<T>`, `Flow<T>`, or read-only properties
6. **ViewModels handle**:
   - State management
   - Business operations
   - Event handling
   - Data transformations

## 📁 File Organization

- **View files** (Composables): `src/commonMain/kotlin/*/SomethingView.kt`
  - Pure UI composition
  - No mutable state
  - No logic

- **ViewModel files**: `src/commonMain/kotlin/*/SomethingViewModel.kt`
  - Holds state (StateFlow/MutableStateFlow)
  - Contains methods for operations
  - Handles business logic

## Testing Implications

**ViewModels are testable**:
```kotlin
@Test
fun testToggleDebugMenu() {
    val vm = HomeViewModel()
    assertTrue(vm.showDebugMenu.value == false)
    vm.toggleDebugMenu()
    assertTrue(vm.showDebugMenu.value == true)
}
```

**Views are hard to test** - keep them simple so they don't need testing

---

**Enforced by**: Project code review standards
**Applied to**: All Composable functions and ViewModels
