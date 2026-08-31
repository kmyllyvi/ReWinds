# iOS / Kotlin-Native build notes

## There are two sets of framework binaries — configure both
`iosTarget.binaries.framework { }` creates `debugFramework` / `releaseFramework`. The CocoaPods
plugin *separately* adds `podDebugFramework` / `podReleaseFramework`. **Xcode builds the pod ones**:
the "Compile Kotlin Framework" phase runs `embedAndSignAppleFrameworkForXcode`, which resolves to
the pod framework (that is also why `kotlin.apple.deprecated.allowUsingEmbedAndSignWithCocoaPods
Dependencies=true` is in gradle.properties).

So anything configured inside `binaries.framework { }` never reaches the shipped artifact. Use:

```kotlin
iosTarget.binaries.withType<Framework>().configureEach { ... }  // needs import ...mpp.Framework
```

Tell them apart by baseName: pod framework is `composeApp` (lowercase, from `cocoapods { framework
{ } }`), the other is `ComposeApp`. A build log saying `composeApp` is the pod one.
Confirm task names with `./gradlew -PincludeAllTargets=true :composeApp:tasks --all | grep link`.

## Compiler heap: `kotlin.native.jvmArgs`, not `org.gradle.jvmargs`
The Kotlin/Native compiler runs as its own JVM process (`konanc`), so the Gradle daemon heap never
applies to it. `kotlin.native.jvmArgs=-Xmx6g` is what reaches it — verified honored even with
`kotlin.native.disableCompilerDaemon=true`. Proof technique: run with `--info` and grep the spawned
`.../bin/java ... konanc` command line for `-Xmx`. That was the whole fix for the device arm64
Archive OOM in `DevirtualizationAnalysis` (KIM-420); 6 GB links iosArm64 release with room to spare.

Failure signature when the heap is too small: task dies after ~2 min with exit **137** (SIGKILL),
sometimes with `OutOfMemoryError: Java heap space` in `DevirtualizationAnalysis.mergeEdges`.

## Compiler flags that silently do nothing
- `-Xno-devirtualization` — **rejected by Kotlin 2.3.10**: `w: Flag is not supported by this version
  of the compiler`. It is a warning, not an error, so it sat in the build file for months doing
  nothing while the docs claimed devirtualization was off. Always grep a link log for "Flag is not
  supported" after adding anything to `freeCompilerArgs`.
- `kotlin.native.disableDevirtualization` was never a real Gradle property (removed in KIM-420).
  `kotlin.native.disable.lto`, still present, looks like the same species — unverified.
- `-Xno-objc-generics` and `-Xallocator=std` change the Obj-C API surface / runtime allocator, not
  compiler memory. Extending them to the *pod* framework is a product-visible decision → needs human
  sign-off, don't reach for them to fix a build.

## Reproducing an Archive failure without Xcode
`./gradlew -PincludeAllTargets=true :composeApp:linkPodReleaseFrameworkIosArm64` runs exactly the
compile that Archive's Kotlin phase drives — no signing needed, ~11 min when it succeeds. Good
enough to prove a build-tooling fix; a real `Product → Archive` still owns final sign-off, and
Kimmo runs that himself (Marcy can't verify a device archive from a diff).

## Target gating
iOS targets are only declared when `-PincludeAllTargets=true` **or** the `PLATFORM_NAME` env var is
set (Xcode sets it). Plain Gradle commands are Android-only and `iosArm64Main` etc. do not resolve.

## Renaming the Xcode target (done 2026-08-31: `iosApp` → `ReWinds`)
The Xcode **target** name is separate from `PRODUCT_NAME`. `PRODUCT_NAME=${APP_NAME}` (→ `ReWinds`
via `Configuration/Config.xcconfig`) already gave us `ReWinds.app` and the right Home-screen name,
but Organizer/archives are named after the **scheme**, which is named after the target — that is why
archives read "iosApp" long after the display name was correct.

CocoaPods derives its whole integration from the target name, so a rename is *not* two lines:
1. `project.pbxproj` — `name`/`productName` on the PBXNativeTarget, plus the `/* … */` comments on
   the target, its `buildConfigurationList`, and the `targets = ( )` entry.
2. `Podfile` — `target 'iosApp' do` → `target 'ReWinds' do`. Skipping this makes `pod install` fail
   with "unable to find a target named …".
3. `git mv` the shared scheme to `<NewName>.xcscheme` and set `BlueprintName` (both occurrences —
   BuildAction and LaunchAction). Leave `ReferencedContainer = "container:iosApp.xcodeproj"` and
   `BuildableName` alone; those track the *project* and the *product*, not the target.
4. Re-run `pod install`. It rewrites the `Pods-<Target>` xcconfig refs, framework ref and the three
   `[CP]` script phases — do **not** hand-edit those.
5. **`pod install` leaves the old `Pods-<OldName>.{debug,release}.xcconfig` PBXFileReference entries
   and their Pods-group children behind as dangling refs** (the files are gone from disk). Delete
   those 4 lines by hand or Xcode shows red missing files. Verify: `grep -n "Pods-<OldName>"`.

Do **not** rename the `iosApp/` folder or `iosApp.xcworkspace` — CocoaPods keys off the folder path
and `PBXProject "iosApp"` legitimately stays.

Verify with `plutil -lint iosApp/iosApp.xcodeproj/project.pbxproj` and
`xcodebuild -workspace iosApp/iosApp.xcworkspace -list` (the workspace CLI is fine — CLAUDE.md only
bans *Gradle* iOS tasks and using `.xcodeproj`). Expect leftover `iosApp`/`Unnamed` schemes in the
listing: those come from gitignored `xcuserdata` and are local-only noise.

Grep the repo for `-scheme iosApp` afterwards — the old scheme name is hardcoded in
`.github/workflows/e2e-smoke.yml`, `.maestro/README.md`, and several `docs/` files.

## Editing the Xcode project while Xcode is open
Xcode (often running for days) holds the project in memory and live-syncs edits — it rewrote
`xcuserdata` schemes seconds after the shared scheme was renamed. Checksum `project.pbxproj` before
and after any `pod install` / external edit and re-grep, rather than assuming the write stuck.

## Stripping alpha from the App Store icon (no ImageMagick/PIL on this machine)
App Store validation rejects a 1024×1024 `appstore.png` with an alpha channel (PNG colour type 6).
Neither `magick` nor PIL is installed here, and `sips` will not reliably drop alpha — but plain
`zlib` + `struct` in Python decodes/re-encodes a non-interlaced 8-bit PNG in ~60 lines.
The icon is a **squircle with genuinely transparent corners** (~4.3% of pixels, alpha 0), so
"flatten onto white" would have punched a white frame into it. Correct fix: extend the icon's own
edge colour per row into the corners, then composite. Verify by asserting every originally-opaque
pixel is bit-for-bit unchanged, then `sips -g hasAlpha` → `no`.

## Session hygiene
Long Gradle runs exceed the 600 s Bash cap and get backgrounded; a session/context reset kills them
and can roll back unsaved working-tree edits. Write memory updates as small targeted edits and
re-check they survived.
