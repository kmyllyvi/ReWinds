# KIM-273: Settings screen redesign — grouped list, toggles, key status chips

**Status:** Done · **Priority:** High · **Labels:** design, in-review, spec-ready, Feature
**Created:** 2026-06-04T12:11:31.439Z · **Completed:** 2026-06-09T12:59:16.212Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-273/settings-screen-redesign-grouped-list-toggles-key-status-chips
**Related:** related: KIM-266 — Background isobar texture — shared Canvas layer; related: KIM-267 — Tab bar navigation — Places / Chat / Settings; related: KIM-265 — Design token / theme system — Midnight Blue palette

## Description

## Spec

Redesign the Settings screen with the Midnight Blue theme and a grouped-list layout. Four groups: General (language, units, wind speed unit), API Keys (Anthropic and Visual Crossing, each showing a "Configured" status chip), Data (toggles and export action), and About (version string and a destructive "Delete all data" row in red). The screen is a tab destination; it uses the isobar background. No changes to settings persistence logic — UI restyle only, but this ticket does own adding the missing ViewModel state (language, units, wind speed unit, anthropicKeyConfigured, visualCrossingKeyConfigured, autoRefreshEnabled, wifiOnlyEnabled) because it is simple, self-contained, and tightly coupled to this screen.

## Acceptance criteria

- [ ] Screen background is `pageBg` with isobar texture as the lowest layer
- [ ] Settings are organised into exactly 4 labelled groups: General, API Keys, Data, About; group labels are in `textTertiary` uppercase small
- [ ] Each row in General has a label in `textPrimary` and a current-value or chevron on the right in `textSecondary`
- [ ] API Keys group shows two rows (Anthropic, Visual Crossing); each row shows a status chip: "Configured" chip in `accentBlue` with a checkmark when a key is stored, or a "Not set" chip in `accentAmber` (`#e8a030`, with background tint `rgba(232,160,48,0.1)` and border `rgba(232,160,48,0.2)`) when absent — chip state is driven by ViewModel, not computed in the composable
- [ ] Each API key row shows a sub-label below the row title in a smaller muted colour (`textSecondary` or `textTertiary`): "AI chat feature" for Anthropic, "Weather data" for Visual Crossing
- [ ] Data group contains at least one toggle switch and an export action row; toggle thumb colour is `accentBlue` when on, `textTertiary` when off; toggle state is driven by ViewModel
- [ ] About group shows the app version string and a "Delete all data" row; "Delete all data" text and icon are in `error` (`#d95060`); tapping it triggers a ViewModel method (confirmation dialog or direct action per existing behaviour)
- [ ] All rows have `surface` background, separated by 1 dp `border`-coloured dividers or with card-style grouping
- [ ] SettingsViewModel exposes state for: language, units, wind speed unit, anthropicKeyConfigured, visualCrossingKeyConfigured, autoRefreshEnabled, wifiOnlyEnabled — all as observable state; no persistence wiring required beyond what already exists
- [ ] The screen compiles and renders inside the tab bar ([KIM-267](https://linear.app/kimmo-m/issue/KIM-267/tab-bar-navigation-places-chat-settings))

## Definition of done

- [ ] Builds on Android (`./gradlew buildAndroidOnly`)
- [ ] Tests pass (`./gradlew :composeApp:testDebugUnitTest`); new ViewModel state has basic unit test coverage
- [ ] No MV\* violations (see ARCHITECTURE-RULES.md)
- [ ] No new lint violations
- [ ] All colour values reference theme tokens
- [ ] Blocked by: [KIM-265](https://linear.app/kimmo-m/issue/KIM-265/design-token-theme-system-midnight-blue-palette) (tokens), [KIM-266](https://linear.app/kimmo-m/issue/KIM-266/background-isobar-texture-shared-canvas-layer) (isobar), [KIM-267](https://linear.app/kimmo-m/issue/KIM-267/tab-bar-navigation-places-chat-settings) (tab nav)

## Notes

Priority: High.
Proposed size: Medium (2–3 hours). Structural layout work plus ViewModel state fields.
Reviewers needed: code-reviewer, ux-ui-reviewer.
Reference: `docs/designs/screens-v1.html` (sixth mockup, "Settings").

**Gap 1 resolved (2026-06-09):** ViewModel state is in scope for this ticket. Language, units, wind speed unit, key-configured booleans, and toggle booleans are all simple observable fields — splitting them out would add coordination overhead for \~30 minutes of work. No persistence wiring is required beyond what already exists.

**Gap 2 resolved (2026-06-09):** Sub-labels are in scope. The mockup shows "AI chat feature" under Anthropic and "Weather data" under Visual Crossing. They are a single Text composable each and removing them would leave the build visually inconsistent with the design. AC updated accordingly.

**Colour decision (2026-06-09):** The "Not set" chip uses `accentAmber` (`#e8a030`), not `textTertiary`. An unconfigured API key is an actionable gap that blocks AI features — it must attract attention. `textTertiary` is a muted placeholder colour; `accentAmber` is explicitly the design-system "Attention / warnings" token. The design HTML (`.key-status.missing`) was correct; the original AC text was wrong. Both are now in agreement.

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-09T10:07:10.138Z

Developer handoff — KIM-273

**Branch:** `kim-273-settings-redesign-grouped-list`
**PR:** https://github.com/kmyllyvi/ReWinds/pull/23 (Closes KIM-273)
**Build:** Kotlin compile pass (`:composeApp:compileDebugKotlinAndroid` ✅). `buildAndroidOnly` fails only on a **pre-existing** lint error (`READ_DEVICE_CONFIG` ProtectedPermissions in `AndroidManifest.xml`) + a config-cache quirk in that task — both unrelated to this change and present on the base commit.
**Tests:** pass (`:composeApp:testDebugUnitTest` ✅) — `SettingsViewModelTest` now 14 tests, 0 failures.
**New tests written:** yes — `composeApp/src/commonTest/kotlin/settings/SettingsViewModelTest.kt` (8 new cases: General defaults, `setUnits`/`setWindSpeedUnit`/`setLanguage`, auto-refresh + wifi-only defaults & toggles, key-configured flags, `refreshKeyStatus`).

**Summary**
- Settings rebuilt as a grouped list over `pageBg` + `IsobarBackground`, four labelled groups (General, API Keys, Data, About) with `textTertiary` uppercase labels.
- New reusable components in `settings/components/SettingsGroup.kt`: `SettingsGroup` (surface card + 1 dp `border` dividers), `SettingsValueRow`, `SettingsToggleRow` (thumb accentBlue on / textTertiary off), `SettingsKeyRow` (+ status chip), `SettingsDestructiveRow` (error red). All colours via `MaterialTheme.rewinds.*`.
- API-key chips are ViewModel-driven: `accentBlue` "Configured" / `attention` (#e8a030) "Not set" with the specified tint+border. Sub-labels "AI chat feature" / "Weather data" under each title.
- `SettingsViewModel` extended with `languageState`, `unitsState`, `windSpeedUnitState`, `anthropicKeyConfigured`, `visualCrossingKeyConfigured`, `autoRefreshEnabled`, `wifiOnlyEnabled` (+ `UnitSystem`/`WindSpeedUnit` enums) — all observable `StateFlow`, no new persistence wiring.
- Existing key entry/delete + Days-of-Interest editor preserved as dialogs launched from their rows. New EN/DE strings added.

**Deviations / notes**
- "Export data" and "Delete all data" rows are present per the mockup but their actions are intentional placeholders — wiring them to `DatabaseExportImport` / a DB-wipe pipeline needs new DI + business logic, which this UI-only ticket's spec excludes. Flagging for reviewer: if a real delete-all is wanted now, that's a follow-up ticket.
- Kept exactly 4 groups per AC; Days-of-Interest is reached via a row in the Data group rather than its own group.
- iOS to be verified manually in Xcode (not built via Gradle, per workflow).

