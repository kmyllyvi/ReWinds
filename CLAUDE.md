# ReWinds Development Guide

**Project**: Compose Multiplatform app (iOS + Android)
**Current Goal**: Add sqlite3 support for iOS via cocoapods

## Quick Start

### Build Commands
```bash
# Android only
./gradlew buildAndroidOnly

# iOS Simulator (development)
./gradlew :composeApp:linkPodReleaseFrameworkIosSimulatorArm64

# Full build (requires more memory)
./gradlew build --no-daemon
```

### Key Configuration
- **Gradle Heap**: 6GB (set in `gradle.properties`)
- **Kotlin/Native**: Devirtualization disabled (`-Xno-devirtualization` flag)
- **iOS**: Cocoapods manages sqlite3 dependency
- **Database**: SQLDelight with sqlite3 driver

## Project Structure
```
composeApp/
  ├── build.gradle.kts          # Build config + iOS targets + cocoapods setup
  ├── composeApp.podspec        # iOS framework definition for Xcode
  └── src/
      ├── commonMain/           # Shared code
      ├── androidMain/          # Android-specific
      └── iosMain/              # iOS-specific

iosApp/
  ├── Podfile                   # CocoaPods dependencies
  ├── Pods/                     # CocoaPods managed dependencies (sqlite3, etc)
  ├── iosApp.xcworkspace/       # ⚠️ USE THIS (not .xcodeproj)
  └── iosApp.xcodeproj/         # Old project file (ignore with cocoapods)
```

### ⚠️ Important: Using Xcode
**Always open `iosApp.xcworkspace`** - CocoaPods requires this!
- `.xcworkspace` includes CocoaPods managed dependencies
- `.xcodeproj` alone won't have access to sqlite3 and other pods

### ⚠️ Important: Xcode Build Settings with CocoaPods
**DO NOT manually add `-framework` linker flags for Pod-managed frameworks!**
- The Pod's `podspec` (via `vendored_frameworks`) handles all framework linkage
- Manual `-framework ComposeApp` in `OTHER_LDFLAGS` causes duplicate symbol errors
- Xcode build settings should only have `$(inherited)` for Pod-managed frameworks
- See `docs/DEVELOPMENT-120226.md` for detailed explanation

## Current Status

### ✅ Working
- iOS simulator builds with sqlite3 support
- Android builds normally
- Cocoapods integration for native dependencies
- C interop for sqlite3 headers

### ⚠️ Known Issues
- Device ARM64 builds OOM (needs 8GB+ or architectural changes)
- XCFramework builds have KLIB resolver conflicts

## Development Notes
See `docs/DEVELOPMENT.md` for detailed session logs and technical decisions.

## Session Tracking
- **Latest Session**: Feb 12, 2026 - SQLite3 iOS cocoapods integration
- **Commit**: 92038d2 - Configuration saved to project repo
- **Status**: iOS simulator builds working; device builds need memory optimization
