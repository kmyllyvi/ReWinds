# iOS Build Guide: Gradle vs Xcode

**Date**: March 4, 2026

## TL;DR

- ✅ **Use Xcode for iOS development** (simple and reliable)
- ❌ **Don't use Gradle iOS tasks** (`linkPodReleaseFrameworkIosSimulatorArm64`, etc.)
- 📱 **iOS builds work great in Xcode**, even though Gradle tasks fail

## The Problem

The Gradle task `./gradlew :composeApp:linkPodReleaseFrameworkIosSimulatorArm64` is unreliable because:

1. **It only compiles Kotlin code** to an iOS framework binary
2. **It doesn't build the actual Xcode project** that runs the app
3. **CocoaPods integration from Gradle is flaky** compared to Xcode's native handling
4. **Build environment differences** between Gradle and Xcode cause failures

## Understanding the Architecture

### What Gradle Does

```
Kotlin Source Code
        ↓
Kotlin/Native Compiler
        ↓
iOS Framework Binary (ComposeApp.framework)
        ↓
[Gradle task ends here]
```

**Problem**: Just having the framework isn't enough. You still need:
- Xcode to link it into the app
- CocoaPods to inject dependencies
- Build settings configured
- Entitlements and provisioning profiles
- Actual app target compilation

### What Xcode Does

```
Kotlin Source Code (via composeApp.podspec)
        ↓
[Gradle compiles framework, Xcode uses it]
        ↓
CocoaPods Integration
    ├─ Loads Pods project
    ├─ Injects sqlite3 and other dependencies
    └─ Configures build settings
        ↓
Xcode Builds ReWinds Target
    ├─ Links framework with dependencies
    ├─ Applies Xcode build settings
    ├─ Adds provisioning profiles
    └─ Creates .app bundle
        ↓
Simulator Binary Ready to Run
```

## Correct iOS Workflow

### Option 1: Use Xcode GUI (Recommended for Development)

```bash
# Open the workspace (NOT xcodeproj!)
open iosApp/iosApp.xcworkspace

# Then in Xcode:
# 1. Select "ReWinds" scheme (top-left)
# 2. Select simulator target (iPhone 15, etc.)
# 3. Press Cmd+R to build and run
# 4. App launches on simulator
```

**Advantages**:
- Fastest iteration during development
- Full IDE integration (debugging, breakpoints, console)
- All build tools work correctly
- Easy to inspect logs and errors
- Can modify code in Xcode if needed

### Option 2: Use xcodebuild CLI

```bash
# Build for simulator
xcodebuild -workspace iosApp/iosApp.xcworkspace \
  -scheme ReWinds \
  -configuration Debug \
  -sdk iphonesimulator \
  -derivedDataPath build

# Install and run on simulator
xcrun simctl install booted build/Debug-iphonesimulator/ReWinds.app
xcrun simctl launch booted com.km.rewinds
```

**Advantages**:
- Scriptable for CI/CD
- Can be automated
- Same as what Xcode uses internally

### Option 3: Gradle for Framework Only (Rarely Needed)

```bash
# Only if you need the compiled framework for some reason
./gradlew :composeApp:iosSimulatorArm64MainKlibrary

# But you STILL need to build in Xcode to get the actual app
open iosApp/iosApp.xcworkspace
# Then build in Xcode as shown in Option 1
```

**When to use**:
- Debugging Kotlin/Native compilation issues
- Checking if Kotlin code compiles without Xcode
- Rare edge cases

## Why Gradle iOS Fails

### Problem 1: Incomplete Build

Gradle task `linkPodReleaseFrameworkIosSimulatorArm64` fails at multiple stages:
- Building Kotlin/Native framework (sometimes works, sometimes doesn't)
- CocoaPods integration issues
- Missing SDK paths
- Xcode version mismatches

### Problem 2: Environment Differences

**Xcode environment**:
- Has access to iOS SDK paths
- Proper CocoaPods configuration
- Correct provisioning profiles
- Full build settings

**Gradle environment**:
- Trying to guess SDK paths
- Limited CocoaPods integration
- No access to Xcode build cache
- Different compiler flags

### Problem 3: CocoaPods Complexity

The project uses CocoaPods to manage sqlite3 dependency:

```
iosApp/Podfile:
  pod 'composeApp', :path => '../composeApp'  # Kotlin framework
  # (implicitly needs CocoaPods setup)

composeApp/build.gradle.kts:
  cocoapods {
    pod("sqlite3") { ... }  # Declare sqlite3 dependency
  }
```

When you use Xcode:
- CocoaPods is properly integrated
- sqlite3 is available during build
- Everything just works

When you use Gradle:
- CocoaPods integration is attempted
- But often incomplete or misconfigured
- sqlite3 linking fails
- Build breaks

## Building for iOS Properly

### Development (Simulator)

```bash
# 1. Make code changes in IDE/editor

# 2. Open Xcode (if not already open)
open iosApp/iosApp.xcworkspace

# 3. Build and run (Cmd+R in Xcode)
```

### Testing (Unit Tests)

```bash
# Run unit tests via Gradle
./gradlew :composeApp:testDebugUnitTest

# Run UI tests via Xcode
# In Xcode: Product → Test (Cmd+U)
```

### Release Build

```bash
# 1. Build framework via Gradle
./gradlew :composeApp:iosArm64MainKlibrary

# 2. Open Xcode and set scheme to Release
open iosApp/iosApp.xcworkspace

# 3. In Xcode: Product → Archive
# 4. Then: Distribute App (to TestFlight or App Store)
```

## Troubleshooting

### Issue: "linkPodReleaseFrameworkIosSimulatorArm64 failed"

**Solution**: Use Xcode instead
```bash
open iosApp/iosApp.xcworkspace
# Build in Xcode (Cmd+B)
```

### Issue: "Undefined symbol: sqlite3_..."

**Cause**: CocoaPods wasn't properly integrated
**Solution**:
```bash
# Make sure you're using the workspace
open iosApp/iosApp.xcworkspace  # ✅ Correct
# NOT: open iosApp/iosApp.xcodeproj  # ❌ Wrong
```

### Issue: "Swift compiler error" when building in Xcode

**Solution**: Clean and rebuild
```bash
# In Xcode: Shift+Cmd+K (Clean Build Folder)
# Then: Cmd+B (Build)
```

If that doesn't work:
```bash
# Podfile update
cd iosApp
pod repo update
pod install
cd ..

# Then rebuild in Xcode
```

### Issue: "No matching simulator found"

**Solution**: List and select available simulator
```bash
# List simulators
xcrun simctl list devices

# Build for specific simulator
xcodebuild -workspace iosApp/iosApp.xcworkspace \
  -scheme ReWinds \
  -configuration Debug \
  -sdk iphonesimulator \
  -arch arm64
```

## Key Files

| File | Purpose |
|------|---------|
| `iosApp/iosApp.xcworkspace` | ✅ **Use this** - Includes Pods |
| `iosApp/iosApp.xcodeproj` | ❌ Don't use - Missing Pods |
| `iosApp/Podfile` | CocoaPods configuration |
| `iosApp/Pods/` | Generated by CocoaPods |
| `composeApp/composeApp.podspec` | Framework definition for Pods |
| `composeApp/build.gradle.kts` | Kotlin/Native configuration |

## Best Practices

1. **Always use `.xcworkspace`** not `.xcodeproj`
2. **Use Xcode for iOS work** - It's the right tool
3. **Use Gradle for Android** - It's the right tool
4. **Test Kotlin changes** - Use `./gradlew testDebugUnitTest`
5. **Test UI changes** - Use Xcode Test navigator (Cmd+U)
6. **Don't try to use Gradle for iOS builds** - Just use Xcode

## Environment Setup

First-time setup for iOS development:

```bash
# 1. Ensure Cocoapods is installed
sudo gem install cocoapods

# 2. Install pod dependencies
cd iosApp
pod install
cd ..

# 3. Open workspace
open iosApp/iosApp.xcworkspace

# 4. Select simulator
# Xcode top-left: ReWinds > iPhone 15 (or your simulator)

# 5. Build
# Cmd+B to build, Cmd+R to run
```

## Summary

```
iOS Development Workflow:
┌─────────────────────┐
│  Edit Code          │ (in IDE or Xcode)
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Open Xcode Workspace│ (iosApp/iosApp.xcworkspace)
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Build & Run        │ (Cmd+B or Cmd+R)
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ App on Simulator    │ ✅
└─────────────────────┘

DON'T use Gradle iOS tasks!
They only compile Kotlin without building the app.
```
