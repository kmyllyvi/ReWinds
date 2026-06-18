# ReWinds Development Guide

Compose Multiplatform app (iOS + Android).

@docs/ARCHITECTURE-RULES.md

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
**Claude must never edit source files directly.** Any task touching the codebase
(`composeApp/src/`, build files, CI workflows) is handed to the **Randy (developer)** agent.
Exceptions Claude may edit directly: this CLAUDE.md, docs, and agent files (`.claude/agents/`).

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
- Device ARM64 builds OOM (need 8GB+ or architectural changes).
- XCFramework builds have KLIB resolver conflicts.
