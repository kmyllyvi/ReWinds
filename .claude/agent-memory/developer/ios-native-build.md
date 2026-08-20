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

## Session hygiene
Long Gradle runs exceed the 600 s Bash cap and get backgrounded; a session/context reset kills them
and can roll back unsaved working-tree edits. Write memory updates as small targeted edits and
re-check they survived.
