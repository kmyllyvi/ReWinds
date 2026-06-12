---
name: feedback_ux_gating
description: When Kimmo asks for a UX design before deciding a UI interaction, spec the unblocked non-UI tickets and gate the UI ticket on ux-ui-reviewer instead of guessing.
metadata:
  type: feedback
---

When Kimmo's comment resolves a product/data-model question but explicitly says "I'd like to see a UX
design before deciding" for a specific interaction (e.g. KIM-119: place-chip row behaviour once
multi-chat exists), do not invent the UI spec. Instead:

- Proceed to spec and mark `spec-ready` any split tickets that are unaffected by the open UX question
  (e.g. data-layer tickets).
- Leave the UI ticket unspecced/uncreated, note in the umbrella issue that it's blocked on UX direction,
  and name `ux-ui-reviewer` (Mr.T) as the next step to produce a mockup/recommendation.
- Remove `needs-human` if the underlying product/scope decision (e.g. location-locking) is resolved —
  the remaining UX question is a design step, not a Gate-1 blocker on Kimmo.

**Why:** Kimmo confirmed the split direction (KIM-119, 2026-06-12) but flagged a specific UI detail
(place chips under chat) as needing a design pass first. Speccing that UI blind would likely produce
acceptance criteria that get reworked once the mockup lands.

**How to apply:** Any time a comment resolves the big decision but flags one UI/UX detail as
"needs a design first" — split the work exactly like this rather than blocking the whole epic or
guessing the UI spec.
