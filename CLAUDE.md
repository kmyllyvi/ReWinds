# ReWinds Development Guide

**Project**: Compose Multiplatform app (iOS + Android)
**Current Goals**: Fix runtime error on iOS

## ⚠️ Session Init Instructions
On each new session, Claude should read:
1. **`docs/README.md`** — Main documentation hub with all doc navigation
2. **`docs/ARCHITECTURE-RULES.md`** — Architectural patterns and conventions

These establish context before working on any tasks.

## Quick Start

### Build Commands

#### Android
```bash
# Android only (recommended for Android work)
./gradlew buildAndroidOnly
```

#### iOS
```bash
# ⚠️ IMPORTANT: Use Xcode for iOS builds, NOT Gradle
# The Gradle iOS tasks (linkPodReleaseFrameworkIosSimulatorArm64, etc.) are unreliable
# They only compile Kotlin → Framework, without full Xcode integration

# Option 1: Use Xcode GUI (EASIEST)
open iosApp/iosApp.xcworkspace
# Then cmd+R to build and run on simulator

# Option 2: Use xcodebuild CLI
xcodebuild -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -derivedDataPath build

# Option 3: If you only need the Kotlin framework compiled (rare):
./gradlew :composeApp:iosSimulatorArm64MainKlibrary
# But you'll still need Xcode to build the actual app
```

#### Both Platforms
```bash
# Android + iOS Gradle build (CI only — iOS is normally built via Xcode)
./gradlew build -PincludeAllTargets=true --no-daemon

# Or use the convenience task:
./gradlew buildWithIos
```

> **Note**: Plain `./gradlew build` (no flag) now builds **Android only** by default.
> Kotlin/Native iOS targets are skipped unless `-PincludeAllTargets=true` is passed.
> This is intentional — Xcode is the iOS build tool; Gradle iOS compilation was just overhead.

#### Code Coverage
```bash
# Coverage is opt-in to avoid instrumentation overhead on every test run
./gradlew coverageReport -PenableCoverage=true
```

### Key Configuration
- **Gradle Heap**: 8GB (set in `gradle.properties`)
- **iOS targets**: `iosArm64` + `iosSimulatorArm64` (iosX64/Intel removed — Apple Silicon only)
- **Kotlin/Native daemon**: enabled (warm between tasks; `disableCompilerDaemon` removed)
- **Kotlin/Native**: Devirtualization disabled (`-Xno-devirtualization` flag)
- **iOS**: Cocoapods manages sqlite3 dependency
- **Database**: SQLDelight with sqlite3 driver

## Development Workflow

This project uses a **two-agent workflow** for development and quality assurance:

### Coder Agent
**Responsibilities**: Implementation, bug fixes, feature development
- Makes code changes and commits
- Verifies code compiles (metadata tests)
- Follows architectural rules (MV* pattern, etc.)

**Invocation**: "Coder Agent, implement [feature/fix]" or "Coder Agent, work on [task description]"

### QA Agent
**Responsibilities**: Testing, code review, validation
- Runs full test suite on committed changes
- Reviews code against CLAUDE.md rules and conventions
- Identifies regressions, edge cases, or violations
- Reports findings and suggests fixes

**Invocation**: "QA Agent, test and review the latest commit" or "QA Agent, test commit [hash]"

**Workflow**: User tells Coder Agent to implement → Coder commits → User tells QA Agent to review/test → QA reports back

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
- iOS simulator builds with Xcode (use workspace, not Gradle) ✅
- Android builds and runs via Gradle ✅
- CocoaPods integration for native dependencies (sqlite3) ✅
- C interop for sqlite3 headers ✅
- Database export/import feature fully working on BOTH iOS and Android ✅
- AI Chat with flexible metrics querying (visibility, humidity, temperature, etc.) ✅
- Three-layer permission flow for AI data fetching ✅
- **NEW**: iOS Keychain-based API key storage with Settings UI ✅
- All 32+ tests passing ✅

### ⚠️ Known Issues
- Gradle iOS build tasks unreliable (use Xcode instead) — mitigated: iOS targets now excluded from default Gradle builds
- Device ARM64 builds OOM (needs 8GB+ or architectural changes)
- XCFramework builds have KLIB resolver conflicts

## Development Notes
See `docs/DEVELOPMENT.md` for detailed session logs and technical decisions.
See `docs/IMPLEMENTATION_NOTES_*.md` for feature-specific implementation details.

## Session Tracking
- **Latest Session**: March 4, 2026 - iOS Keychain integration + Flexible AI metrics + Permission flow
- **Latest Commits**:
  - `7c301e7` - docs: Add comprehensive implementation notes for iOS Keychain feature
  - `8e7ea87` - Feature: Implement iOS Keychain-based API key storage with Settings screen
- **Status**:
  - ✅ Flexible metrics querying working (50+ metric aliases)
  - ✅ Three-layer permission system for AI API calls implemented
  - ✅ iOS Keychain secure storage with Settings UI complete
  - ✅ Users can enter API key once, persists across restarts
  - ✅ Build workflow clarified: Use Xcode for iOS (not Gradle)
  - ✅ All tests passing
  - ✅ Ready for app store distribution
