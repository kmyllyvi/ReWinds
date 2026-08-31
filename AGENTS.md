# ReWinds Development Guide

Compose Multiplatform app (iOS + Android).

## Architecture rules (summary)
Full detail + code examples: `docs/agent/ARCHITECTURE-RULES.md` — read it before writing or reviewing code.
- **MV\* pattern**: all logic/state lives in ViewModels. Views are pure render — `collectAsState()` + call VM methods. No `remember { mutableStateOf(...) }`, business logic, or DB/network calls in Composables.
- **KMP-safe commonMain**: no `String.format` / `"%.1f".format()` (JVM-only). Use `core.utils.formatDecimal()`.

## Build

**Android:** `./gradlew buildAndroidOnly`

**iOS:** Use Xcode, not Gradle — `open iosApp/iosApp.xcworkspace` then Cmd+R.
Always the `.xcworkspace`, never `.xcodeproj` (CocoaPods needs it). Gradle iOS tasks
(`linkPodReleaseFrameworkIosSimulatorArm64`, etc.) are unreliable — they only compile the
Kotlin framework, not the app.

**Both (CI only):** `./gradlew build -PincludeAllTargets=true --no-daemon`.
Plain `./gradlew build` is Android-only; iOS targets need `-PincludeAllTargets=true`.

**Coverage (opt-in):** `./gradlew coverageReport -PenableCoverage=true`

## Config that bites
- Gradle heap is 8GB (`gradle.properties`).
- iOS targets are `iosArm64` + `iosSimulatorArm64` only — do not add Intel/`iosX64`.
- Database is SQLDelight + sqlite3; on iOS, CocoaPods manages the sqlite3 dependency.
- Do **not** add manual `-framework` linker flags for Pod-managed frameworks. The podspec's
  `vendored_frameworks` handles linkage; a manual `-framework ComposeApp` in `OTHER_LDFLAGS`
  causes duplicate-symbol errors. Pod-managed frameworks should have only `$(inherited)`.

## Workflow — code changes go to Randy, always
**Codex must never edit source files directly.** Any task touching the codebase
(`composeApp/src/`, build files, CI workflows) is handed to the **Randy (developer)** agent.
Exceptions Codex may edit directly: this AGENTS.md, docs, and agent files (`.Codex/agents/`).

Full lane + handover protocol: `docs/agent/WORKFLOW.md`.

### Agent roster
| Agent | Role |
|-------|------|
| `Randy (developer)` | Implementation, bug fixes, commits, PR + Linear handover |
| `Marcy (code-reviewer)` | Review against AC/DoD/MV* rules (Randy hands off in-session after opening the PR) |
| `Shirley (po)` | Spec + acceptance criteria for Backlog items |
| `Armin (codebase-architect)` | Architecture exploration, technical planning |
| `Seppo (qa-test-agent)` | Testing (manual dispatch) |
| `Mr.T (ux-ui-reviewer)` | UI/UX review (manual dispatch) |
| `Phill (doc-agent)` | Docs update after merge (auto-triggered) |

## Known issues
- Gradle iOS build tasks are unreliable — use Xcode.
- ~~Device ARM64 builds OOM~~ — fixed (KIM-420). The Kotlin/Native compiler runs in its own
  process, so `org.gradle.jvmargs` never applied to it; `kotlin.native.jvmArgs=-Xmx6g` gives it
  the heap the device release build needs. Lower that value and Archive builds OOM again.
- XCFramework builds have KLIB resolver conflicts.
