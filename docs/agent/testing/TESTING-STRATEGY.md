# ReWinds Testing Strategy

**Last refreshed**: June 24, 2026 (previously dated Feb 19, 2026 — see **History** at
the bottom for what changed and why this doc was rewritten rather than incrementally
patched).

## Current state (ground truth)

| Suite | Files | `@Test` methods | Runs in per-PR CI? |
|---|---|---|---|
| `commonTest` (JVM, shared) | 52 | 531 | ✅ via `testDebugUnitTest` |
| `androidUnitTest` (JVM, Android-only) | 3 | 25 | ✅ via `testDebugUnitTest` |
| `androidInstrumentedTest` (Compose semantic, emulator) | 5 | 8 | ✅ via `android-instrumented` job |
| `iosTest` (Kotlin/Native platform/unit) | 4 | 7 | ✅ via `ios-unit-tests` job |

All four suites are now gated in `.github/workflows/ci.yml` on every push/PR to
`develop`/`main`. The `android-instrumented` and `ios-unit-tests` jobs were added to
close the gap this document used to describe as open (see **History**) — the
long-standing KIM-293 follow-up ("wire Layer 1 into per-PR CI") is done. The separate
Maestro E2E suite (`.maestro/`, `e2e-smoke.yml`) remains intentionally
manual/release-gated — see [§2 Layered UI testing strategy](#layered-ui-testing-strategy)
below, that's a different layer with a different cadence by design, not an open gap.

For real, current coverage **percentages** (not test counts), run `/coverage` or
`./coverage.command` and read `docs/coverage/detailed.html` — don't estimate them here.
For known coverage **gaps** (which files/logic lack tests), see
[`COVERAGE-GAP-ANALYSIS.md`](./COVERAGE-GAP-ANALYSIS.md) — that document is the living
source of truth for "what's untested and why it matters," refreshed as gaps are closed.
This document is about testing **strategy and layering**, not a gap inventory or a
numeric roadmap.

---

## Layered UI testing strategy

The three-layer model for UI coverage — deterministic gates on the inside, exploratory
discovery on the outside:

| Layer | Suite                                    | Scope                                      | When it runs                     | Gates? | Reference / location                                  |
| ----- | ---------------------------------------- | ------------------------------------------ | --------------------------------- | ------ | ----------------------------------------------------- |
| **1** | **Compose semantic UI tests** (KIM-293)  | Single-screen behaviour, Android           | **Per PR/push** (`android-instrumented` job) | ✅ Yes | `composeApp/src/androidInstrumentedTest/kotlin/` (instrumented, `AndroidJUnitRunner`) |
| **2** | **Maestro E2E smoke suite** (KIM-294)    | End-to-end critical journeys, Android + iOS| **Manual / release to master**   | Yes (release) | `.maestro/flows/`, [`.maestro/README.md`](../../../.maestro/README.md) |
| **3** | **Agentic exploratory testing** (KIM-296)| Unscripted "click around like a human"     | **Periodic, manual**             | **No** | [`AGENTIC-EXPLORATORY-TESTING.md`](./AGENTIC-EXPLORATORY-TESTING.md) |

**How the layers relate.** Layers 1 and 2 lock down journeys we already know about
(the 5–10 critical journeys defined in **KIM-291**). Layer 3 — a human-triggered
vision/computer-use agent exercising the running app without a script — is for finding
the broken states we _haven't_ thought of yet. Its findings feed back **inward**: a
confirmed bug becomes a Linear ticket, and a recurring or critical-path bug is promoted
into a Maestro flow (Layer 2) or a Compose semantic test (Layer 1) so it can never
silently regress. Layer 3 is explicitly **non-CI-gating** — it never blocks a PR or a
release. See [`AGENTIC-EXPLORATORY-TESTING.md`](./AGENTIC-EXPLORATORY-TESTING.md) for
the full runbook, seed exploration goals, cadence, feedback loop, and cost budget — the
direct answer to **KIM-288**.

**Layer 1 is now per-PR-gated** (`android-instrumented` job in `ci.yml`, added June
2026) — previously it was manual-only; that gap is closed.

---

## iOS testing

### iOS UI test approach — DECISION (KIM-295)

**Decision: Maestro is the primary (and only) iOS UI test path. No native
`iosSimulatorArm64` Compose UI test target is pursued for UI testing.**

The cross-platform Maestro E2E smoke suite (KIM-294, `.maestro/`) drives the **built**
app on a simulator through the P0 critical journeys — the same flow files run on
Android and iOS via shared `id:` selectors. iOS UI coverage comes from there, not from
a Gradle/Kotlin-Native UI test target.

**Rationale (one line):** Kotlin/Native UI-test infra is not viable here — slow compile
times, OOM risk on the project's Gradle heap, immature tooling, and CLAUDE.md mandates
Xcode (not Gradle) for iOS builds; Maestro already exercises the real app on a simulator
and costs us nothing extra to extend to iOS.

See **[`.maestro/README.md`](../../../.maestro/README.md)** for the full Maestro setup,
the iOS run instructions, and the per-journey selector gaps. In CI the iOS Maestro job
is opt-in behind `vars.ENABLE_IOS_E2E` (see `.github/workflows/e2e-smoke.yml`) until
simulator provisioning + signing are wired up.

**Revisit conditions:** native iOS Compose UI tests would only be reconsidered if
Maestro proves insufficient (e.g. it can't reach an iOS-only UI state, or its
flake/maintenance cost outweighs its coverage). Reopening the question requires a
time-boxed spike measuring Kotlin/Native UI-test compile time, OOM behavior under the
current heap, and CI feasibility (macOS runner minutes/budget). No such spike has been
performed.

### Existing iOS platform/unit tests (unaffected by the UI-test decision above)

The 4 Kotlin/Native tests under `iosTest/` are **non-UI** platform/unit tests:

```
iosTest/kotlin/
├── core/
│   ├── PlatformFunctionsTest.kt
│   └── DatabaseDriverFactoryTest.kt
└── integration/
    ├── DatabaseIntegrationTest.kt
    └── CoroutineDispatchersTest.kt
```

These now run per-PR/push via the `ios-unit-tests` job (`ci.yml`, `macos-14` runner,
`:composeApp:iosSimulatorArm64Test -PincludeAllTargets=true`). **Cost note**: macOS
runners bill ~10x Linux minutes on GitHub Actions; this is a private repo on a limited
monthly minutes budget. If this job's cost proves too high in practice, narrow its
trigger (e.g. push-to-`develop`-only) rather than removing the gate — see the comment
above the job in `ci.yml`.

## Android testing

**Key tests unique to Android**: permissions handling (database file access), Activity
lifecycle integration, the Android-specific SQLDelight driver, Compose UI tests
(`androidInstrumentedTest/`), file system operations (`DatabaseExportImport.android.kt`).

`androidUnitTest/` holds JVM-only Android tests that need the real SQLDelight JDBC
driver but no emulator (e.g. `SqlDelightDatabaseTest.kt`, `NetworkServiceTest.kt`,
`DownloadedMonthsDbTest.kt`) — these run via `testDebugUnitTest` alongside `commonTest`,
same as any other unit test.

## Test file organization

```
composeApp/src/commonTest/kotlin/        — shared JVM+Native unit tests (52 files)
composeApp/src/androidUnitTest/kotlin/   — Android-only JVM unit tests (3 files; real SQLite driver, no emulator)
composeApp/src/androidInstrumentedTest/kotlin/  — Compose semantic tests, emulator-run (5 files)
composeApp/src/iosTest/kotlin/           — Kotlin/Native platform/unit tests (4 files)
```

There is no fixed phase/roadmap structure for *which* file goes where beyond this — new
tests go in `commonTest` by default (per `Randy-developer.md`'s testing rules), dropping
to `androidUnitTest`/`iosTest` only when the logic under test is genuinely
platform-specific.

## Running tests locally

```bash
# JVM unit tests (fast, what CI's `android` job runs)
./gradlew :composeApp:testDebugUnitTest -PincludeAllTargets=false

# Compose semantic / instrumented tests (needs a running emulator)
./gradlew :composeApp:connectedDebugAndroidTest -PincludeAllTargets=false

# iOS Kotlin/Native platform/unit tests (needs -PincludeAllTargets=true to declare iOS targets)
./gradlew :composeApp:iosSimulatorArm64Test -PincludeAllTargets=true

# Coverage report (opt-in instrumentation; see /coverage or coverage.command)
./gradlew :composeApp:coverageReport -PenableCoverage=true
```

## References

- **Test frameworks**: `kotlin.test`, `kotlinx.coroutines.test`, `androidx.compose.ui.test` (instrumented), JUnit4 (`AndroidJUnit4` runner)
- **Database**: SQLDelight, sqlite3 JDBC driver (JVM/`androidUnitTest`), native driver (iOS)
- **Networking**: Ktor client (+ `ktor-client-mock` for `NetworkServiceTest.kt`)
- **DI**: Koin
- **Architecture**: MV* pattern — see [`ARCHITECTURE-RULES.md`](../ARCHITECTURE-RULES.md). Views are deliberately untested by design; all logic lives in ViewModels.

## History

This document was a forward-looking Phase 1/2/3 roadmap written Feb 19, 2026, targeting
"32 Android unit tests + 4 iOS platform tests" growing to 67 → 115 → 165+ over three
phases, with numeric coverage targets (50% → 65% → 75%) and a long list of specific
planned test files (`WeatherRepositoryTest.kt`, `MonthlyStatisticsViewModelTest.kt`,
`SqlDelightDatabaseTest.kt`, `NetworkServiceTest.kt`, etc., with effort estimates in
hours).

By June 2026 that roadmap was obsolete in a good way: the actual suite had grown to 64
files / 571 `@Test` methods across all four source sets — past every Phase 3 target —
just not always under the exact planned file names (e.g. `SqlDelightDatabaseTest.kt` and
`NetworkServiceTest.kt` did get built, in `androidUnitTest/` rather than `commonTest/`,
once a JVM SQLite test driver was added). Keeping the old phase tables around as "the
plan" was actively misleading: a reader would conclude 30% coverage and a pending
3-month roadmap when the real numbers were already far ahead and the recommended tests
already existed.

This refresh replaces the roadmap with a ground-truth snapshot plus pointers to the
documents that now own that material:
- **Current coverage %** → `docs/coverage/detailed.html` (run `/coverage`), not this doc.
- **Known gaps / what's still untested** → [`COVERAGE-GAP-ANALYSIS.md`](./COVERAGE-GAP-ANALYSIS.md).
- **Coverage tooling setup & exclusions** → [`COVERAGE-SETUP.md`](./COVERAGE-SETUP.md).

The Layered UI testing strategy and iOS decision-record sections (KIM-293/294/295/296)
were kept as-is in substance — those are still-active architectural decisions, not stale
numbers — with one update: Layer 1 (Compose semantic tests) is now per-PR CI-gated
(`android-instrumented` job), closing the gap this document and
`COVERAGE-GAP-ANALYSIS.md` §2.5 both flagged as open.
