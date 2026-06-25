# Test Coverage Gap Analysis (2026-06-20)

**Status**: Analysis only — no test code written. Findings below are recommendations
for whoever picks up the follow-up work (routed to Randy per `CLAUDE.md`, since
implementing them touches `composeApp/src/`).

> **Update (June 2026):** the section-0 recommendation has since landed —
> `generate_coverage_metrics.py` now parses real JaCoCo XML, and the
> `jacocoTestReport` task excludes presentational Compose code (`*View.kt`, `App.kt`,
> the `components/` and `ui/` packages) from the denominator, since the MV* rules make
> Views untested by design. See **Exclusions** in `COVERAGE-SETUP.md`. `core/Router.kt`
> (section 2.2) is deliberately **not** excluded — its branching logic
> (`isShowingPlacesPush`) is real and now unit-tested, so it stays in the count.
>
> **Further update (June 24, 2026):** §2.1 (`SqlDelightDatabase` merge/transaction
> tests), §2.2 (`Router.kt` routing-decision test), and §2.3 (`NetworkService`
> error-mapping tests) are all **done** — `SqlDelightDatabaseTest.kt` (in
> `androidUnitTest/`, using the JDBC sqlite driver this doc recommended adding),
> `RouterTest.kt`, and `NetworkServiceTest.kt`. §2.5 (CI gating) is also **done**:
> `.github/workflows/ci.yml` now runs `androidInstrumentedTest` and `iosTest` per
> PR/push (`android-instrumented` and `ios-unit-tests` jobs). The
> `TESTING-STRATEGY.md` refresh (priority item 5) is done too — it's now a
> ground-truth snapshot instead of the stale Feb-2026 roadmap.
>
> **Final update (June 25, 2026):** §2.4 (platform-specific `DatabaseExportImport`/
> Keychain tests) is now **done** too — `iosTest/core/DatabaseExportImportTest.kt`,
> `iosTest/core/KeychainBridgeTest.kt`, `androidInstrumentedTest/core/DatabaseExportImportTest.kt`.
> Writing these surfaced three silent no-op stubs masquerading as working backup
> code (iOS `exportDatabase`/`listBackups` and Android `importDatabase` all did
> nothing and returned hardcoded results) — all three now have real
> implementations. Every item in §3's priority list is closed; only the
> "run the new platform tests on real CI/hardware to confirm green" follow-up
> remains, since this sandbox can't reach Google's Maven repos to execute
> Android/iOS Gradle tasks.

## 0. Headline finding: the coverage report is fabricated

`docs/coverage/detailed.html` and `docs/human/coverage/coverage-report.html` claim
**92.2% line / 91.3% branch coverage, grade A+, 361 tests**. This number is **not
real**. `generate_coverage_metrics.py` (repo root) never parses the JaCoCo `.exec`
file it claims to check for — it contains a hardcoded Python dict
(`modules_coverage`) with eight made-up files and made-up line/branch counts,
annotated with comments like `# Estimate coverage based on test distribution` and
`# 94%`. The numbers in the HTML are arithmetic over those hardcoded literals, not
output from the JaCoCo agent.

This script is wired into `composeApp/build.gradle.kts`'s `coverageReport` task
(depends on `testDebugUnitTest`, then shells out to the script) and **that task
runs in CI on every push to `develop`** (`.github/workflows/ci.yml`), uploading the
fabricated report as a build artifact. Anyone trusting this number — including a
future agent run — gets a false signal that coverage is excellent and stable, when
in fact nobody has measured it.

**Recommendation**: have Randy rewrite `generate_coverage_metrics.py` to parse the
real JaCoCo output (either the `.exec` file via the JaCoCo Ant/CLI report tool, or
simpler: depend on the standard `jacocoTestReport` Gradle task and parse its
generated `jacocoTestReport.xml` for the real line/branch totals) before trusting
or publicizing any coverage percentage again. Until that's fixed, treat the
existing report as informationally void.

## 1. Actual current test inventory (ground truth, by file count)

| Suite | Files | `@Test` methods |
|---|---|---|
| `commonTest` (JVM, runs in CI via `testDebugUnitTest`) | 53 | 531 |
| `androidUnitTest` (JVM, DB-backed via JDBC driver, runs in CI via `testDebugUnitTest`) | 3 | 25 |
| `androidInstrumentedTest` (Compose semantic / Maestro-adjacent, now CI-gated per §2.5) | 6 | 12 |
| `iosTest` (Kotlin/Native, now CI-gated per §2.5) | 6 | 21 |

(Updated 2026-06-25 after the §2.4 follow-up landed — see the update notes above.)

So the real test count (~589) is actually higher than the fabricated report's "361"
— the project is not under-tested in volume. The gaps are about *what* isn't
tested, not how many tests exist. `docs/agent/testing/TESTING-STRATEGY.md` is also
stale (Feb 2026, describes 33+10 tests and a 30% coverage estimate); it predates
most of the current suite and should be refreshed or archived so it stops
contradicting reality.

## 2. Concrete coverage gaps, ranked by risk

### 2.1 `SqlDelightDatabase` persistence/merge logic — untested (highest priority)

`composeApp/src/commonMain/kotlin/core/Database.kt:67-`. This is the real
SQLDelight-backed implementation of `Database`, and it contains the riskiest logic
in the data layer:

- `saveWeatherResponse()`: branches on whether the place already exists, dedups
  incoming days against `existingDateTimes`, and separately upserts/deletes
  `WeatherStation` rows — all inside one `dbQuery.transaction { }` block.
- `getSavedPlaceFull()`: three-level join (response → days → hours) plus a station
  join, reassembled via `DataMapping`.
- `cleanupForecastDays()`, `getWeatherDataFor()`, `getPlaceDayCounts()`.

None of this has a direct test. The only two things that look adjacent are:
- `DataMappingKIM149Test.kt` — tests the pure `DataMapping.fromWeatherResponse()` /
  `toWeatherResponse()` mapping functions only. No SQL, no transaction, no merge
  logic.
- `iosTest/.../DatabaseIntegrationTest.kt` — three smoke tests confirming the
  iOS native driver can create a database, run an empty query, and run an empty
  transaction. It does not call `SqlDelightDatabase` at all, let alone exercise
  `saveWeatherResponse`'s merge branch.

There is no JVM in-memory SQLite test path today: the version catalog
(`gradle/libs.versions.toml`) declares `sqldelight-android-driver` and
`sqldelight-native-driver` but not `app.cash.sqldelight:sqlite-driver` (the
JDBC/Xerial driver SQLDelight ships for exactly this purpose — fast in-memory
unit tests on the JVM). Adding that as a `commonTest`/`androidUnitTest`
dependency would let a real `SqlDelightDatabase` be instantiated against a
real, ephemeral SQLite file in `commonTest` and unblock testing the merge logic
directly — no emulator or simulator required.

**Recommended first tests**: `saveWeatherResponse` with (a) a brand-new place, (b)
an existing place with all-new days, (c) an existing place with overlapping days
(must not duplicate), (d) station upsert replacing a previous station set, and a
round-trip `getSavedPlaceFull` after each.

### 2.2 `Router.kt` — the main navigation Composable has untested branching logic, and some of it arguably belongs in a ViewModel

`composeApp/src/commonMain/kotlin/core/Router.kt` (248 lines) has zero references
in any test file. It's a `@Composable`, which is expected to be test-light per the
MV* rule in `ARCHITECTURE-RULES.md` — but `AppTabs()` computes real branching logic
directly in the View:

```kotlin
val showingPlacesPush = activeTab == AppTab.PLACES &&
    (currentPlacesRoute is PlaceSummaryRoute || currentPlacesRoute is MonthlyStatisticsRoute)
```

plus the `TabRoutingNavigator` delegate wiring (which tab a "chat about this place"
action lands on, and how root-back behaves per tab) is constructed inline in the
Composable rather than exposed from a ViewModel. This is exactly the "conditional
rendering logic in the View" pattern the project's own architecture rules call out
as the anti-pattern to avoid — and because it lives in a Composable, it's also the
hardest part of the navigation system to unit test today. `TabRoutingNavigatorTest`
and `NavigatorTest` test the underlying navigator classes in isolation, but nothing
tests `AppTabs()`'s routing *decisions* (which screen renders for a given
`activeTab` + back-stack state).

**Recommendation**: extract `showingPlacesPush` and the two `TabRoutingNavigator`
constructions into a small testable function or a thin ViewModel (e.g.
`AppTabsViewModel.routeFor(activeTab, placesStack): RouteDecision`), then test that
function directly. This both closes the coverage gap and removes a borderline
architecture-rule violation in one move.

### 2.3 `NetworkService.kt` — zero tests on the only network error-mapping path

`composeApp/src/commonMain/kotlin/core/NetworkService.kt` has no test file
anywhere. Both `fetchWeatherData` and `fetchGeoSearchData` independently catch
`ClientRequestException` (reads the response body, wraps as `NetworkException`
with `httpStatus`) and generic `Exception` (wraps with a different message) — this
is the only place HTTP failures get translated into the app's domain exception,
and it's currently unverified. A Ktor `MockEngine`-backed test (already a common
KMP-test pattern; check if `ktor-client-mock` is already a dependency before
adding it) could assert: a 4xx response produces a `NetworkException` with the
correct `httpStatus` and body text, and a connection failure produces a
`NetworkException` with a wrapped message — cheap tests, currently entirely
missing.

### 2.4 ~~iOS-only and Android-only platform code is essentially untested~~ — CLOSED 2026-06-25

Fixed in `0da5e66` (`composeApp/src/iosTest/kotlin/core/DatabaseExportImportTest.kt`,
`composeApp/src/iosTest/kotlin/core/KeychainBridgeTest.kt`,
`composeApp/src/androidInstrumentedTest/kotlin/core/DatabaseExportImportTest.kt`).
Writing these tests surfaced that the risk called out below was real, not
hypothetical: `DatabaseExportImport` had **three silent no-op stubs**
masquerading as working code — iOS `exportDatabase()` copied nothing and
unconditionally returned success, iOS `listBackups()` always returned an empty
list, and Android `importDatabase()` always returned a hardcoded failure. All
three are now real implementations (mirroring the platform that already
worked), with passing-by-inspection tests for the success and failure paths;
the Keychain bridges (`KeychainBridge`/`WeatherKeychainBridge` callback wiring)
are now covered too.

Not yet executed on real hardware/CI — this sandbox can't reach
`dl.google.com`/`maven.google.com` to resolve the Android Gradle Plugin, so
neither `iosSimulatorArm64Test` nor `connectedDebugAndroidTest` has run.
Original finding, for reference: `iosMain/`/`androidMain/`'s ~570 lines of
platform-specific file I/O and keychain/credential logic had only the 7 iOS
smoke tests touching any of it (platform detection + driver creation), and
nothing on the Android side.

### 2.5 CI gates only the JVM unit tests — the other two suites can silently rot

`.github/workflows/ci.yml` runs `:composeApp:testDebugUnitTest` per PR/push. Per
`docs/agent/testing/TESTING-STRATEGY.md` §5.1's own note, the 4 iOS Kotlin/Native
tests and the 5 `androidInstrumentedTest` (Compose semantic) files are **not**
run by any per-PR or per-push workflow — only manually or, for the Maestro E2E
suite, on release to `master`. A regression in iOS platform code or in the
Compose semantic tests would not surface until someone runs them by hand. This is
a known, already-documented gap (KIM-293's open follow-up), not a new finding,
but worth restating here since it compounds with 2.4: the platform code with the
least direct unit-test coverage is also the code whose existing tests aren't CI-gated.

## 3. Suggested priority order

1. ~~Fix `generate_coverage_metrics.py` to report real numbers~~ — **done**.
2. ~~Add a JVM SQLite test driver and write `SqlDelightDatabase` merge/transaction
   tests (§2.1)~~ — **done**.
3. ~~Extract and test `Router.kt`'s routing decision logic (§2.2)~~ — **done**.
4. ~~Add `NetworkService` error-mapping tests (§2.3)~~ — **done**.
5. ~~Refresh or archive `TESTING-STRATEGY.md`~~ — **done**.
6. ~~Wire the iOS/instrumented suites into per-PR CI (§2.5)~~ — **done**.
7. ~~Platform-specific tests for `DatabaseExportImport` and the Keychain
   bridges (§2.4)~~ — **done**.

All items closed. **Remaining**: confirm the §2.4 suite actually runs green —
it was written and statically reviewed in a sandbox that cannot reach Google's
Maven repos to execute `iosSimulatorArm64Test`/`connectedDebugAndroidTest`, so
it needs a real CI run or a machine with Maven access before being trusted.
