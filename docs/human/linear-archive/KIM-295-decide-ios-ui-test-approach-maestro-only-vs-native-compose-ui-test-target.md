# KIM-295: Decide iOS UI test approach: Maestro-only vs native Compose UI test target

**Status:** Done · **Priority:** High · **Labels:** spec-ready
**Created:** 2026-06-15T07:17:30.791Z · **Completed:** 2026-06-15T17:11:21.467Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-295/decide-ios-ui-test-approach-maestro-only-vs-native-compose-ui-test
**Parent:** KIM-289 — [EPIC] UI Testing Strategy & Automation
**Related:** related: KIM-292 — Navigation testing strategy: fakeable Navigator/Router for tests; related: KIM-296 — Periodic AI/agentic exploratory testing process (runbook + cadence); related: KIM-294 — Maestro setup + cross-platform E2E smoke suite (Android + iOS)

## Description

## Spec

The iOS UI testing approach is already de facto decided and partially documented: `.maestro/README.md` (landed in [KIM-294](https://linear.app/kimmo-m/issue/KIM-294/maestro-setup-cross-platform-e2e-smoke-suite-android-ios)) states "This is also the primary iOS UI test path ([KIM-295](https://linear.app/kimmo-m/issue/KIM-295/decide-ios-ui-test-approach-maestro-only-vs-native-compose-ui-test)) — no native Compose UI test target is stood up for iOS," and gives the rationale (Kotlin/Native compile times, immature tooling, OOM risk, CLAUDE.md's Xcode-not-Gradle steer for iOS).

What's missing is making this an explicit, easy-to-find decision record rather than an incidental note inside the Maestro README, and closing the loop in the older `docs/agent/testing/TESTING-STRATEGY.md`, which still describes a Gradle-based `iosSimulatorArm64Test` UI test plan (Section 5.1 / "Recommended iOS Test Structure") that has been superseded. This ticket is documentation-only — no code changes.

## Acceptance criteria

- [ ] `docs/agent/testing/TESTING-STRATEGY.md` Section 5.1 ("iOS Testing") and the "Recommended iOS Test Structure" block are updated to state the current decision: Maestro is the primary iOS UI test path ([KIM-294](https://linear.app/kimmo-m/issue/KIM-294/maestro-setup-cross-platform-e2e-smoke-suite-android-ios)/.maestro/), no native `iosSimulatorArm64` Compose UI test target is pursued for UI testing, with a one-line rationale and a link/reference to `.maestro/README.md`
- [ ] The stale references to `./gradlew :composeApp:iosSimulatorArm64Test` as a UI-testing recommendation are removed or clearly marked as superseded (the 4 existing iOS platform/unit tests under `iosTest/` are unaffected — this ticket only concerns UI-test strategy, not the existing platform unit tests)
- [ ] A short "Revisit conditions" note is added: native iOS Compose UI tests would only be reconsidered if Maestro proves insufficient, with a one-line description of what a future spike would need to measure (compile time, OOM behavior, CI feasibility) — no spike is performed as part of this ticket
- [ ] `.maestro/README.md` is cross-referenced from `docs/agent/testing/TESTING-STRATEGY.md` (or vice versa) so the decision is discoverable from either doc
- [ ] No new tests required — documentation-only change, no source code modified

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] New tests written in the same commit for all non-trivial logic (ViewModel, Repository, pure functions, bug fixes). No "tests later". Exempt only: pure UI styling, config/doc-only changes — see AC for this ticket's exemption status.
- [ ] No MV* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations

## Notes

* This ticket is doc-only, formalizing a decision already implemented in practice via [KIM-294](https://linear.app/kimmo-m/issue/KIM-294/maestro-setup-cross-platform-e2e-smoke-suite-android-ios)'s `.maestro/README.md`. Effort is small (~30-60 min): edit two markdown files, no code/build changes expected.
* Reviewers needed: code-reviewer (to confirm no source files were touched and the doc accurately reflects current `.maestro/` and `iosTest/` state).
* Assumption (PO call, low-stakes): "decision recorded" per the AC means updating the existing strategy doc rather than creating a new standalone decision doc — keeps the docs tree from fragmenting further. If Kimmo prefers a dedicated ADR-style file, that's a cheap follow-up.
* Out of scope, not raised here per instruction: the [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android) follow-up question about CI-wiring of instrumented tests on every PR remains parked for Kimmo separately.

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-15T17:11:22.370Z

PR #33 merged into develop. Marcy's review passed (doc-only, all factual claims cross-checked against `e2e-smoke.yml`/`.maestro/README.md`, all AC/DoD met). Marking as Done.

### kimmo.myllyviita@gmail.com — 2026-06-15T17:10:43.241Z

**Marcy (code-reviewer) — PR #33 review: PASS ✅**

Reviewed the diff (`5ed94b2`) on `kim-295-ios-ui-test-strategy-doc` against develop. Doc-only as specced — `git diff --name-only` shows exactly two `.md` files, zero source/build files.

**AC verification (all met):**
- ✅ §5.1 + "Recommended iOS Test Structure" rewritten as a decision record: Maestro is the primary/only iOS UI test path, no native `iosSimulatorArm64` Compose UI target pursued, one-line rationale present, links to `.maestro/README.md`.
- ✅ Stale `./gradlew :composeApp:iosSimulatorArm64Test` UI-test recommendation struck through and marked superseded. Nuance is accurate: the Gradle task still exists and is correctly noted as fine for the platform/unit tests — not deleted, just not the UI mechanism.
- ✅ "Revisit conditions" note added; future spike would measure compile time, OOM behavior, CI feasibility; no spike performed here.
- ✅ Cross-referenced both directions (README → §5.1 anchor; §5.1 → `.maestro/README.md`; plus a top-of-doc callout). All relative link paths resolve; GitHub anchor slug `#51-ios-testing` matches the `### 5.1 iOS Testing` heading.
- ✅ No new tests required (doc-only exemption stated in AC).

**Cross-checked claims against actual repo state:**
- 4 existing `iosTest/` tests, non-UI — confirmed (PlatformFunctions, DatabaseDriverFactory, CoroutineDispatchers, DatabaseIntegration); unaffected by the decision, as the doc states.
- CI iOS Maestro job opt-in behind `vars.ENABLE_IOS_E2E` — confirmed at `.github/workflows/e2e-smoke.yml:87` (`if: ${{ vars.ENABLE_IOS_E2E == 'true' }}`).
- `.maestro/README.md` existing "primary iOS UI test path" line is consistent with the new decision record.

**DoD:** Android build + `testDebugUnitTest` green per Randy; trivially unaffected since no source touched. No MV* surface (no Kotlin). No lint surface.

No Critical/Major/Minor issues. Verdict: **Ready to merge.** Leaving `in-review` per KIM-292/293 precedent — merge next, then issue → Done.

### kimmo.myllyviita@gmail.com — 2026-06-15T17:08:19.968Z

Handover — ready for review

**Branch:** `kim-295-ios-ui-test-strategy-doc`
**PR:** https://github.com/kmyllyvi/ReWinds/pull/33 (Closes KIM-295)
**Build:** PASS — `./gradlew buildAndroidOnly` succeeds (with `--no-configuration-cache`; see deviation).
**Tests:** PASS — `./gradlew :composeApp:testDebugUnitTest -PincludeAllTargets=false` green, no tests broken. No new tests (doc-only, per AC exemption).

**Summary**
Doc-only change formalizing the already-implemented Maestro-only iOS UI test decision. Two files touched: `docs/agent/testing/TESTING-STRATEGY.md` and `.maestro/README.md`. No source code modified.

AC coverage:
- §5.1 "iOS Testing" + "Recommended iOS Test Structure" rewritten as an explicit decision record: Maestro is the primary iOS UI test path (KIM-294 / `.maestro/`); no native `iosSimulatorArm64` Compose UI test target is pursued for UI testing. One-line rationale included.
- Stale `./gradlew :composeApp:iosSimulatorArm64Test` *UI-testing* recommendation + `iosApp/UITests/*.swift` plan struck through as superseded; clarified the 4 existing `iosTest/` platform/unit tests are unaffected and still use that task. Also de-staled the Phase 2/3 "iOS UI Tests in Xcode" plan rows.
- "Revisit conditions" note added: native iOS Compose UI tests reconsidered only if Maestro proves insufficient; a future spike would measure compile time, OOM behavior, CI feasibility. No spike performed here.
- Bidirectional cross-reference: top-of-doc note + §5.1 link to `.maestro/README.md`, and `.maestro/README.md` now links back to §5.1.

**Deviations**
- `buildAndroidOnly` fails under Gradle's configuration cache due to a **pre-existing** issue in the task itself (`Task.project` accessed at execution time) — unrelated to this doc-only change. It builds clean with `--no-configuration-cache`, and `:composeApp:build` completes regardless. Flagging in case it's worth a separate fix ticket.
- Per the spec's PO note, recorded the decision in the existing strategy doc rather than a standalone ADR file. Happy to split into an ADR if preferred (cheap follow-up).

### kimmo.myllyviita@gmail.com — 2026-06-15T17:03:03.529Z

Spec'd and moved to Todo for Randy.

This is a doc-only ticket: the iOS UI test decision (Maestro-only, no native Compose UI test target) is already implemented in practice via KIM-294's `.maestro/README.md`. The remaining work is to formalize that decision in `docs/agent/testing/TESTING-STRATEGY.md` (Section 5.1 / "Recommended iOS Test Structure" is stale — still describes a Gradle `iosSimulatorArm64Test` UI plan), add a short "revisit conditions" note, and cross-link the two docs.

AC, DoD, and Notes are in the issue body. No source code changes expected — code-reviewer should mainly confirm only docs were touched and the content matches current `.maestro/` + `iosTest/` state.

KIM-296 (agentic exploratory testing runbook) stays in Backlog at Low priority — it's lower urgency per its own notes (depends on the deterministic layers landing first) and will get a full spec pass when picked up next.

