# KIM-325: Raise coverage on WeatherRepositoryImpl, ChatRepositoryImpl, AnthropicClient

**Status:** Done · **Priority:** Medium · **Labels:** spec-ready
**Created:** 2026-06-26T08:31:08.533Z · **Completed:** 2026-06-26T10:02:18.725Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-325/raise-coverage-on-weatherrepositoryimpl-chatrepositoryimpl

## Description

## Spec

Three components carry real, untested business logic and are the biggest genuine (non-exclusion-policy) gaps in the coverage report: `WeatherRepositoryImpl` (224 lines, 0%), `ChatRepositoryImpl` (68 lines, 0%), and `AnthropicClient` (54 lines, 7%), plus the Anthropic request/response serializers. This ticket adds unit tests for these, raising the project's measured coverage toward (and ideally past) the floor set in [KIM-324](https://linear.app/kimmo-m/issue/KIM-324/coverage-clean-exclusion-globs-set-enforced-floor-patch-coverage-ci). It is the "do the work" companion to [KIM-324](https://linear.app/kimmo-m/issue/KIM-324/coverage-clean-exclusion-globs-set-enforced-floor-patch-coverage-ci)'s "build the gate" — [KIM-324](https://linear.app/kimmo-m/issue/KIM-324/coverage-clean-exclusion-globs-set-enforced-floor-patch-coverage-ci) must land first so the new floor/patch thresholds exist to measure progress against.

## Acceptance criteria

- [ ] `WeatherRepositoryImpl` has unit tests covering its public methods' success paths, at minimum one error/failure path per method that has error handling (e.g. network failure, empty/malformed API response), and any caching or data-merging logic it performs.
- [ ] `ChatRepositoryImpl` has unit tests covering its public methods' success paths and at least one failure path per method that has error handling.
- [ ] `AnthropicClient` has unit tests covering request construction and response parsing for at least one successful call and one error/non-200 response.
- [ ] Anthropic serializers (request/response models) have unit tests verifying round-trip (de)serialization for at least one representative payload per model, including any optional/nullable fields.
- [ ] After this ticket's tests are added, `./gradlew :composeApp:coverageReport -PenableCoverage=true` shows `WeatherRepositoryImpl`, `ChatRepositoryImpl`, and `AnthropicClient` each above 70% line coverage, recorded in this issue's comments with before/after numbers.
- [ ] `./gradlew :composeApp:jacocoTestCoverageVerification` (added in [KIM-324](https://linear.app/kimmo-m/issue/KIM-324/coverage-clean-exclusion-globs-set-enforced-floor-patch-coverage-ci)) passes after these changes, i.e. this ticket does not merely add tests but actually clears the CI gate with margin.

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit for all non-trivial logic (ViewModel, Repository, pure functions, bug fixes). No "tests later". Exempt only: pure UI styling, config/doc-only changes — see AC for this ticket's exemption status.
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations

## Notes

**Dependency**: blocked by [KIM-324](https://linear.app/kimmo-m/issue/KIM-324/coverage-clean-exclusion-globs-set-enforced-floor-patch-coverage-ci) — the floor/patch-coverage gate must exist first so this ticket's "clears the gate with margin" AC is checkable, and so the before/after numbers in [KIM-324](https://linear.app/kimmo-m/issue/KIM-324/coverage-clean-exclusion-globs-set-enforced-floor-patch-coverage-ci)'s re-baseline aren't immediately stale.

**Sizing**: this is test-writing across three distinct components (repository x2, network client + serializers) — estimate 3–4 hours, on the larger end of a single session but kept as one ticket because the three components share the same theme (AI chat + weather data fetch path) and a reviewer benefits from seeing them together rather than three near-identical small PRs. If a developer finds any one component needs significant test-scaffolding (e.g. new fakes/mocks not yet in the codebase) split that component out at that point rather than stalling the whole ticket.

**Not in scope**: no production code behaviour changes — this is test-only, unless writing tests surfaces an actual bug, in which case stop and flag via `needs-human` rather than silently fixing it (test-only tickets shouldn't quietly become behaviour-change tickets).

Reviewers needed: code-reviewer (test quality, no MV* impact); qa-test-agent (logic-heavy — repository and network-client test coverage benefits from a second pass on edge cases).

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-26T10:02:37.254Z

## Marcy review — APPROVED, moving to Done

Reviewed the actual diff on `kim--325-raise-coverage` (PR #58), ran the four new test classes locally (`testDebugUnitTest` green). All AC + DoD items verified individually; no Critical/Major/Minor findings.

**AC**
- ✅ `WeatherRepositoryImpl` — success paths for all public methods; error paths per method with handling (gap-fetch swallow, geo-search network error → empty, fetchAndPersistStations network error leaves DB untouched, getPreviousDays no-key → throws); caching proven via a counting fake (second read = 0 extra DB loads); data-merge asserts full ordered range after gap fetch.
- ✅ `ChatRepositoryImpl` — all public methods + eviction-at-cap + auto-title + explicit-rename-not-clobbered; failure paths (unknown session → empty/null). Uses a **real in-memory SQLite** engine, not a mocked DB — per project convention.
- ✅ `AnthropicClient` — success + non-200 (429/401, status surfaced) + connection-failure for both `sendMessage` and `sendRawMessage`; asserts endpoint + auth/version headers.
- ✅ Serializers — round-trips for Tool / Request (incl. defaults-when-absent) / Response+Usage / null Usage / mixed content blocks / ToolResult `is_error` true + default-false. Optional/nullable fields covered.
- ✅ Coverage above 70% line on all three (87.6% / 100% / 81.1%), before/after recorded in handover comment; `jacocoTestCoverageVerification` passes with margin.

**DoD** — Android build + tests green; MV* not applicable (test-only); no new lint.

**Production change** — the `AnthropicClient internal constructor(apiKey, HttpClient)` test seam is acceptable under the ticket's "no behaviour change" rule: it mirrors the existing `NetworkService` seam exactly, and Koin DI still binds the unchanged `(apiKey, enableLogs)` constructor (verified in DI.kt). No runtime path altered.

**Fakes vs production** — `FakeDatabase`/`FakeNetworking` implement the full `Database`/`Networking` interfaces; the by-datetime merge in the fake matches the repo's re-query-after-fetch contract. The Chat eviction test correctly validates the runtime FK-cascade caveat (pragma off → repo deletes messages explicitly).

**Note for merge:** test-only, low-risk, no design changes — no need for a second human pass before merge.

### kimmo.myllyviita@gmail.com — 2026-06-26T10:00:15.686Z

## Randy handover — ready for review

**Branch:** `kim--325-raise-coverage`
**PR:** https://github.com/kmyllyvi/ReWinds/pull/58
**Build:** PASS — `./gradlew buildAndroidOnly` green (incl. lint)
**Tests:** PASS — `./gradlew :composeApp:testDebugUnitTest` green; CI green (PR #58, including the patch-coverage diff-cover gate and the floor enforcement step).

### Coverage (before → after, line)
| Class | Before | After |
|---|---|---|
| `WeatherRepositoryImpl` | 0% (0/224) | **87.6%** (220/251) |
| `ChatRepositoryImpl` | 0% (0/68) | **100%** (68/68) |
| `AnthropicClient` | 7.4% (4/54) | **81.1%** (43/53) |

All three exceed the 70% AC target.

**New overall baseline:** 63.6% → **72.4%** line, 38.4% → **45.0%** branch.
`jacocoTestCoverageVerification` passes with margin (floor 60% line / 35% branch).

### Summary
Test-only ticket. Added:
- `composeApp/src/commonTest/kotlin/core/WeatherRepositoryImplTest.kt` — fake `Networking` + fake `Database`; success/merge/gap-fetch/cache/error paths.
- `composeApp/src/androidUnitTest/kotlin/ai/ChatRepositoryImplTest.kt` — real in-memory SQLDelight `AppDatabase`; all public methods + eviction/auto-title.
- `composeApp/src/androidUnitTest/kotlin/ai/AnthropicClientTest.kt` — Ktor `MockEngine`; request construction, typed + raw response parsing, non-200 + connection-failure error mapping.
- `composeApp/src/commonTest/kotlin/ai/AnthropicSerializersTest.kt` — round-trip (de)serialization of request/response/tool/usage + content serializers, incl. nullable/optional fields.

### Deviations / notes
- **One production change (test seam only):** `AnthropicClient` now has an `internal constructor(apiKey, HttpClient)` so a `MockEngine` client can be injected. The production secondary constructor `(apiKey, enableLogs)` is unchanged and is what Koin DI binds. This mirrors the existing `NetworkService` seam — no runtime behaviour change.
- `ChatRepositoryImpl` and `AnthropicClient` tests live in `androidUnitTest` (not commonTest) because ktor-client-mock and the JDBC SQLite driver are JVM-only in this project. This is the existing convention (`NetworkServiceTest`, `SqlDelightDatabaseTest`). No component required splitting into a separate PR.
- No bug surfaced that would have required a production behaviour change, so nothing was flagged `needs-human`.

Handing off to Marcy (code-reviewer) next.

