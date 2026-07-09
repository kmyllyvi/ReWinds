# KIM-267: Tab bar navigation — Places / Chat / Settings

**Status:** Done · **Priority:** High · **Labels:** spec-ready, Feature, design
**Created:** 2026-06-04T12:09:56.754Z · **Completed:** 2026-06-05T14:16:02.031Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-267/tab-bar-navigation-places-chat-settings
**Related:** related: KIM-265 — Design token / theme system — Midnight Blue palette

## Description

## Spec

Replace the current navigation structure with a 3-tab bottom navigation bar: Places (home), Chat, and Settings. Map is not a tab — it remains a push destination reached by drilling into a Place Detail. The tab bar must be styled to match the Midnight Blue design: dark semi-transparent background (`rgba(7,15,28,0.96)`), top border in `#1a3050`, active tab icon/label in `#8ecff0`, inactive in `#4a7a9b`. This ticket covers the navigation scaffolding only; screen content is redesigned in separate tickets.

## Acceptance criteria

- [ ] The app has exactly 3 tabs visible at the bottom of every tab-level screen: Places, Chat, Settings — in that left-to-right order
- [ ] Tapping a tab navigates to the corresponding screen; the previously active screen's state is preserved when switching tabs (standard back-stack behaviour)
- [ ] Map screen is NOT a tab; it is reachable by tapping a place in the Places list and is dismissed with back navigation
- [ ] Active tab indicator uses `accentBlue` (`#8ecff0`); inactive tabs use `textTertiary` (`#4a7a9b`)
- [ ] Tab bar background is `rgba(7,15,28,0.96)` with a 1 dp top border in `border` (`#1a3050`)
- [ ] Tab bar height is 72 dp (matching design)
- [ ] The tab bar is absent on the Map screen and Place Detail screen (push destinations)
- [ ] Navigation state is managed in a ViewModel or Navigator (not inside a composable via `remember`)
- [ ] Deep-linking into Chat from Place Detail (the "Chat" button on that screen) is preserved or a follow-up ticket is explicitly noted

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`); new logic has basic coverage
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations
- [ ] Existing `Navigator` / `NavigationRoutes` wiring is updated or replaced, not duplicated
- [ ] Blocked by: [KIM-265](https://linear.app/kimmo-m/issue/KIM-265/design-token-theme-system-midnight-blue-palette) (theme tokens needed for tab colour values)

## Notes

Priority: High — all screen redesign tickets assume this navigation shell exists.
Proposed size: Medium (2–3 hours). Navigation refactors touch every screen entry-point.
Reviewers needed: code-reviewer, ux-ui-reviewer.
Assumption: the existing `Navigator`/`Router` in `commonMain` can be extended; if a full replacement is needed, the developer should flag it in the PR.
Assumption: "Chat from Place Detail" deep-link is a nice-to-have; if it requires significant extra work it can be a follow-up ticket rather than blocking this one.

## Comments

_No comments._
