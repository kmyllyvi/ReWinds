---
name: behaviour-change-flag
description: When a bug fix changes previously-implicit behaviour (e.g. silent auto-fetch → explicit confirmation), flag it as an assumption in Notes for Kimmo to confirm at Gate 1.
metadata:
  type: feedback
---

When a fix changes what the app silently did before (like auto-fetching data without asking), that is a behaviour change even if it matches the stated desired outcome. Always surface this explicitly in the Notes "Assumptions" block so Kimmo can confirm at Gate 1 rather than discover it post-merge.

**Why:** KIM-321 — three tools (`get_wind_summary`, `get_monthly_stats`, `get_best_days`) previously called `getDaysRange` which auto-fetched silently. The fix introduces an explicit confirmation step. This is correct per the bug report intent but is still a user-visible behaviour change that Kimmo needs to sign off on.

**How to apply:** In the Notes section, list any "previously implicit behaviour that will now be explicit" as an assumption bullet. Do not assume Kimmo wants the stricter behaviour just because the bug report asks for it — confirm.
