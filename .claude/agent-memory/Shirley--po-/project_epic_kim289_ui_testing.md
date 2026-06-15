---
name: epic-kim289-ui-testing
description: Status of KIM-289 UI Testing epic child tickets — which still need PO spec passes
metadata:
  type: project
---

KIM-289 (UI Testing Strategy epic) child tickets KIM-290-296 were drafted by Seppo/Armin with
reasonable AC text, but only KIM-290 and KIM-291 have the standard ReWinds DoD block
(Android build, tests, MV* check, lint) and the `spec-ready` label.

KIM-292 (nav testing strategy), KIM-293 (Compose flow tests), KIM-294 (Maestro suite),
KIM-295 (iOS test approach decision), KIM-296 (agentic exploration runbook) are still in
Backlog without `spec-ready` — they are not yet at Gate 1 despite looking plausible.

**Why**: Seppo asked PO to "review/refine AC across the epic" but the bigger gap is the
missing DoD/spec-ready formality, not AC wording.

**How to apply**: When KIM-292/293/294 are picked up (293/294 depend on KIM-290 landing and
KIM-291, which is done), do a full spec pass — add DoD block, verify AC are atomic/testable,
add `spec-ready`. KIM-292 may need scope-splitting (touches Navigator/Router across multiple
ViewModels — check against the 1-3hr session sizing rule in [[feedback_ticket_splitting]]).
