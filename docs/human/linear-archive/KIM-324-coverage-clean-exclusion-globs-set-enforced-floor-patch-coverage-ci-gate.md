# KIM-324: Coverage: clean exclusion globs, set enforced floor + patch-coverage CI gate

**Status:** Done · **Priority:** High · **Labels:** spec-ready
**Created:** 2026-06-26T08:30:46.930Z · **Completed:** 2026-06-26T09:00:42.786Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-324/coverage-clean-exclusion-globs-set-enforced-floor-patch-coverage-ci

## Description

## Spec

Current measured coverage (50.5% line / 32.5% branch, per `docs/coverage/detailed.html`) is artificially low: ~1,050 of the 2,544 "missed" lines are generated code or presentational Compose UI that the JaCoCo exclude globs in `composeApp/build.gradle.kts` (`jacocoTestReport` task, ~line 367) fail to catch, even though excluding them is already the stated policy (see comment block above the globs, and `docs/agent/ARCHITECTURE-RULES.md` MV* rule: Views are untested by design). This ticket (1) fixes the exclude globs so the percentage reflects only code coverage is meant to measure, (2) re-baselines off the cleaned number, (3) sets an enforced floor + patch-coverage gate so future PRs cannot regress or land large blocks of untested logic, and (4) records the gate as a generic DoD policy in `docs/agent/WORKFLOW.md`. CI (`.github/workflows/ci.yml`) already runs `coverageReport` on every PR — this ticket turns that existing report into an actual gate instead of an uploaded-and-ignored artifact.

## Acceptance criteria

- [ ] `excludes` list in the `jacocoTestReport` task (`composeApp/build.gradle.kts`) adds globs for the generated resource accessors currently leaking into the denominator: `String0_commonMainKt`, `Plurals0_commonMainKt`, `ActualResourceCollectorsKt` (compose-resources generated string/plural tables — same "generated scaffolding" category as the existing `db/**`, `R.class`, `BuildConfig.*` excludes).
- [ ] `excludes` list extended so presentational Compose files that escape the current `*ViewKt.class` pattern are also excluded: files compiled from `StationMapModal.kt`, `ChatSessionSwitcher.kt`, `DayDetailSheet.kt`, `VcKeyOnboardingScreen.kt` (and any other `*Modal/Sheet/Screen/Switcher.kt` source file under `commonMain`) are covered by a glob, not by naming each file individually if a pattern can match the suffix family.
- [ ] After the exclude fix, `./gradlew :composeApp:coverageReport -PenableCoverage=true` is run locally and the resulting line % and branch % are recorded in this issue's comments as the new baseline (replacing the stale 50.5%/32.5% figures).
- [ ] A `jacocoTestCoverageVerification` task is added to `composeApp/build.gradle.kts`, wired with `violationRules` enforcing a minimum LINE and BRANCH coverage percentage. The minimum is set at or slightly below the new cleaned baseline (a floor, not a stretch target) so it fails only on regression, not on today's code.
- [ ] `jacocoTestCoverageVerification` is added to the CI job in `.github/workflows/ci.yml` (after the existing `coverageReport` step) and the job fails (non-zero exit) when the verification task fails.
- [ ] A patch/diff-coverage check is added to CI: `diff-cover` (Python package, no paid external service) runs against the JaCoCo XML report (`composeApp/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`) comparing the PR branch to `develop`, and fails the job if newly added/changed lines fall under a configured patch-coverage threshold (recommend 70% patch line coverage as the starting threshold — high enough to block drive-by untested logic, low enough not to block small UI-adjacent diffs that legitimately have little testable logic).
- [ ] The patch-coverage step only runs on `pull_request` events (it needs a diff against a base branch; skip or no-op on direct `push` to `develop`/`main`).
- [ ] `docs/agent/WORKFLOW.md` DoD section (the 5-bullet generic list, currently ending "No new lint violations") gets a new standing bullet: PRs must pass the CI patch-coverage gate (threshold and tool named) — this is project-wide policy, not a per-ticket AC, per Kimmo's existing DoD/AC split convention.
- [ ] This issue's comments name the exact follow-up step that is **not** done by this ticket: enabling the new CI job(s) as a **required status check** under GitHub branch protection on `develop`, which is a repo admin setting only Kimmo (or an admin-rights Randy) can apply — not something achievable in code.
- [ ] No new tests required for this ticket — it is build-config, CI workflow, and documentation only; no application logic changes.

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit for all non-trivial logic (ViewModel, Repository, pure functions, bug fixes). No "tests later". Exempt only: pure UI styling, config/doc-only changes — see AC for this ticket's exemption status.
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations

## Notes

**Sizing**: proposed as one session — all three concerns (exclude-glob fix, CI gate wiring, WORKFLOW.md DoD update) are config/YAML/doc edits, no new application code, fits comfortably under 3 hours. Splitting further would just be ticket overhead.

**Sequencing**: the AC is ordered deliberately — fix excludes and re-baseline (AC 1–3) BEFORE setting the floor (AC 4), because the floor must be set off the honest number, not the polluted 50.5%.

**Enforcement mechanism chosen and why**: `diff-cover` (self-hosted, free, reuses the JaCoCo XML CI already produces) over Codecov, because it needs no paid external service or new account, and CI already generates the exact XML `diff-cover` consumes — this is the smaller, more contained change. If `diff-cover` proves too noisy or hard to tune in practice, Codecov's `patch` target is the documented fallback; that decision is left to Randy/Kimmo if `diff-cover` doesn't work out, and should come back as a comment on this issue rather than a silent deviation.

**Threshold is a PO recommendation, not Gate-1-fixed**: I suggest a global floor at-or-just-below the cleaned baseline (ratchet, not stretch) and a 70% patch-coverage threshold for new/changed lines. Kimmo should confirm or adjust both numbers at Gate 1 — they're easy to tune later (just config values) so getting them exactly right now is lower-stakes than shipping the mechanism.

**Genuine low-coverage targets this ticket does NOT fix** (tracked separately): `WeatherRepositoryImpl` (0%), `ChatRepositoryImpl` (0%), `AnthropicClient` (7%) and Anthropic serializers — see the companion ticket "Raise coverage on WeatherRepositoryImpl, ChatRepositoryImpl, AnthropicClient" for that work. This ticket only fixes the *measurement* and the *gate*; it does not write new tests for existing gaps.

**Assumption flagged for Kimmo**: I assumed "fails the PR" means the GitHub Actions job exits non-zero (so the check shows red), and that turning that into an actual merge-block requires the separate branch-protection step called out above — these are two different things and only the first is achievable by Randy in code.

Reviewers needed: code-reviewer (build/CI config correctness, no MV* impact). qa-test-agent not needed (no application logic). ux-ui-reviewer not needed (no UI).

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-26T09:00:34.507Z

## Marcy review — APPROVED ✅

Independent verification complete. All 11 acceptance criteria and all DoD items met. Config/CI/doc-only — no application source touched (`git diff --stat` confirms only `build.gradle.kts`, `ci.yml`, `WORKFLOW.md`, and coverage report/history artifacts changed), so the "no new tests" exemption is legitimate and correctly stated. No MV* surface.

**AC checklist:**
1. ✅ Generated-resources glob added (`**/rewinds/composeapp/generated/resources/**`) — covers String0/Plurals0/ActualResourceCollectors.
2. ✅ Presentational suffix globs added (`*ModalKt/*SheetKt/*ScreenKt/*SwitcherKt`). Verified these match exactly the four named files (StationMapModal, DayDetailSheet, VcKeyOnboardingScreen, ChatSessionSwitcher) and no ViewModel/logic file — glob is conservative and exact.
3. ✅ Re-baseline recorded (63.6% line / 38.4% branch). I reproduced it: ran `jacocoTestReport` locally and parsed the XML — 2598/4087 = 63.6% line, 695/1810 = 38.4% branch. Exact match to the handover and `history.json`.
4. ✅ `jacocoTestCoverageVerification` added with LINE min 0.60 / BRANCH min 0.35 — a ratchet just below baseline. Shares `coverageExcludes`/`coverageClassDirs` with the report task so measured and enforced denominators can't drift (good factoring).
5. ✅ Wired into CI after `coverageReport`; exits non-zero on regression.
6. ✅ diff-cover step added, consumes the exact XML path the report task emits (`reports/jacoco/jacocoTestReport/jacocoTestReport.xml` — verified the paths match), `--fail-under 70`.
7. ✅ Patch step gated `github.event_name == 'pull_request'`.
8. ✅ WORKFLOW.md DoD gained the standing patch-coverage bullet.
9. ✅ Branch-protection follow-up correctly flagged as out-of-scope (repo admin only).
10. ✅ No tests required — exemption stated.

**DoD:** Build green; `testDebugUnitTest` + `jacocoTestCoverageVerification` pass locally at the floor. I additionally confirmed the rule is **non-vacuous**: forcing the line floor to 0.99 fails the build ("lines covered ratio is 0.63, but expected minimum is 0.99"), so the gate genuinely catches regression rather than passing silently.

**Non-blocking notes (no action required for this ticket):**
- `fetch-depth: 0` on checkout already makes the extra `git fetch --depth=1 origin <base_ref>` in the diff-cover step redundant — harmless, leave it.
- On a config/doc-only PR diff-cover no-ops ("no lines with coverage information"). Worth knowing the gate only bites when a diff actually touches measured `.kt` lines — expected behavior, not a defect.
- Honest scope limit (same-model-family caveat): I verified mechanism correctness and spec conformance. Whether 70% patch / 60-35% floor are the *right* long-term numbers is a tuning judgment — the ticket itself defers that to Kimmo, which is correct.

Verdict: **Ready to merge.** Removing in-review, setting Completed.

### kimmo.myllyviita@gmail.com — 2026-06-26T08:56:13.856Z

## Randy handover — ready for review

**Branch:** `kim--324-coverage-gate`
**PR:** https://github.com/kmyllyvi/ReWinds/pull/57 (Closes KIM-324)
**Build:** PASS — `./gradlew buildAndroidOnly` green; full CI green (all 3 jobs).
**Tests:** PASS — `./gradlew :composeApp:testDebugUnitTest` green. No new tests (config/CI/doc-only, per AC exemption).

### New cleaned coverage baseline (AC 3)
After fixing the exclude globs, `./gradlew :composeApp:coverageReport -PenableCoverage=true`:

| Metric | Old (polluted) | New (cleaned) |
|---|---|---|
| Line | 50.5% | **63.6%** (2598/4087) |
| Branch | 32.5% | **38.4%** (695/1810) |

### Thresholds set
- **Global floor** (`jacocoTestCoverageVerification`): **60% line / 35% branch** — a ratchet just below the cleaned baseline, fails on regression only. Shares the exact same `coverageExcludes`/`coverageClassDirs` as `jacocoTestReport` (factored into shared vals) so the measured and enforced denominators can't drift.
- **Patch coverage** (`diff-cover`): **≥70%** on new/changed lines, `pull_request` events only.

### What was done
1. Excludes cleaned: added `**/rewinds/composeapp/generated/resources/**` (covers `String0`/`Plurals0`/`ActualResourceCollectors`) and `*ModalKt`/`*SheetKt`/`*ScreenKt`/`*SwitcherKt` globs. Verified the four named files (StationMapModal, DayDetailSheet, VcKeyOnboardingScreen, ChatSessionSwitcher) are all pure `@Composable` with no ViewModel/logic — and that they are the *only* `*Modal/Sheet/Screen/Switcher.kt` files under commonMain, so the glob is conservative and exact.
2. `jacocoTestCoverageVerification` task added and wired into CI after `coverageReport` (job exits non-zero on regression). Verified passing locally at the floor.
3. `diff-cover` step added to CI (PR-only); checkout now `fetch-depth: 0`. Verified locally in an isolated venv: runs against the JaCoCo XML and exits 0 with "No lines with coverage information in this diff" on a config/doc-only diff (graceful no-op). CI run confirmed both new steps green.
4. `docs/agent/WORKFLOW.md` DoD gained the standing patch-coverage gate bullet.

### Follow-up NOT done by this ticket (AC 9 — repo admin only)
Enabling the CI coverage job as a **required status check** under GitHub **branch protection on `develop`** is a repo setting, not code. Only Kimmo (or an admin-rights user) can apply it. Until then the gate exits non-zero (check shows red) but does not hard-block merge. This is the only AC item not achievable in code, flagged in the PR description too.

### Deviations
None. `diff-cover` used as specified (no Codecov fallback needed). Thresholds are the PO-recommended values — Kimmo can tune them freely (pure config).

Handing off to Marcy (code-reviewer) for review.

