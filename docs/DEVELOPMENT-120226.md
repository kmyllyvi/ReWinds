# Development Log

## Session: Feb 12, 2026 - SQLite3 iOS Cocoapods Integration

### Objective
Add sqlite3 support for iOS builds using cocoapods dependency management

### Issues Encountered & Solutions

#### 1. Gradle Cache Corruption
**Error**: `metadata.bin (No such file or directory)` in ~/.gradle/caches/8.13/groovy-dsl/
**Root Cause**: Partial/corrupted gradle cache after previous failed builds
**Solution**:
```bash
rm -rf ~/.gradle/caches ~/.gradle/wrapper
./gradlew clean --no-daemon
```
**Result**: ✅ Cache fully reset, builds work

#### 2. Kotlin Hierarchy Template Conflicts
**Error**: Default Kotlin Hierarchy Template conflicts with explicit `.dependsOn()` for iOS source sets
**Location**: build.gradle.kts lines 102-121 (iosMain, iosArm64Main, etc.)
**Solution**: Added to gradle.properties:
```properties
kotlin.mpp.applyDefaultHierarchyTemplate=false
```
**Result**: ✅ iOS source set hierarchy properly configured

#### 3. Compose Dependencies Deprecation (⚠️ Not Fixed)
**Warning**: `compose.runtime`, `compose.material3`, etc. deprecated
**Investigation**: Attempted to create explicit lib declarations in libs.versions.toml
**Finding**: These libraries aren't available as Maven artifacts - only through the Compose plugin
**Status**: ⚠️ Warnings remain but non-blocking (future migration task)

#### 4. embedAndSign/CocoaPods Conflict
**Error**: `embedAndSign` task incompatible with cocoapods dependencies
**Root Cause**: Old framework embedding approach conflicts with cocoapods
**Solution**: Added to gradle.properties:
```properties
kotlin.apple.deprecated.allowUsingEmbedAndSignWithCocoaPodsDependencies=true
```
**Result**: ✅ Conflict suppressed (temporary workaround, deprecated property)

#### 5. jansi Native Library Architecture Mismatch
**Error**: `libjansi.jnilib` x86_64 incompatible with arm64 system
**Root Cause**: Gradle cached old x86_64 jansi library
**Solution**:
```bash
rm -rf ~/.gradle/native/jansi
```
**Result**: ✅ Correct arm64 version downloaded on next build

#### 6. Cocoapods SQLite3 Setup
**Configuration**:
- build.gradle.kts: `pod("sqlite3")` declared in cocoapods block
- Generated sqlite3.def: Correctly links to system `-framework sqlite3`
- Podfile: Auto-generated with `pod 'sqlite3'`
- C Interop: Cinterop task `cinteropSqlite3IosArm64` compiles successfully

**Files Generated**:
- `/composeApp/build/cocoapods/defs/sqlite3.def` - Links to system framework
- `/composeApp/build/cocoapods/synthetic/ios/Pods/Target Support Files/sqlite3/` - Modulemap and headers
- `/composeApp/build/cocoapods/framework/ReWinds.framework` - Final framework

**Result**: ✅ SQLite3 properly integrated

#### 7. iOS Compilation Memory Issues
**Error**: `java.lang.OutOfMemoryError: Java heap space` during DevirtualizationAnalysis
**Phase**: Kotlin/Native LTO (Link-Time Optimization) phase
**Initial Attempts**:
- Increased heap from 2GB → 4GB: Still OOM
- Tried `-Xno-llvm-lto` flag: Didn't help (doesn't disable devirtualization)
- Increased heap to 6GB: Partial help

**Solution**: Disable devirtualization phase
```kotlin
// In build.gradle.kts iOS targets block
freeCompilerArgs += "-Xno-devirtualization"
```
Also in gradle.properties:
```properties
org.gradle.jvmargs=-Xmx6144M -Dfile.encoding=UTF-8 -Dkotlin.daemon.jvm.options\="-Xmx6144M" -Djansi.tmpdir=/tmp
kotlin.native.disable.lto=true
```

**Result**:
- ✅ Simulator builds (iosSimulatorArm64) now work
- ⚠️ Device builds (iosArm64) still OOM - needs further investigation

### Build Status

#### ✅ Working
- `./gradlew build --no-daemon` - Full project builds
- `./gradlew :composeApp:linkPodReleaseFrameworkIosSimulatorArm64` - Simulator framework (42MB binary)
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64` - iOS simulator Kotlin compilation
- C interop for sqlite3 compiles successfully

#### ⚠️ Partial/Issues
- `./gradlew :composeApp:linkPodReleaseFrameworkIosArm64` - Device framework OOM even at 6GB heap
- `./gradlew :composeApp:podPublishXCFramework` - KLIB resolver conflicts (duplicate platform libraries)

### Configuration Changes

**gradle.properties**:
```properties
# Increased from 2GB to 6GB
org.gradle.jvmargs=-Xmx6144M -Dfile.encoding=UTF-8 -Dkotlin.daemon.jvm.options\="-Xmx6144M" -Djansi.tmpdir=/tmp

# Disable default hierarchy template (use explicit dependsOn instead)
kotlin.mpp.applyDefaultHierarchyTemplate=false

# Kotlin/Native optimizations
kotlin.native.disableCompilerDaemon=true
kotlin.native.disable.lto=true

# CocoaPods compatibility
kotlin.apple.deprecated.allowUsingEmbedAndSignWithCocoaPodsDependencies=true

# Xcode version compatibility
kotlin.apple.xcodeCompatibility.nowarn=true
```

**build.gradle.kts**:
- Added `-Xno-devirtualization` to iOS target compilation flags
- Confirmed cocoapods block has `pod("sqlite3")` and `binaryOption("bundleId", "com.km.rewinds.ReWinds")`

**composeApp.podspec**:
- Already correctly configured:
  - `spec.vendored_frameworks = 'build/cocoapods/framework/ReWinds.framework'`
  - `spec.dependency 'sqlite3'`
  - Script phase calls gradle syncFramework during pod install

### Manual Pod Setup (After Initial Session)
**User added pods manually** - sqlite3 library now linked and included:
- Created `iosApp/Podfile` with pod dependencies
- Ran `pod install` - generated `iosApp.xcworkspace`
- **Critical**: Always use `.xcworkspace` in Xcode (not `.xcodeproj`)
- sqlite3 and other pods now accessible in Xcode project

### Recommendations

**For Development/Testing**:
- Use simulator framework (`linkPodReleaseFrameworkIosSimulatorArm64`)
- Xcode development with iOS Simulator works correctly with sqlite3
- **IMPORTANT**: Open `iosApp.xcworkspace` in Xcode, NOT `iosApp.xcodeproj`

**For Production Builds**:
- Device builds need architectural solution:
  - Option A: Further reduce compiler optimizations
  - Option B: Split framework into multiple targets
  - Option C: Use different build strategy for release builds
  - Option D: Upgrade system RAM or use CI/CD pipeline with more resources

**Future Tasks**:
- [ ] Fix Compose deprecation warnings (update to new syntax)
- [ ] Resolve device ARM64 build memory issues
- [ ] Test sqlite3 integration in actual iOS app
- [ ] Set up CI/CD pipeline for device builds
- [ ] Document iOS app integration steps

### References
- Kotlin Multiplatform: https://kotl.in/kmp
- Cocoapods integration: https://kotl.in/cocoapods
- Kotlin/Native: https://kotl.in/kotlin-native
