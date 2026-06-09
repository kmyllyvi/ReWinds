# Gradle iOS Build Exclusion by Default

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. Decision reconstructed from codebase and CLAUDE.md only. -->

**Date:** 2026-06-09
**PR:** unknown (bootstrap)
**Ticket:** unknown (bootstrap)
**Status:** Active

---

## Context

Kotlin/Native iOS Gradle tasks (`linkPodReleaseFrameworkIosSimulatorArm64`, etc.) were unreliable in the project's Gradle build. They only compile Kotlin to a framework binary without running through Xcode's build system (no proper CocoaPods integration, no iOS SDK paths). Running them in default builds added compile time and occasionally crashed with `podspec`/`generateDummyFramework` errors.

## Decision

iOS targets are excluded from the default `./gradlew build` task. They are only included when `-PincludeAllTargets=true` is passed explicitly (for CI). A `buildAndroidOnly` convenience task was added. A `buildWithIos` task exists for combined builds but is intended for CI only.

The canonical iOS build tool is **Xcode** (`open iosApp/iosApp.xcworkspace` → Cmd+R).

## Rationale

- Xcode is the correct iOS toolchain: it applies CocoaPods integration, correct SDK paths, and full Xcode build settings.
- Removing iOS from the default Gradle build eliminates a class of build failures and reduces developer build times.
- Apple Silicon only (`iosArm64` + `iosSimulatorArm64`); `iosX64` (Intel) was removed.

## Consequences

- Developers must use Xcode for all iOS work. Running `./gradlew build` on iOS CI requires the flag.
- The Kotlin/Native compiler daemon is kept warm between tasks (daemon not disabled) to improve build performance when iOS targets are compiled.
- Devirtualization is disabled (`-Xno-devirtualization`) to work around a Kotlin/Native runtime issue.
- Do not add `-framework ComposeApp` manually in Xcode build settings. The `composeApp.podspec` `vendored_frameworks` entry handles all linkage.
