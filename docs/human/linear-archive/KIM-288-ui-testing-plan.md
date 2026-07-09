# KIM-288: UI testing plan

**Status:** Done · **Priority:** Urgent · **Labels:** EPIC, QA
**Created:** 2026-06-15T07:05:34.222Z · **Completed:** 2026-06-15T18:06:44.784Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-288/ui-testing-plan
**Related:** related: KIM-292 — Navigation testing strategy: fakeable Navigator/Router for tests; related: KIM-295 — Decide iOS UI test approach: Maestro-only vs native Compose UI test target; related: KIM-290 — Add testTag / contentDescription coverage to commonMain screens; related: KIM-294 — Maestro setup + cross-platform E2E smoke suite (Android + iOS); related: KIM-291 — Define 5-10 critical user journeys for UI test automation; related: KIM-293 — Compose semantic UI tests for critical flows (Android)

## Description

I want to plan proper UI testing for this app. Reason is that UI is getting more complicated and not all user journeys work as expected - I'd like to have robust UI automation using latest tech.

I've heard AI can act really human way of just "clicking" around the app like human without needing to specify all user journeys?

Does this work in native env and what cost?

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-15T07:17:55.039Z

**Note for Shirley (PO) — epic + tickets ready for your review**

Armin's architectural plan for UI testing has been reviewed and approved from a QA execution standpoint. I've turned it into a technical epic and prioritized child tickets:

- **Epic: KIM-289** — [EPIC] UI Testing Strategy & Automation (layered approach: Compose semantic tests on every PR -> Maestro E2E smoke nightly/pre-release -> periodic AI/agentic exploration, non-gating)

Child tickets (priority order):
1. **KIM-290** (Urgent) — Add testTag/contentDescription coverage to commonMain (hard prerequisite; currently zero coverage)
2. **KIM-291** (Urgent) — Define the 5-10 critical user journeys — **this is the PO task assigned to you**. Maestro scope and CI-gating value depend on this list.
3. **KIM-292** (High) — Navigation testing strategy (fakeable Navigator/Router)
4. **KIM-293** (High) — Compose semantic UI tests for critical flows (Android, every PR)
5. **KIM-294** (High) — Maestro setup + cross-platform E2E smoke suite (Android + iOS)
6. **KIM-295** (Medium) — Decide iOS UI test approach (QA recommends Maestro-only for iOS)
7. **KIM-296** (Low) — Periodic AI/agentic exploratory testing runbook + cadence

**Action requested:** please review/refine the acceptance criteria across the epic and child tickets, and own **KIM-291** (defining the critical journeys) — that one unblocks both the Compose flow tests (KIM-293) and the Maestro suite (KIM-294). Happy to adjust scope or sequencing based on your input.

