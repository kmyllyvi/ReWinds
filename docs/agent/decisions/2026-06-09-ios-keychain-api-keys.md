# iOS Keychain for API Key Storage

<!-- ⚠ bootstrap: Generated without ticket history on 2026-06-09. Decision reconstructed from codebase only. -->

**Date:** 2026-06-09
**PR:** unknown (bootstrap)
**Ticket:** unknown (bootstrap)
**Status:** Active

---

## Context

The app requires two API keys at runtime: Anthropic (AI chat) and Visual Crossing (weather data). On Android these are injected at build time via `gradle.properties` → `BuildConfig`. iOS has no equivalent mechanism that is usable without rebuilding the app, making it impractical for end users to configure their own keys.

## Decision

On iOS, API keys are stored in the iOS Keychain using `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`. A build-config override path is also supported via `Info.plist` / `Config.local.xcconfig` (gitignored) for developer convenience. The priority order at startup is: build config → Keychain → empty (triggers Settings prompt).

The Kotlin/Swift bridge uses a callback pattern: Kotlin `KeychainBridge` / `WeatherKeychainBridge` objects hold Swift-registered closures (`saveKeyCallback`, `deleteKeyCallback`). The Settings screen calls the Kotlin `actual` `saveApiKeyPlatform()` which invokes the registered closure, keeping the iOS Keychain write in Swift and out of the Kotlin/Native layer.

## Rationale

- Native Security framework APIs are simpler to call from Swift than from Kotlin/Native C interop.
- The callback bridge avoids tight coupling between Kotlin and iOS-specific Swift types.
- `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` is appropriate for API keys (no iCloud backup needed, no background-access requirement).

## Consequences

- Users who migrate to a new iPhone must re-enter their API keys.
- The Visual Crossing key has no build-config path on iOS; it must always be entered via Settings.
- Android does not benefit from this pattern; keys are build-time only and not runtime-configurable.
