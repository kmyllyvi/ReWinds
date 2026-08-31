# Randy (developer) — ReWinds memory

Project facts live in `CLAUDE.md`, `docs/agent/ARCHITECTURE-RULES.md`, `docs/agent/WORKFLOW.md`.
This file holds only what those don't say, or what they say misleadingly.

## Topic files
- [iOS / Kotlin-Native build](ios-native-build.md) — pod vs non-pod framework binaries, compiler
  heap, flags that silently do nothing, repro commands and timings. Also: the full Xcode
  target/scheme rename checklist (CocoaPods leaves dangling refs), and stripping alpha from the
  App Store icon with no ImageMagick/PIL on this machine.

## Gotchas that cost time
- **`gradle.properties` has the git assume-unchanged bit set** (`git ls-files -v` → lowercase `h`).
  Edits to it are invisible to `git status`/`git diff` and will silently not be committed. To
  commit: `git update-index --no-assume-unchanged gradle.properties`, stage, commit, then set the
  bit back so the local state is left as found. Check `git ls-files -v | grep -E "^[a-z]"` before
  trusting a clean `git status` on any build file. (Found KIM-420.)
- The working tree on `develop` often carries unrelated local modifications (docs, test-run
  artifacts). Stage explicit paths, never `git add -A`.
- **Xcode is usually running on this machine** and live-syncs `project.pbxproj` / `xcuserdata`.
  Re-verify project-file edits after any `pod install` instead of assuming they stuck.
- No ImageMagick and no PIL here. For simple PNG surgery, pure `zlib`+`struct` Python beats
  fighting `sips` (which will not reliably drop an alpha channel).
- CI job "failure" in ~2-3s with annotation "job was not started because recent account payments
  have failed / spending limit" = GitHub Actions BILLING block, not code. Hits every job. Don't spend
  fix attempts — flag `needs-human`, assign Kimmo; local build/test is the real signal.

## Verification timings (this machine, 34 GB)
- `./gradlew buildAndroidOnly :composeApp:testDebugUnitTest` — ~4 min.
- `./gradlew -PincludeAllTargets=true :composeApp:linkPodReleaseFrameworkIosArm64` — ~11 min when
  it succeeds. Budget for it; the Bash tool's 600 s cap will background the call.
