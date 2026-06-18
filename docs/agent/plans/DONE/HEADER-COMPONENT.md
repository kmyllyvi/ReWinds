# Session: Mar 6 - 2026 (Part 2) - UI Improvements

**Started**: Mar 6, 2026 (afternoon)
**Status**: In Progress

---

## Session Goal
UI consistency and component reusability improvements. Start with refactoring headers across the app into a reusable component.

---

## Phase 1: Header Component

### Design Reference
Split-layout header with:
- Left side: Logo + app name or back button + title
- Right side: Action buttons/icons
- Light gray shaded background container

### Current Header Implementations

**1. HomeView** (`home/HomeView.kt`)
- Custom `HomeHeader()` function
- Layout: Logo + "ReWinds" (left) | Settings + Chat icons (right)
- Padding: 16dp horizontal, 12dp vertical
- No background color (just padding)

**2. PlaceSummaryView** (`place/PlaceSummaryView.kt`)
- Custom Row layout
- Layout: Back button + Place name (left) | Info icon button (right)
- Info button has rounded background + primary color
- Same padding as HomeView

**3. MonthlyStatisticsView** (`place/MonthlyStatisticsView.kt`)
- Uses Material3 `TopAppBar` with Scaffold
- Layout: Back button + title (left) | (right side TBD)

**4. SettingsView** (`settings/SettingsView.kt`)
- Custom back button + "Settings" title
- Basic layout, no dedicated header function
- Padding similar to others

**5. ChatView** (`ai/ChatView.kt`)
- Custom back button + "Chat" title
- Minimal header design

### Inconsistencies Found
- ❌ HomeView: Custom function, no background
- ❌ PlaceSummaryView: Inline Row, right icon has background
- ❌ MonthlyStatisticsView: Material3 TopAppBar
- ❌ SettingsView: Basic inline layout
- ❌ ChatView: Basic inline layout
- Padding and sizing varies
- Icon styling inconsistent (some have backgrounds, some don't)

---

## AppHeader Component Design

### API Specification
```kotlin
@Composable
fun AppHeader(
    title: String,
    modifier: Modifier = Modifier,
    leftContent: @Composable (() -> Unit)? = null,    // Custom left (e.g., back button)
    rightContent: @Composable (() -> Unit)? = null,   // Custom right (e.g., action buttons)
    onBackClick: (() -> Unit)? = null,                // If provided, shows back button
    showLogo: Boolean = true,                          // Show ReWinds logo + name on left
)
```

### Usage Examples
```kotlin
// Home screen (logo + icons on right)
AppHeader(
    title = "ReWinds",
    showLogo = true,
    rightContent = {
        IconButton(onClick = { navigator.navigateToSettings() }) {
            Icon(Icons.Default.Settings, "Settings")
        }
        IconButton(onClick = { navigator.navigateToChat() }) {
            Icon(Icons.Default.Chat, "Chat")
        }
    }
)

// Place details (back button + title + info icon)
AppHeader(
    title = placeName,
    onBackClick = { onBackClick() },
    rightContent = {
        IconButton(onClick = { /* ... */ }) {
            Icon(Icons.Filled.Info, "Info",
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary))
        }
    }
)

// Settings (just back button + title)
AppHeader(
    title = "Settings",
    onBackClick = { navigator.back() }
)
```

---

## Implementation Checklist
- [x] Design component API (props/parameters)
- [x] Implement AppHeader component in `components/` folder
- [x] Refactor HomeView to use AppHeader
- [x] Refactor PlaceSummaryView to use AppHeader
- [x] Refactor MonthlyStatisticsView to use AppHeader
- [x] Refactor SettingsView to use AppHeader
- [x] Refactor ChatView to use AppHeader
- [ ] Test across all screen sizes
- [ ] Verify visual consistency with design mockup

## Changes Made

### New Component
- **components/AppHeader.kt** - Reusable header component with:
  - Split layout (left + right content areas)
  - System background (no custom shading)
  - Flexible API supporting all 5 screen types
  - Logo support for home screen
  - Back button support for detail screens

### Refactored Screens
1. **HomeView**: Replaced `HomeHeader()` function → Uses AppHeader with logo + 2 action buttons
2. **PlaceSummaryView**: Replaced inline Row → Uses AppHeader with back + title + info button
3. **MonthlyStatisticsView**: Replaced Material3 TopAppBar + Scaffold → Uses AppHeader with back + title
4. **SettingsView**: Replaced inline Row header → Uses AppHeader with back + title
5. **ChatView**: Replaced `ChatHeader()` function → Uses AppHeader with back + title

### Cleanup
- Removed 5 custom header implementations
- Removed unused imports (TopAppBar, Scaffold, ExperimentalMaterial3Api)
- Consolidated header styling to single component

---

## Notes
- All headers share similar padding (16dp H, 12dp V)
- Split layout is consistent across most views
- Some actions have colored backgrounds, others don't (keep flexible)
