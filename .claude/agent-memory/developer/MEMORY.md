# Developer (Randy) — Memory Index

One line per topic. Full detail in the linked files. Project facts (stack/build/iOS) live in the
repo: `CLAUDE.md`, `docs/agent/ARCHITECTURE-RULES.md`. Detailed patterns: [notes.md](notes.md).

## Build / verify
- `./gradlew buildAndroidOnly` = compile + unit tests + lint (~4 min). Unit only:
  `./gradlew :composeApp:testDebugUnitTest -PincludeAllTargets=false --no-daemon` (~1.5 min).
- buildAndroidOnly can OOM on the iOS-framework link task locally + has a pre-existing `lintDebug`
  `ProtectedPermissions` failure — verify via the test task if that bites. [notes.md]
- gh: PR/commit bodies with apostrophes break bash heredocs — write a temp file, use `--body-file`. [notes.md]
- CI workflow details (Android/iOS jobs, simulator selection, BuildConfig emission). [notes.md]
- CI job "failure" in ~2-3s with annotation "job was not started because recent account payments
  have failed / spending limit" = GitHub Actions BILLING block, not code. Hits every job. Don't spend
  fix attempts — flag `needs-human`, assign Kimmo; local build/test is the real signal.
- Code coverage (JaCoCo): [coverage.md]. Build-config specifics: [build-config.md].

## Architecture / MV*
- Modal + per-cell-busy state: `showXxx: StateFlow<Boolean>` / `downloadingMonth: Int?` on VM, never
  `remember{mutableStateOf}` in View; open/close methods on VM. [notes.md]
- Progressive render + skeletons: VM clears state then emits in stages; skeletons need DISTINCT
  testTags from loaded elements (Marcy gate). [notes.md]
- Tab nav: 3 back stacks in `Router.kt`; cross-tab intents MUST use `TabRoutingNavigator`, not raw
  `NavigatorImpl`. Launch-gate chain order in `Navigation()`. [notes.md]
- IME/keyboard inset: handle bottom inset in ONE place (Router Scaffold), no child `imePadding()` on
  iOS (double-counts). [notes.md]

## Testability seams (recurring)
- Injectable defaulted-ctor seams so VMs test without platform: `ApiKeyChecker`/`PlatformApiKeyChecker`,
  `AnthropicKeyStore`/`PlatformAnthropicKeyStore`, `emailSender`/`platformLabel`, `ioDispatcher`. [notes.md]
- CRITICAL Koin gotcha: `viewModelOf`/`*Of` reflect over ALL ctor params and IGNORE Kotlin defaults →
  `NoDefinitionFoundException`. Any VM with a defaulted param MUST use explicit `viewModel { X(get(),…) }`
  in `DI.kt`. Regression: `ChatViewModelKoinGraphTest`. [notes.md]
- Interface fakes: give NEW interface methods a DEFAULT body so you don't touch ~10 fakes. DB-backed
  tests (real SQLDelight) go in `androidUnitTest` (JVM-only), not commonTest. [notes.md]
- Reuse file-private test fakes by adding `@Test`s to that same file (widen its `viewModel(...)` helper). [notes.md]

## Platform seams
- `sendEmail` / `openUrl` in `core/Platform.kt` (iOS `UIApplication.openURL`; Android `ACTION_SENDTO`/
  `ACTION_VIEW` + `FLAG_ACTIVITY_NEW_TASK`). iOS mailto needs `MailtoBuilder` percent-encoding. [notes.md]
- Language persistence: Android SharedPreferences via `provideAndroidContextForLanguage`; iOS NSUserDefaults. [notes.md]

## Feature areas
- Onboarding — first-run Claude key (KIM-252): `openUrl` seam, `AnthropicKeyStore`, VM-owned
  save/validate (`AnthropicKeySaveState`), `ChatViewModel.showClaudeKeyNudge`, reusable `KeyNudgeBanner`.
  "First run" = no key in Keychain, no flag. [notes.md]
- Onboarding — welcome + VC hard gate (KIM-334/309): revisitable-screen pattern, "seen once" flag. [notes.md]
- Chat sessions (KIM-285/286/287): ChatSession schema, `ChatSessionLogic` (pure rules), session switcher
  state, static `PlaceTagPill` (`CHAT_PLACE_TAG_PILL`). [notes.md]
- Chat data-awareness (KIM-321): reactive downloaded-months Flow, dynamic system prompt, VC fetch gating. [notes.md]
- Weather stations (KIM-258/259): `WeatherStation` table, `fetchAndPersistStations`, VC `stations` is a
  keyed OBJECT not array — always add a JSON-layer test. htmlToBase64 RFC-4648 fix. [notes.md]
- Repo cache (KIM-278): route ALL writes through `persistAndInvalidate`. [notes.md]
- Monthly summary (KIM-327/328/329/332): day-card wind = sustained not gust (accessor, not composable);
  chart highlight via `Set<Int>` from VM; per-cell download indicator. [notes.md]
- Hourly wind chart (KIM-303/305/306/370): pure logic in `place/HourlyWind.kt` (`yAxisTicks` default 5
  intervals, `toDisplayUnit(unit)`, `hourlyWindSlots`, `thresholdYFraction`, `criteriaShading`), render
  in `place/components/HourlyWindChart.kt`. Data stored in km/h → convert to `WindSpeedUnit` for display
  (chart takes `windSpeedUnit`, not a label). Grid lines drawn per-tick to stay aligned. Shading tiers
  are km/h-based and unit-independent (don't convert). a11y desc hardcodes "km/h" → keep it raw. [notes.md]

## Design system & localization
- `MaterialTheme.rewinds.*` tokens only, never hex literals; `IsobarBackground()` lowest layer;
  no `.background()` on page-level views. [notes.md]
- Localization: `AppStrings` data class (plurals = lambdas) + `LocalAppStrings` CompositionLocal; every
  new field needs BOTH EN + DE (LocalizationTest is hand-written, won't auto-catch a missing field). [notes.md]

## UI test layers
- Compose semantic (KIM-293): `androidInstrumentedTest/uitest/`, shared doubles in `commonTestFixtures`,
  build real VM from fakes + pass directly; merged-tree assertion gotcha. [notes.md]
- Maestro E2E (KIM-294): `.maestro/`, run on master/dispatch only; selectors are TestTags as `id:`. [notes.md]

## Reproducing iOS runtime crashes locally (no Gradle iOS build)
- xcodebuild + simctl boot/install/launch --console-pty for the Kotlin `Caused by:` chain; force a deep
  screen by temporarily changing default tab; simulate DB upgrade with hand-built app.db. [notes.md]
