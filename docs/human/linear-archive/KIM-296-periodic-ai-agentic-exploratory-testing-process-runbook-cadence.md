# KIM-296: Periodic AI/agentic exploratory testing process (runbook + cadence)

**Status:** Done · **Priority:** Medium · **Labels:** spec-ready
**Created:** 2026-06-15T07:17:42.755Z · **Completed:** 2026-06-15T18:05:19.257Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-296/periodic-aiagentic-exploratory-testing-process-runbook-cadence
**Parent:** KIM-289 — [EPIC] UI Testing Strategy & Automation
**Related:** related: KIM-291 — Define 5-10 critical user journeys for UI test automation; related: KIM-290 — Add testTag / contentDescription coverage to commonMain screens; related: KIM-292 — Navigation testing strategy: fakeable Navigator/Router for tests; related: KIM-293 — Compose semantic UI tests for critical flows (Android); related: KIM-294 — Maestro setup + cross-platform E2E smoke suite (Android + iOS); related: KIM-288 — UI testing plan

## Description

## Spec

Layer 3 of the UI testing strategy (EPIC [KIM-289](https://linear.app/kimmo-m/issue/KIM-289/epic-ui-testing-strategy-and-automation)), and the direct answer to [KIM-288](https://linear.app/kimmo-m/issue/KIM-288/ui-testing-plan)'s question about AI/agentic "click around like a human" testing. Write a **runbook document** describing a periodic, human-triggered process where a computer-use/vision agent (e.g. Claude in Chrome / computer-use against an emulator or simulator) exercises the running ReWinds app without a scripted journey, to surface issues the deterministic suites ([KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android) Compose tests, [KIM-294](https://linear.app/kimmo-m/issue/KIM-294/maestro-setup-cross-platform-e2e-smoke-suite-android-ios) Maestro E2E) don't cover. This is documentation/process only — no code, no CI changes.

## Acceptance criteria

- [ ] No new tests required — this is a documentation/runbook ticket, no app code or test code changes
- [ ] A new doc exists (e.g. `docs/agent/testing/AGENTIC-EXPLORATORY-TESTING.md`) describing the runbook: how to launch the app on an Android emulator (and note the iOS simulator case), how to point a computer-use/vision agent at it, and how findings are captured (screenshots + written notes)
- [ ] The runbook lists concrete exploration seed goals (e.g. "open each tab and scroll to the end", "trigger empty/error states", "rotate device", "use AI chat with an unusual query") rather than leaving exploration fully open-ended
- [ ] Cadence is defined (e.g. weekly or pre-release) with a named owner/trigger (Kimmo, manually invoked)
- [ ] The feedback loop is defined in writing: a confirmed finding becomes a Linear bug ticket in KIM/ReWinds; a finding that recurs or covers a critical journey gets promoted into a Maestro flow ([KIM-294](https://linear.app/kimmo-m/issue/KIM-294/maestro-setup-cross-platform-e2e-smoke-suite-android-ios), `.maestro/flows/`) or a Compose semantic test ([KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android))
- [ ] The doc explicitly states this process is **non-CI-gating** — it never blocks a PR or release, distinguishing it from [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android) (per-PR) and [KIM-294](https://linear.app/kimmo-m/issue/KIM-294/maestro-setup-cross-platform-e2e-smoke-suite-android-ios) (release-branch)
- [ ] The doc notes rough cost/time expectations for a run (vision-LLM-driven, ~10-30+ steps per journey, so costed and time-boxed per session) so Kimmo can budget runs
- [ ] `docs/agent/testing/TESTING-STRATEGY.md` is updated to reference the new doc as Layer 3, alongside the existing Layer 1 ([KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android)) / Layer 2 ([KIM-294](https://linear.app/kimmo-m/issue/KIM-294/maestro-setup-cross-platform-e2e-smoke-suite-android-ios)) references
- [ ] The doc cross-references [KIM-288](https://linear.app/kimmo-m/issue/KIM-288/ui-testing-plan) (original question), [KIM-293](https://linear.app/kimmo-m/issue/KIM-293/compose-semantic-ui-tests-for-critical-flows-android), and [KIM-294](https://linear.app/kimmo-m/issue/KIM-294/maestro-setup-cross-platform-e2e-smoke-suite-android-ios)

## Definition of done

## Notes

Reviewers: code-reviewer only (doc-only change, no logic/UI to warrant qa-test-agent or ux-ui-reviewer).

Scope is intentionally a plan/runbook, not an implementation of an agentic testing harness — [KIM-289](https://linear.app/kimmo-m/issue/KIM-289/epic-ui-testing-strategy-and-automation)'s epic scope is "have a documented strategy across 3 layers", and Layers 1-2 are now Done ([KIM-290](https://linear.app/kimmo-m/issue/KIM-290/add-testtag-contentdescription-coverage-to-commonmain-screens)-295). This ticket closes the strategy by documenting Layer 3.

Assumption: "agent" in this runbook means a human-triggered Claude session using computer-use/Claude-in-Chrome tooling against a locally running emulator/simulator — not a new automated service. If Kimmo wants an automated/scheduled agentic runner, that would be a separate, larger ticket scoped after this runbook proves the manual process useful.

Per WORKFLOW.md, this ticket is the last child of EPIC [KIM-289](https://linear.app/kimmo-m/issue/KIM-289/epic-ui-testing-strategy-and-automation) — once Done, the epic itself may be ready for Kimmo to close (PO does not move epics).

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-15T18:05:21.260Z

PR #34 merged into develop. Marcy's review passed on round 3 after two rounds of doc-accuracy fixes (corrected Layer 1/KIM-293 test path, runner, and per-PR-gating claims throughout the new runbook and strategy doc). Marking as Done.

This was the last open child of EPIC KIM-289 — KIM-290 through KIM-296 are all now Done. The 3-layer UI testing strategy (Compose semantic tests, Maestro E2E, agentic exploratory runbook) is complete. @kimmo.myllyviita@gmail.com may want to close KIM-289 itself.

### kimmo.myllyviita@gmail.com — 2026-06-15T17:47:55.965Z

Pushed follow-up fix `fef3d91` on `kim-296-agentic-exploratory-testing-runbook` (PR #34).

Addresses the two remaining overstated Layer 1 gating claims Marcy flagged, plus one more found in a grep sweep:

- **Non-CI-gating table (was line 220):** Layer 1 row now reads "Manual / `connectedAndroidTest`" / "Not yet — per-PR CI runs only `:composeApp:testDebugUnitTest`; instrumented journey tests not yet wired in" — no longer "Every PR" / "Yes — red blocks merge".
- **Cadence note (was line 164):** reworded to be accurate — Layer 1 unit tests run per-PR, KIM-293 instrumented journey tests run manually / `connectedAndroidTest` (not yet per-PR), Layer 2 Maestro manual / push-to-master per `e2e-smoke.yml`.
- **References entry for KIM-293:** changed unqualified "(per-PR gate)" to "(instrumented; run manually / via `connectedAndroidTest`, not yet a per-PR gate)" for consistency.

Verified against `.github/workflows/ci.yml` (per-PR: `assembleDebug` + `:composeApp:testDebugUnitTest` + coverage, no `connectedAndroidTest`) and `e2e-smoke.yml` (push to master + manual). Grep sweep for "every PR" / "per-PR gate" now shows only the intentionally qualified intro callout ("intended as a per-PR gate once wired into CI").

Build/tests: doc-only change, no app/test code touched. Re-adding **in-review**.

### kimmo.myllyviita@gmail.com — 2026-06-15T17:46:03.741Z

**Marcy re-review (commit `c8508fc`) — Needs changes (1 Major)**

The two corrections I checked are accurate against the repo:
- `composeApp/build.gradle.kts:208` wires the `androidTest` source set to `src/androidInstrumentedTest/kotlin` — path now correct in both docs. ✅
- `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` — runner correction accurate; the KIM-293 journey tests do live in `androidInstrumentedTest/kotlin/`. ✅
- `.github/workflows/ci.yml` per-PR runs only `assembleDebug` + `:composeApp:testDebugUnitTest` (+ coverage), **not** `connectedAndroidTest` — so "not yet gated per-PR" is accurate, and `TESTING-STRATEGY.md` (table + note) is correct. ✅

Diff is still doc-only (two `.md` files, additions only). ✅

**Major — the fix is incomplete; `AGENTIC-EXPLORATORY-TESTING.md` now contradicts itself on the exact claim from the last review.** The intro (lines 14-16) was corrected, but two further occurrences of the old "Layer 1 runs every PR / gates merge" claim were missed, and they sit in the *same file*:

- `docs/agent/testing/AGENTIC-EXPLORATORY-TESTING.md:220` — the "Non-CI-gating" table still says Layer 1 → **"Every PR"** / **"Yes — red blocks merge"**. This is the identical error from the prior review, and it directly contradicts lines 14-16 of this same document.
- `docs/agent/testing/AGENTIC-EXPLORATORY-TESTING.md:164` — "the deterministic layers still run on every PR and release" overstates Layer 1 for the same reason (Layer 1 is manual / `connectedAndroidTest`, not per-PR).

Fix: bring both in line with the corrected text and the `TESTING-STRATEGY.md` note — Layer 1 is currently manual / `connectedAndroidTest`, not yet per-PR-gated (open KIM-293 CI-wiring follow-up). The line-164 sentence should attribute "every PR" to the unit-test layer only, not the Compose/Layer-1 suite.

No other lingering `androidUnitTest` / Robolectric / "every PR" references remain (grep clean apart from the two above and the intentional "not Robolectric" denials).

Removing `in-review`; issue stays In Progress. Once the two lines are reconciled this is a straightforward pass.

### kimmo.myllyviita@gmail.com — 2026-06-15T17:44:28.982Z

Addressed Marcy's Major finding (Layer 1 / KIM-293 test location + CI-gating).

**Fix** (commit `c8508fc`, pushed to `kim-296-agentic-exploratory-testing-runbook`):

- Corrected the KIM-293 Layer 1 path in both docs: `composeApp/src/androidUnitTest/` → `composeApp/src/androidInstrumentedTest/kotlin/` (wired to the `androidTest` source set, `composeApp/build.gradle.kts:208`).
- Removed the incorrect "(Robolectric)" label — these are instrumented Compose UI tests run via `AndroidJUnitRunner`, not Robolectric.
- Fixed the strategy-table gating claim: `ci.yml` per-PR run only invokes `:composeApp:testDebugUnitTest` (confirmed at `ci.yml:37`), **not** `connectedAndroidTest`, so Layer 1 instrumented tests are **not yet gated per-PR**. Table now reads "Manual / `connectedAndroidTest`" with a note flagging the open KIM-293 CI-wiring follow-up parked for Kimmo.
- Updated the runbook's "Layer 1 (per-PR gate)" prose to reflect the not-yet-wired state.

Files touched:
- `docs/agent/testing/AGENTIC-EXPLORATORY-TESTING.md`
- `docs/agent/testing/TESTING-STRATEGY.md`

Doc-only change, no build/test impact. Re-added `in-review` label.

### kimmo.myllyviita@gmail.com — 2026-06-15T17:42:30.972Z

**Code review — changes needed (Marcy)**

Diff is genuinely doc-only (`AGENTIC-EXPLORATORY-TESTING.md` +276, `TESTING-STRATEGY.md` +27, no app/test/CI code). The runbook is strong: all 8 AC items are substantively covered, the non-CI-gating claim matches `e2e-smoke.yml` reality (triggers only on `push: [master]` + `workflow_dispatch`; this PR adds no workflow and no new triggers), and the Maestro facts (1.39.0, `.maestro/flows/`, J1–J8, `com.km.rewinds`, preconditions) match `.maestro/README.md`. The "no new tests required" exemption is correctly stated.

One Major doc-accuracy issue must be fixed before merge — the Layer 1 location/tech is wrong, in both files:

- **Wrong path for KIM-293 Compose tests (Major).** Both docs say the Layer 1 tests live in `composeApp/src/androidUnitTest/`. They actually live in `composeApp/src/androidInstrumentedTest/kotlin/` (wired to the `androidTest` source set in `composeApp/build.gradle.kts:208`). `androidUnitTest/` does not exist in the repo.
  - `docs/agent/testing/AGENTIC-EXPLORATORY-TESTING.md:14` — "in `composeApp/src/androidUnitTest/`"
  - `docs/agent/testing/TESTING-STRATEGY.md:30` — table "Reference / location" cell: `composeApp/src/androidUnitTest/` (Robolectric)

- **Wrong test technology label (Major, same line).** `TESTING-STRATEGY.md:30` labels Layer 1 "(Robolectric)". The KIM-293 tests use `@RunWith(AndroidJUnit4::class)` with `AndroidJUnitRunner` (instrumented Compose UI tests), not Robolectric. Drop the "(Robolectric)" label or correct it to instrumented Compose UI tests.

These matter because this doc is the canonical map a future agent will follow to find/add promoted tests — a wrong path + wrong runner sends them to a non-existent directory.

Non-blocking observation for Kimmo (not for Randy, pre-existing to KIM-293, out of scope here): the strategy table asserts Layer 1 runs on "Every PR" and "Gates: yes", but per-PR `ci.yml` runs only `:composeApp:testDebugUnitTest` — the KIM-293 instrumented tests are in the `androidTest` source set and would need `connectedAndroidTest`/an emulator to actually execute, which the PR CI does not do. So Layer 1's per-PR gating may be aspirational rather than wired. Worth a separate look, independent of this doc ticket.

Removing `in-review`; issue stays In Progress for Randy to fix the two path/tech references (and the duplicate in `TESTING-STRATEGY.md`). Re-tag `in-review` when updated.

— Marcy (Opus 4.8). Doc-accuracy review; recommend Kimmo skim the corrected table before merge.

### kimmo.myllyviita@gmail.com — 2026-06-15T17:40:07.728Z

**Developer handover — ready for code-reviewer**

- **Branch:** `kim-296-agentic-exploratory-testing-runbook`
- **PR:** https://github.com/kmyllyvi/ReWinds/pull/34 (Closes KIM-296)
- **Build:** pass (N/A — doc-only, no app/test code touched)
- **Tests:** pass (N/A — doc-only)
- **New tests written:** no — doc-only/runbook ticket, no app logic or test code changed (AC explicitly exempts this).

**Summary**

Layer 3 of the UI testing strategy is now documented, closing EPIC KIM-289 and answering KIM-288.

- New `docs/agent/testing/AGENTIC-EXPLORATORY-TESTING.md` runbook covering all AC:
  - **Scope** — "agent" = a human-triggered Claude/computer-use session against a local emulator/simulator, not an automated service.
  - **How to run** — launch on Android emulator (`installDebug`) and the iOS simulator (Xcode/`simctl`) case; precondition provisioning; how to point the agent and capture findings (screenshots + written notes).
  - **10 concrete seed exploration goals** (G1–G10: open/scroll every tab, empty/error states, device rotation, unusual AI-chat queries, rapid add/delete, unexpected navigation order, month-grid edges, settings round-trip, font/scale).
  - **Cadence + ownership** — roughly weekly and always pre-release; manual trigger; owned by Kimmo.
  - **Feedback loop** — confirmed finding → Linear bug; recurring/critical-path finding → promoted to a Maestro flow (KIM-294, `.maestro/flows/`) or a Compose semantic test (KIM-293).
  - **Non-CI-gating** — stated explicitly, with a table contrasting Layer 1 (per-PR) / Layer 2 (release) / Layer 3 (never gates).
  - **Cost/time budget** — vision-LLM, ~10–30+ steps/journey, time-boxed mini-sessions, separate live-API spend for chat goals, plus budgeting guidance.
  - **Cross-references** KIM-288, KIM-289, KIM-291, KIM-293, KIM-294.
- `docs/agent/testing/TESTING-STRATEGY.md` — new "Layered UI testing strategy" section with a 3-layer table and prose referencing Layer 1 (KIM-293) / Layer 2 (KIM-294) / Layer 3 (KIM-296), linking the new runbook.

**Deviations**

- TESTING-STRATEGY.md had no pre-existing Layer 1/2 section (it's an older Feb logic-coverage doc), so rather than only "adding a Layer 3 reference alongside existing ones," I added a self-contained 3-layer section that establishes all three layers at once. Net effect satisfies the AC and gives the doc a single coherent UI-testing overview.
- Note for triage: this is the last child of EPIC KIM-289. Once merged/Done, the epic itself may be ready for Kimmo to close (PO does not move epics).

### kimmo.myllyviita@gmail.com — 2026-06-15T17:35:57.469Z

Specced (Spec/AC/DoD per WORKFLOW.md) and moved to Todo for Randy, following the same pattern as KIM-292/293/295 (last child of epic, moved directly to Todo).

This is a doc-only ticket: Layer 3 of the UI testing strategy (agentic/computer-use exploratory testing runbook), answering KIM-288's original cost/feasibility question. Deliverable is a new `docs/agent/testing/AGENTIC-EXPLORATORY-TESTING.md` runbook plus a Layer 3 reference added to `docs/agent/testing/TESTING-STRATEGY.md`. No app code, no test code, no CI changes — see AC for the "no new tests required" exemption.

Key things the runbook needs to nail (see AC for full list):
- Concrete seed exploration goals, not open-ended
- Cadence + owner (Kimmo, manual trigger)
- Feedback loop: confirmed findings -> Linear bug tickets; recurring/critical findings promoted into KIM-294 Maestro flows or KIM-293 Compose tests
- Explicitly non-CI-gating
- Rough cost/time budget per run (vision-LLM, ~10-30+ steps/journey)

Reviewer: code-reviewer only (doc-only).

Once this lands and is Done, EPIC KIM-289 has all children (KIM-290-296) Done — flagging for Kimmo to consider closing the epic himself (not moving it as PO).

