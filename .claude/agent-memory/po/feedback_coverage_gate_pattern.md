---
name: feedback_coverage_gate_pattern
description: How to split and spec a coverage-enforcement ticket (measurement fix + CI gate vs raising actual coverage)
metadata:
  type: feedback
---

When a coverage-enforcement ask combines (a) fixing the measurement/exclusion baseline, (b) wiring a
CI gate, and (c) actually raising coverage on specific low-coverage files — split into two tickets, not
three and not one:

1. **"Build the gate" ticket**: clean exclusion globs, re-baseline, add `jacocoTestCoverageVerification`
   (global floor/ratchet) + a patch/diff-coverage check (e.g. `diff-cover` against the existing JaCoCo
   XML — prefer this over Codecov/paid services if CI already generates the XML the tool needs), and
   add the DoD bullet to WORKFLOW.md. This is config/CI/doc only, no application logic — fits one
   session even though it touches three files (build.gradle.kts, ci.yml, WORKFLOW.md).
2. **"Do the work" ticket**: write the actual unit tests for the named low-coverage files. Blocked-by
   the gate ticket (numbers aren't stable to test against until the gate/baseline exists). This is
   test-writing across multiple components — size at 3-4 hrs, acceptable as one ticket if the
   components share a clear theme (e.g. all on the same feature's data path), otherwise split further.

Key spec decisions that held up:
- Always flag the "branch protection is a repo *setting*, not code" boundary explicitly in the gate
  ticket's AC/Notes — it's the one piece truly outside what Randy can deliver in a PR.
- Threshold numbers (floor %, patch-coverage %) are framed as PO recommendations, not Gate-1-fixed —
  call this out explicitly so Kimmo knows tuning them later is cheap and doesn't need a new spec pass.
- "No behaviour changes" tickets (test-only) get an explicit Notes line: if writing tests surfaces a
  real bug, stop and flag needs-human rather than silently fixing it — keeps scope honest.

See also [[feedback_dod_vs_ac_in_tickets]] pattern referenced in user's own memory (DoD = generic
app-wide policy in WORKFLOW.md; ticket-specific items go in AC) — this directly drove putting "PRs
must pass the patch-coverage gate" into WORKFLOW.md's DoD bullets rather than into the ticket's AC.
