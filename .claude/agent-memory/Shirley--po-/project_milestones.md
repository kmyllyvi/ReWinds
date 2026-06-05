---
name: project-milestones
description: ReWinds two-milestone product strategy — Milestone A (BYOK free) and Milestone B (subscription)
metadata:
  type: project
---

ReWinds has two milestones in Linear (project ID 843c4f95-d18a-430e-9df0-08c129c90863):

**Milestone A — MVP Public Free App (BYOK)** (ID: 20a4e799-2585-446b-a16a-511fd8331426)
Users bring their own Visual Crossing and Anthropic API keys. Free, no monetisation. Faster path to store to validate demand.
**iOS App Store only.** Android is explicitly out of scope for Milestone A — Android stays on internal build-config. Any Milestone A issue that previously mentioned Android has been scoped to iOS only.

**Milestone B — Sellable Product (Subscription)** (ID: a7af5a71-dc1f-4e95-8ed9-28236011df8a)
We provide API keys, users pay subscription. Needs auth (Supabase/KIM-74), payments (Apple IAP for iOS via StoreKit 2, Stripe for web/Android), usage tracking, account management.

**Why:** Two-phase strategy: prove demand with free BYOK app first, then build monetisation on top.
**How to apply:** When triaging new ReWinds issues, assign to the right milestone. Anything requiring server-side infrastructure or payments is Milestone B. Anything a solo user can run with their own keys is Milestone A.

Note: Stripe direct billing is NOT allowed for iOS digital goods — Apple IAP (StoreKit 2) is required for iOS subscriptions. Stripe can be used for web and Android.

---

**Visual redesign (added 2026-06-04):** Full Midnight Blue theme redesign underway. Tickets KIM-265–KIM-273 cover the 9-ticket sequence. Foundation order: tokens (KIM-265) → isobar texture (KIM-266) → tab nav (KIM-267) → 6 screen redesigns (KIM-268–KIM-273). All Backlog + spec-ready, awaiting Gate 1. Design artefacts: `docs/designs/color-themes.html` and `docs/designs/screens-v1.html`.
