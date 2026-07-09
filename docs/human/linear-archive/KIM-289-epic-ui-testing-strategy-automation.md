# KIM-289: [EPIC] UI Testing Strategy & Automation

**Status:** Done · **Priority:** High · **Labels:** _none_
**Created:** 2026-06-15T07:15:59.195Z · **Completed:** 2026-06-15T18:06:46.253Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-289/epic-ui-testing-strategy-and-automation
**Parent:** KIM-288 — UI testing plan

## Description

## Goal

Establish robust, layered UI test automation for ReWinds (Compose Multiplatform, Android + iOS) so that critical user journeys are protected against regressions as the UI grows. Tracks the strategy investigation in [KIM-288](https://linear.app/kimmo-m/issue/KIM-288/ui-testing-plan).

## Current State (verified 2026-06-15)

* **One** Compose UI test exists: `composeApp/src/androidInstrumentedTest/kotlin/ai/ChatLayoutTest.kt` ([KIM-127](https://linear.app/kimmo-m/issue/KIM-127/bug-chat-messages-are-visible-under-chat-header)) — `createComposeRule`, text-selector based, brittle.
* **Zero** `Modifier.testTag(...)` usage anywhere in `commonMain` — UI tests can only match on text today.
* `ui-test-junit4` is already declared as an `androidTestImplementation` dependency in `composeApp/build.gradle.kts` — framework is set up but barely used.
* Strong Koin DI test pattern exists (`composeApp/src/commonTest/kotlin/ai/ChatViewModelKoinGraphTest.kt`) — good foundation for injecting fakes/test doubles.
* No iOS UI test target, no Maestro config, no E2E/agentic tooling.

## Recommended Layered Approach

1. **Compose semantic tests** (`commonTest` / `androidInstrumentedTest`) — component-level + critical-flow correctness. Run on **every PR** via the `./gradlew buildAndroidOnly` pipeline. Reuse the existing Koin test-module pattern for fakes.
2. **Maestro E2E smoke suite** — 5-10 YAML flows covering core journeys, cross-platform (Android + iOS) against built apps. Run **nightly / pre-release**.
3. **AI/agentic exploration** (computer-use style) — periodic, **human-triggered** exploratory testing. Surfaces unexpected issues. **NOT a merge gate** (slow, costly, non-deterministic — each step is a vision LLM call, journeys are 10-30+ steps).

## On the "AI clicks around like a human" question (from [KIM-288](https://linear.app/kimmo-m/issue/KIM-288/ui-testing-plan))

Yes, this works against simulator/emulator screenshots via computer-use style agents, and it works in native environments. But it is best used as periodic exploratory testing, not as CI gating: each step is an LLM vision call, full journeys run 10-30+ steps, and runs are slow, costly, and non-deterministic. The reliable, cheap, deterministic CI coverage comes from layers 1 (Compose) and 2 (Maestro). All three layers depend on the same prerequisite: stable accessibility identifiers (`testTag` / `contentDescription`).

## Prerequisites (sequenced into child tickets)

* Add `Modifier.testTag(...)` / `contentDescription` to key interactive elements across `commonMain` (currently zero) — **blocks everything else**.
* Decide navigation testing strategy (how `Navigator`/`Router` inject fakes / test NavController).
* Decide iOS approach: stand up an iOS Compose UI test target on `iosSimulatorArm64`, OR rely on Maestro for iOS to avoid that friction.
* Define the 5-10 critical user journeys — **PO task (Shirley)**. Maestro and CI-gating value depend on this list.

## QA execution notes / refinements

* Sequencing: testTag coverage (KIM child #1) is a hard prerequisite for the Compose flow tests and the Maestro suite — both rely on stable identifiers. Prioritized accordingly.
* The single existing `ChatLayoutTest.kt` should be migrated off text selectors onto testTags once they land, to harden it and serve as the reference pattern.
* Recommend Maestro as the primary iOS path initially (iOS Compose UI tests on Kotlin/Native are friction-heavy and slow to compile ~10-44min); revisit a native iOS Compose UI target only if Maestro proves insufficient.
* Agentic exploration should be scoped as a documented runbook + periodic cadence, with findings funneled back into Maestro/Compose tests — not as standing infra.

## Architectural references

* `composeApp/build.gradle.kts`
* `composeApp/src/androidInstrumentedTest/kotlin/ai/ChatLayoutTest.kt`
* `composeApp/src/commonTest/kotlin/ai/ChatViewModelKoinGraphTest.kt`
* `composeApp/src/commonMain/kotlin/core/` (Navigator, Router, NavigationRoutes, TabRoutingNavigator)
* Screens: `home/HomeView.kt`, `ai/ChatView.kt`, `ai/ChatSessionSwitcher.kt`, `settings/SettingsView.kt`, `place/PlaceSummaryView.kt`, `place/MonthlyStatisticsView.kt`

Plan authored by Armin (codebase-architect), reviewed & approved from a QA execution standpoint. Child tickets below.

## Comments

_No comments._
