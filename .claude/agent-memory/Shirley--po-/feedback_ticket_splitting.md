---
name: feedback-ticket-splitting
description: When to split tickets for the ReWinds redesign — data vs UI, foundation vs screen
metadata:
  type: feedback
---

When a redesign involves both foundational infrastructure (theme tokens, navigation shell, shared composables) and per-screen UI work, split into separate tickets at the infrastructure boundary.

**Why:** Screen tickets cannot be safely specced or built until the tokens/shell they reference exist. Splitting keeps each ticket at 1–3 hours and makes dependencies explicit.

**How to apply:**
- Theme token system = its own ticket, blocked by nothing, prerequisite for all screens
- Shared background composable = its own ticket, blocked by tokens
- Navigation shell = its own ticket, blocked by tokens, prerequisite for tab-level screens
- Each screen = its own ticket, blocked by whichever foundation items it uses
- DB migration + Compose UI in the same issue = split into two linked tickets (UI blocks on data)
