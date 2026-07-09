# KIM-323: Platform-specific tests for `DatabaseExportImport` (Android/iOS) and Keychain bridges

**Status:** Done · **Priority:** High · **Labels:** _none_
**Created:** 2026-06-24T13:47:15.657Z · **Completed:** 2026-06-26T08:28:09.360Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-323/platform-specific-tests-for-databaseexportimport-androidios-and

## Description

Drafting the §2.4 ticket now. Let me pull the actual file specifics so the acceptance criteria are concrete rather than generic.

Pulled the actual implementations. Worth flagging before the ticket text: several of these aren't just *untested* — some are unfinished stubs that tests will immediately expose as broken. I've folded that into the draft so it doesn't get lost.

---

**Labels:** `spec-ready`, `testing` **Status to set:** Todo (no blockers — confirmed below) **Linked doc:** `docs/agent/testing/COVERAGE-GAP-ANALYSIS.md` §2.4

### Background

§2.4 of the coverage gap analysis is the last open item from that doc's priority list (items 1–6, including the §2.5 CI-wiring follow-up, are done as of `kimmomyllyviita/coverage-followups`). `DatabaseExportImport` (Android + iOS) and the Keychain bridge objects have no tests beyond incidental construction in one instrumented test (`HomeJourneyTest.kt`).

**Drafting this ticket surfaced real functional gaps, not just missing tests** — worth fixing alongside, since a test suite that just documents broken behavior isn't much of a win:

| Platform | Method | Current behavior |
| -- | -- | -- |
| Android | `exportDatabase()` | Real implementation — copies `app.db` to `Documents/ReWinds/rewinds_backup_<ts>.db` |
| Android | `importDatabase()` | **Stub** — always returns `Result.failure(Exception("Import not implemented on Android - use iOS"))` |
| Android | `listBackups()` | Real implementation — lists matching files in the backup dir |
| iOS | `exportDatabase()` | **Placeholder** — does not copy any file; returns a success message telling the user to copy `app.db` manually |
| iOS | `importDatabase()` | Real implementation — copies source file over `app.db` via `NSFileManager` |
| iOS | `listBackups()` | **Stub** — always returns `Result.success(emptyList())` |

Net effect: export only works on Android, import only works on iOS, and backup listing only works on Android. A user following "export on iOS, import on Android" or vice versa hits silent stubs today.

### Scope

1. **Android** `DatabaseExportImport` (`androidMain/.../DatabaseExportImport.android.kt`)
   * Test `exportDatabase()` copies the real db file to the expected path and returns the path in the message.
   * Test `exportDatabase()` failure path when the source db file doesn't exist.
   * Test `listBackups()` returns matching `rewinds_backup_*.db` files and filters out non-matching files.
   * Fix `importDatabase()` to actually implement the copy (mirror the iOS logic, adapted to `java.io.File`), then test new-file overwrite and "source not found" cases.
   * **Test home**: `androidInstrumentedTest` — needs a real `Context`/file system, and that source set is now CI-gated (`android-instrumented` job, shipped in §2.5), so this no longer needs new test infra to land in CI.
2. **iOS** `DatabaseExportImport` (`iosMain/.../DatabaseExportImport.ios.kt`)
   * Fix `exportDatabase()` to actually copy `app.db` to a real backup path (mirror Android's timestamped-filename pattern), then test it.
   * Fix `listBackups()` to actually list backup files from disk, then test empty-dir and populated-dir cases.
   * Test `importDatabase()`'s existing real logic: success copy, overwrite-existing-target, and "source file not found".
   * **Test home**: `iosTest` — `NSFileManager`/`NSSearchPathForDirectoriesInDomains` already work today in `iosSimulatorArm64Test` (real simulator file system, same pattern `DatabaseIntegrationTest.kt` already uses), so no new test infra is needed here either.
3. **Keychain bridges** (`KeychainBridge.kt`, `WeatherKeychainBridge.kt`, `IosKeychain.kt`, `IosWeatherKeychain.kt`)
   * These hold callback wiring only — no actual Keychain API calls happen in Kotlin (that's on the Swift side, registered at app startup). So this is plain, low-effort Kotlin logic to test, not platform Keychain mocking:
     * `registerKeychainCallbacks`/`registerWeatherKeychainCallbacks` store the given lambdas.
     * `KeychainBridge.saveKey`/`deleteKey` invoke the registered callback with the right argument; no-op (no throw) when nothing is registered.
     * `setApiKeyFromKeychain`/`setWeatherApiKeyFromKeychain` forward to `ApiKeyManager`/`WeatherApiKeyManager`.
   * **Test home**: `iosTest` — pure Kotlin, no simulator file/Keychain access required.

### Acceptance Criteria

- [ ] Android: importDatabase() actually copies a file (no longer a hardcoded failure); exportDatabase(), importDatabase(), listBackups() each have passing tests in androidInstrumentedTest covering success + at least one failure/edge case.
- [ ] iOS: exportDatabase() and listBackups() actually perform real file operations (no longer placeholders); all three methods have passing tests in iosTest covering success + at least one failure/edge case.
- [ ] Keychain bridge callback registration/invocation has passing tests in iosTest for both KeychainBridge and WeatherKeychainBridge.
- [ ] No regressions in existing suites (testDebugUnitTest, connectedDebugAndroidTest, iosSimulatorArm64Test all green in CI).
- [ ] COVERAGE-GAP-ANALYSIS.md §2.4 updated to "done" once merged.

### Definition of Done

* All new/fixed code merged to `develop` via PR, reviewed by Marcy against `ARCHITECTURE-RULES.md`.
* All four CI jobs (`android`, `android-instrumented`, `ios-unit-tests`) green on the PR.
* Ticket closed with a link to the merged PR.

### Blockers

None — required test infra (`androidInstrumentedTest`, `iosTest`) is already wired into per-PR CI as of the §2.5 work; no new dependencies or environment setup needed.

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-26T08:28:17.098Z

Closed as Done. Scope was implemented and merged in PR #54 (commit `0da5e66`):

- `composeApp/src/androidInstrumentedTest/kotlin/core/DatabaseExportImportTest.kt`
- `composeApp/src/iosTest/kotlin/core/DatabaseExportImportTest.kt`
- `composeApp/src/iosTest/kotlin/core/KeychainBridgeTest.kt`

`docs/agent/testing/COVERAGE-GAP-ANALYSIS.md` §2.4 is marked **CLOSED 2026-06-25**, completing all 7 gap-analysis priority items. The ticket had simply not been moved to Done after the work landed.

PR: https://github.com/kmyllyvi/ReWinds/pull/54

