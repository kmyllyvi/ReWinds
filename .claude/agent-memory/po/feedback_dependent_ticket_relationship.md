---
name: feedback_dependent_ticket_relationship
description: When a new raw idea appears to replace/conflict with an existing Backlog ticket, read the existing ticket fully and state the supersede/complement read explicitly in Notes — don't guess silently, don't auto-cancel either
metadata:
  type: feedback
---

When drafting a spec for a new idea that overlaps or conflicts with an existing Backlog ticket (e.g. a
new interaction pattern proposed to replace an old one that's still queued), fetch the existing ticket's
full body via `get_issue` and compare mechanisms concretely — don't infer from the title alone.

**Why**: KIM-364's raw idea ("long-press → archive, because swipe-to-delete had a real opacity bug")
looked at a glance like it might be unrelated to KIM-274 ("restore swipe-to-delete after tab bar
redesign"), but reading KIM-274's full body confirmed they're mechanism-incompatible: KIM-274 wants to
revive the exact gesture KIM-364 says was abandoned for a UI bug. Grepping the current code
(`PlaceButton.kt`) added a third data point — the old gesture isn't even wired up anymore, cutting
against KIM-274 being complementary.

**How to apply**: state the supersede-vs-complement read plainly in the new ticket's Notes (e.g. "my
read is X supersedes Y; I have not cancelled Y myself — that's Kimmo's call"). This is exactly the kind
of decision that affects scope/backlog hygiene but is cheap to reverse (Kimmo can just say "no, keep
both") — so it belongs as a flagged Note for Gate 1, not a `needs-human` escalation, and not a silent
auto-cancel of the older ticket either.
