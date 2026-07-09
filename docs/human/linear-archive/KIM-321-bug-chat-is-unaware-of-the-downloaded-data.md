# KIM-321: Bug: Chat is unaware of the downloaded data

**Status:** Done · **Priority:** High · **Labels:** spec-ready, Bug, Improvement
**Created:** 2026-06-20T15:50:55.825Z · **Completed:** 2026-06-21T08:17:02.380Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-321/bug-chat-is-unaware-of-the-downloaded-data

## Description

## Spec

Claude has no awareness of which months are already in the local database before it calls weather tools. Three of the five tools (`get_wind_summary`, `get_monthly_stats`, `get_best_days`) call `getDaysRange` directly without a prior availability check, so they either silently trigger a Visual Crossing API call or return empty results and hallucinate "no data cached." The one tool that does check (`get_weather_metrics`) conflates "Partial" with "Missing" in its permission message, giving the user no indication of what is already downloaded vs. what needs fetching. The system prompt never tells Claude which months are available, so Claude cannot self-correct.

The fix has three layers: (1) add a `getDownloadedMonths(place)` query to `WeatherRepository` that returns the set of `YYYY-MM` strings with at least one day in the DB, exposed reactively so the cached month-set in the ViewModel stays current as new data is downloaded; (2) inject that month-set into the system prompt, re-assembled from the live cached value at send time, so Claude knows upfront what data exists; (3) require explicit user confirmation on all five tools before any paid Visual Crossing API fetch, with the permission gate naming missing months (in full month-name format) separately from already-downloaded months.

## Acceptance criteria

- [ ] `WeatherRepository` exposes a new `suspend fun getDownloadedMonths(place: String): Set<String>` that returns `YYYY-MM` strings for months with at least one `Day` row in the DB for that place. Implementation may use `SUBSTR(datetime, 1, 7)` GROUP BY or equivalent; no schema migration required. Internal grouping/storage stays `YYYY-MM` — full month names are a presentation-layer concern only (see AC below).
- [ ] `WeatherRepository` also exposes the downloaded-months data as a reactive Flow (e.g. `fun observeDownloadedMonths(place: String): Flow<Set<String>>` backed by the SQLDelight query's `.asFlow().mapToList(...)`), so any write to the `Day` table — from a chat-triggered fetch, a manual download, or any other path — causes the Flow to re-emit automatically. No webhook, socket, or manual "new data" signal is used; SQLDelight's query invalidation is the mechanism.
- [ ] `WeatherRepository` has unit tests for `getDownloadedMonths`: returns empty set when place has no days, returns correct months when days exist across multiple months, ignores days from other places. The reactive Flow variant has a test confirming it emits a new value after a `Day` row is inserted for the observed place.
- [ ] Per MV* rules, the ViewModel (`ChatViewModel` or equivalent) collects the downloaded-months Flow per saved place into its own state (e.g. `StateFlow<Map<String, Set<String>>>`) at session-init time — not the View. This state is the single cached source for the session; it updates automatically on new downloads without any explicit re-fetch call from the UI.
- [ ] The system prompt sent to Anthropic is assembled at send time (not as a static constant) by reading the current value of the cached downloaded-months state and formatting it into the prompt — e.g. `Helsinki: October 2025, November 2025, December 2025`. Because the state is reactive, a message sent immediately after a new download reflects the new month without requiring a new session or manual refresh. `ANTHROPIC_SYSTEM_PROMPT` becomes a template function, not a static string constant.
- [ ] Month names in this summary and in all user-facing confirmation/permission copy use full month names (e.g. "October 2025"), produced via a KMP-safe helper (e.g. `formatMonthName(yyyyMM: String): String` added alongside `FormatUtils.kt`, using a manual month-name table or `kotlinx-datetime`'s `Month` enum — no `String.format`, no `java.time` formatting APIs in `commonMain`).
- [ ] `get_wind_summary`, `get_monthly_stats`, and `get_best_days` each call `checkDataAvailability` before calling `getDaysRange`. When status is `Partial` or `Missing` they return a `permission_required` JSON response (same shape as `get_weather_metrics`, updated per the next AC) instead of proceeding to a network call. This applies with no exceptions — all five tools require explicit user confirmation before any paid Visual Crossing API fetch; there is no auto-fetch path left in any tool.
- [ ] The `permission_required` response for `Partial` availability includes a `"downloaded_months"` field listing the months already in the DB and a `"missing_months"` field listing the months that would require a new API fetch, both formatted as full month names (e.g. `"October 2025"`). The response for `Missing` states that no data is downloaded for the requested range.
- [ ] When `ChatViewModel` receives a `permission_required` tool result, the confirmation message shown to the user explicitly names the missing months (full month-name format) and clarifies that confirming will download new data (a paid Visual Crossing API call). Already-downloaded months are listed separately and must not be described as needing a fetch.
- [ ] No new tests required for the system-prompt template formatting itself if it is pure string construction with no branching logic; otherwise a unit test covering zero places, one place with months, and two places is required. The `formatMonthName` helper requires a unit test covering all 12 months plus an invalid/malformed input case.

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit for all non-trivial logic (ViewModel, Repository, pure functions, bug fixes). No "tests later". Exempt only: pure UI styling, config/doc-only changes — see AC for this ticket's exemption status.
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations

## Notes

Priority: High (downgraded from Urgent at original spec time — the bug is real and user-facing but does not block core app functionality; Urgent is reserved for crashes/data-loss).

Size estimate: **3 points** (roughly 2–3 hours). The DB query and reactive Flow wrapper are straightforward; the month-name helper is small and isolated; the logic change across the three tools and the confirmation copy are moderate. No schema migration.

Reviewers needed: **code-reviewer** (Marcy) + **qa-test-agent** (Seppo) — tool dispatch logic and the permission gate are logic-heavy paths that benefit from manual chat-flow testing.

**Gate-1 decisions from Kimmo (resolved 2026-06-20):**

1. **Prompt timing/caching** — resolved as reactive SQLDelight Flow (`.asFlow().mapToList(...)`) feeding ViewModel state, with the system prompt re-assembled from that live cached value at send time. This replaces the earlier open question about re-reading on every message vs. a one-time cache; no webhook/socket/manual-refresh mechanism is needed because SQLDelight's own query invalidation handles freshness.
2. **Month format** — resolved as full month names (e.g. "October 2025") in all user-facing copy, via a new KMP-safe `formatMonthName` helper. Internal DB grouping remains `YYYY-MM`.
3. **Confirmation UX** — resolved as: all five tools require explicit confirmation before any paid fetch, no exceptions. This confirms the behaviour-change flag raised in the original spec (users who previously got silent auto-fetch on three of the five tools now see a confirmation step every time) — Kimmo has signed off on this as intended.

All three open questions from the original spec are now closed; no outstanding assumptions remain for this ticket.

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-21T08:17:17.704Z

Marcy (code-reviewer) — **Approved, ready to merge.**

All 10 acceptance criteria and all DoD items verified against the diff on `kimmomyllyviita/kim-321-bug-chat-is-unaware-of-the-downloaded-data` (PR #49). Independently re-ran `./gradlew :composeApp:testDebugUnitTest` against the branch: BUILD SUCCESSFUL, 0 failures, and all 5 new test classes executed (DownloadedMonthsDb 4, WeatherToolsPermissionGate 8, AnthropicSystemPrompt 4, ChatViewModelDownloadedMonths 2, FormatUtils +7). MV* clean — all logic lives in the VM/repository/tools layer, no View file touched. KMP-safe (no `String.format`/`java.time` in commonMain). Permission gate correctly precedes every `getDaysRange`; `list_saved_places` legitimately exempt (no date range). Removing **in-review**, moving to Completed.

Caveat: same-model-family review — I reliably catch spec-drift, MV* violations, and missing coverage, but not deep design flaws. The two minor notes below are non-blocking; recommend Kimmo eyeball the chat flow once before/after merge (Seppo's manual chat-flow pass is also still listed as a reviewer on this ticket).

Minor (non-blocking, optional follow-ups):
- `buildPermissionRequired`: the message branch keys on `status == Missing || downloadedNames.isEmpty()`, so a `Missing` status that happens to have an in-range downloaded month would still print the "no data" copy while populating a non-empty `downloaded_months` array. Benign because `Missing` implies no overlap by definition, but the message/array could theoretically disagree.
- `observeDownloadedMonths()` in the VM snapshots the saved-place list once at init; a place added mid-session won't get a collector until the next session. Matches the AC ("at session-init time") so not a defect — just noting the lifecycle assumption.

### kimmo.myllyviita@gmail.com — 2026-06-21T08:13:45.494Z

Randy (developer) handoff

- Branch: kimmomyllyviita/kim-321-bug-chat-is-unaware-of-the-downloaded-data
- PR: https://github.com/kmyllyvi/ReWinds/pull/49
- Build: PASS (`./gradlew buildAndroidOnly`)
- Tests: PASS (`./gradlew :composeApp:testDebugUnitTest`) — all green incl. new suites

Summary
- `WeatherRepository.getDownloadedMonths(place)` + reactive `observeDownloadedMonths(place): Flow<Set<String>>` backed by `SUBSTR(datetime,1,7)` GROUP BY exposed via `.asFlow().mapToList(...)`. No schema change.
- `ChatViewModel` collects each saved place's Flow into `StateFlow<Map<String,Set<String>>>` at session init (MV*: logic in the VM).
- `ANTHROPIC_SYSTEM_PROMPT` is now `AppConstants.buildAnthropicSystemPrompt(...)`, assembled at send time from the live month-set; re-pushed to `AiRepository` before every send (and after a confirmed fetch).
- KMP-safe `formatMonthName("YYYY-MM") -> "October 2025"` added to FormatUtils (no `String.format` / `java.time`); internal grouping stays `YYYY-MM`.
- All data-fetching tools (`get_wind_summary`, `get_monthly_stats`, `get_best_days`, `get_weather_metrics`) check availability first and return a shared `permission_required` response that names `downloaded_months` and `missing_months` separately in full month-name format. `Missing` states no data is downloaded for the range. No auto-fetch path remains. (`list_saved_places` takes no date range, so no gate applies.)

Tests added
- formatMonthName: 12 months + invalid inputs.
- System-prompt template: 0 / 1 / 2 places (+ place-with-no-months).
- Permission gate across all four data tools + `monthsInRange`; asserts no fetch on Partial/Missing.
- ChatViewModel: per-place Flow collection into state + prompt assembled-from-state before send.
- DB-backed `getDownloadedMonths` / `observeDownloadedMonths` (re-emits after a Day insert) against an in-memory JDBC SQLDelight driver.

Deviations / notes
- Added a JVM-only `androidUnitTest` source set + `sqldelight-sqlite-driver` (test-only) to satisfy the reactive-Flow-after-insert AC, which needs a real driver that cannot live in `commonTest`.
- New interface methods carry empty / `flowOf(emptySet())` defaults so unrelated test doubles did not need changes.
- Per project policy iOS was not built via Gradle; commonMain code is KMP-safe (no JVM-only APIs introduced).

Handing off to Marcy (code-reviewer) next.

