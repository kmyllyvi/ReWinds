---
name: linear-labels-by-id
description: Linear save_issue labels field takes label IDs not names; passing names silently wipes all labels
metadata:
  type: feedback
---

`mcp__linear-server__save_issue` `labels` param expects label **IDs**, not name strings. Passing names (e.g. "spec-ready,Feature") is silently ignored and **replaces the full label set** — result comes back with `"labels":[]`, wiping every existing label.

**Why:** discovered on KIM-279 merge — tried to set labels by name to drop `in-review` while keeping `spec-ready`/`Feature`; all three vanished. The WORKFLOW-required outcome (remove `in-review`) was achieved, but the descriptive labels were lost too.

**How to apply:** to remove one label while keeping others, first resolve current label IDs via `list_issue_labels` (or read them off the issue), then pass the surviving IDs. For the review lane, removing `in-review` is the only mandatory label change; losing cosmetic labels is not worth risking a wrong-ID write — note it for Kimmo instead of guessing IDs. See [[fk-cascade-not-enforced]] neighbours in this memory dir for other project gotchas.
