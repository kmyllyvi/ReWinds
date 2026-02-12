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
```

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
