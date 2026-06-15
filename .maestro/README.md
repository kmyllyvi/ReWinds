# Maestro E2E smoke suite (KIM-294)

Layer 2 of the [UI testing strategy](../docs/) (EPIC KIM-289): black-box,
accessibility-tree-driven E2E flows that drive the **built** app through the 8 P0
critical journeys (J1–J8) defined in KIM-291. This is also the **primary iOS UI
test path** (KIM-295) — no native Compose UI test target is stood up for iOS.
The decision record (rationale + revisit conditions) lives in
[`docs/agent/testing/TESTING-STRATEGY.md` §5.1](../docs/agent/testing/TESTING-STRATEGY.md#51-ios-testing).

Selectors come from `composeApp/src/commonMain/kotlin/core/TestTags.kt` (KIM-290).
Compose maps `Modifier.testTag(...)` onto the Android `testTag` and the iOS
`accessibilityIdentifier`, so the same `id:` selector works on both platforms.

> These flows run against a **built app + emulator/simulator**. They are slow and
> (for J6) hit the live Anthropic API, so they run **manually / on release to
> master only** (no nightly cron — keeps CI minutes in budget on the free plan),
> never on every PR. Per-PR UI gating is KIM-293 (Compose semantic tests).

## Pinned Maestro version

```
Maestro 1.39.0
```

Pin locally and in CI to this exact version — Maestro's YAML/CLI surface changes
between minors. Install:

```bash
# Pinned install (recommended)
curl -fsSL "https://get.maestro.mobile.dev" | MAESTRO_VERSION=1.39.0 bash
maestro --version   # must print 1.39.0

# Homebrew (may install a newer version — verify, then pin if it drifts)
brew install maestro
```

CI installs the same pinned version (see `.github/workflows/e2e-smoke.yml`).

## App id (`${APP_ID}`)

Every flow uses `appId: ${APP_ID}` so one flow file runs on both platforms.
Export it before running:

| Platform | App id                                          |
| -------- | ----------------------------------------------- |
| Android  | `com.km.rewinds`                                |
| iOS      | `com.km.rewinds<TEAM_ID>` (bundle id is `${BUNDLE_ID}${TEAM_ID}`, see `iosApp/Configuration/Config.xcconfig`) |

On iOS the installed bundle id includes your Apple Developer `TEAM_ID` suffix
(from `iosApp/Config.local.xcconfig`). Find the installed id with
`xcrun simctl listapps booted | grep -i rewinds`.

## Running locally

### Android emulator

```bash
# 1. Start an emulator and build + install the debug app
./gradlew :composeApp:installDebug -PincludeAllTargets=false

# 2. Run the suite
export APP_ID=com.km.rewinds
maestro test .maestro/                       # whole P0 suite
maestro test .maestro/flows/j2_add_place_via_search.yaml   # single flow
```

### iOS simulator

```bash
# 1. Build + install on a booted simulator via Xcode (Cmd+R once), or:
xcodebuild -workspace iosApp/iosApp.xcworkspace -scheme iosApp \
  -configuration Debug -sdk iphonesimulator -derivedDataPath build
xcrun simctl install booted \
  build/Build/Products/Debug-iphonesimulator/iosApp.app

# 2. Resolve the team-suffixed bundle id and run
export APP_ID="$(xcrun simctl listapps booted | grep -i rewinds | head -1 | sed -E 's/.*"([^"]*rewinds[^"]*)".*/\1/')"
maestro test .maestro/
```

> iOS builds are done via Xcode (`iosApp.xcworkspace`), **not** Gradle iOS tasks
> — see `CLAUDE.md`. Maestro only drives the already-installed app.

## Test data / keys (preconditions)

Some flows need state that a fresh install does not provide. Provision before running:

| Flow                | Precondition                                                        |
| ------------------- | ------------------------------------------------------------------- |
| J1, J7              | Fresh state — handled in-flow by `clearState`.                      |
| J2, J3              | Visual Crossing key configured so place search resolves.            |
| J4                  | A month in **FULL** state (downloaded data) — see selector gap below. |
| J5                  | A month in **NO_DATA** state (a fresh place's first cell).          |
| J6                  | A valid **Anthropic** key + network (live API call).                |
| J8                  | ≥2 chat sessions — seeded in-flow via "New chat".                   |

Configure keys through the in-app Settings screen before the run, or pre-seed via
the platform key store (Android `BuildConfig`/gradle.properties, iOS Keychain).
Run with the device **locale set to English** — three flows fall back to text
selectors (see gaps) and those strings are English.

## Selector gaps — elements with no `TestTags` constant

Per the KIM-294 AC ("if any `TestTags` constant needed by a journey doesn't exist
yet, call it out — don't invent ad-hoc selectors silently"), these journeys hit
elements that have **no** `TestTags` constant on `develop` today. Each is handled
with an explicit, commented text/index fallback in the flow, not a silent one:

1. **Month cell state (J4 FULL, J5 NO_DATA).** `TestTags.PLACE_MONTH_CELL` is
   non-indexed and state-agnostic; nothing distinguishes a FULL cell from a
   NO_DATA cell by id. Flows tap `index: 0` and rely on a seeded precondition.
2. **`DownloadMissingDaysDialog` (J5).** `PlaceSummaryView.kt:181` — no testTag on
   its title / Download / Cancel. Flow falls back to localized text.
3. **Chat API-key-missing `AlertDialog` (J7).** `ChatView.kt:203` — no testTag on
   its title / "Go to Settings" button. Flow falls back to localized text; the
   post-navigation assertion uses `SETTINGS_ANTHROPIC_KEY_ROW` (locale-independent).

Recommended follow-up (KIM-290 territory, out of scope here): add
`PLACE_MONTH_CELL_FULL` / `PLACE_MONTH_CELL_NO_DATA` (or a state-encoded tag),
`DOWNLOAD_DIALOG_*`, and `CHAT_API_KEY_DIALOG_*` constants, then replace the text
fallbacks above with `id:` selectors.

## Platform-specific forks

Documented inline in each flow. Summary:

- **Keyboard (J2, J6, J7):** `hideKeyboard` after typing — needed on iOS so the
  software keyboard does not obscure the next target; no-op on Android.
- **Locale (J5, J7):** the text-fallback selectors are English; switch device
  locale to English or swap the strings. All `id:`-based selectors are
  locale-independent.

## Flow inventory

| Flow file                              | Journey | Summary                              |
| -------------------------------------- | ------- | ------------------------------------ |
| `j1_first_launch_empty_state.yaml`     | J1      | Empty Home + VC key nudge banner     |
| `j2_add_place_via_search.yaml`         | J2      | Search → suggestion → PlaceRow       |
| `j3_open_place_summary.yaml`           | J3      | PlaceRow → summary → back            |
| `j4_monthly_statistics_full_month.yaml`| J4      | FULL month cell → statistics         |
| `j5_download_missing_days_no_data.yaml`| J5      | NO_DATA cell → download dialog       |
| `j6_ask_ai_chat_question.yaml`         | J6      | Chat question → assistant reply      |
| `j7_chat_blocked_no_api_key.yaml`      | J7      | Missing key → dialog → Settings      |
| `j8_switch_chat_sessions.yaml`         | J8      | Session switcher → switch session    |
