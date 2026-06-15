---
name: epic-kim289-ui-testing
description: Status of KIM-289 UI Testing epic child tickets — which still need PO spec passes
metadata:
  type: project
---

KIM-289 (UI Testing Strategy epic): KIM-290, 291, 292, 293, 294 are Done.

KIM-295 (iOS UI test approach decision) — specced 2026-06-15, moved to Todo (High
priority), handoff comment posted. Doc-only ticket: the decision (Maestro-only for
iOS, no native Compose UI test target) is already implemented in practice via
KIM-294's `.maestro/README.md`; remaining work is formalizing it in the stale
`docs/agent/testing/TESTING-STRATEGY.md` Section 5.1 and cross-linking the two docs.

KIM-296 (agentic/AI exploratory testing runbook) — still in Backlog, no spec yet,
set to Low priority per its own notes (depends on KIM-293/294 landing first, which
they now have — but it's a process/runbook ticket, lowest urgency of the two
remaining children). Needs a full spec pass (DoD block + spec-ready) when picked
up next — same pattern as KIM-292/293/295.

**Why**: epic children get specced one at a time as they're picked up; KIM-295 was
prioritized over KIM-296 because it closes a concrete decision/doc gap with an
existing CI hook (`ENABLE_IOS_E2E` var in `.github/workflows/e2e-smoke.yml`)
already pointing at it, whereas KIM-296 is a standalone process doc with no
blocking dependency.

**How to apply**: when KIM-296 is picked up, do the same spec pass (Spec/AC/DoD,
generic DoD block per WORKFLOW.md, spec-ready label). Note: the open KIM-293
CI-wiring follow-up question (instrumented tests on every PR) remains parked for
Kimmo separately — do not resolve it as part of KIM-296.

Note on KIM-289 itself: this is the epic/parent — once 295 and 296 are both Done,
consider whether the epic itself should be closed (PO doesn't move epics; flag to
Kimmo when both children land).
