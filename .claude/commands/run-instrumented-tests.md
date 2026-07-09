Run the Layer 1 Compose semantic instrumented test suite (`androidInstrumentedTest`) locally against a connected/booted Android emulator.

This is distinct from `/run-ui-tests` (Maestro E2E, Layer 2) — this suite drives real Views with
in-memory fakes (`FakeNavigator`, fake repositories) via Compose's semantic test API, no network,
no real DB, no secrets.

## Usage

`/run-instrumented-tests` — runs the full `androidInstrumentedTest` source set

## What this does

1. Check that an Android emulator is running (`adb devices` — must list at least one `device`, not
   just `offline`/`unauthorized`). If none is running, tell the user to boot one (e.g. via Android
   Studio's Device Manager or `emulator -avd <name>`) and stop — do not attempt to launch an emulator
   yourself.
2. Run `./gradlew :composeApp:connectedDebugAndroidTest -PincludeAllTargets=false --no-daemon --stacktrace`.
3. Report pass/fail per test class. If any test fails, print the relevant JUnit XML/HTML report
   under `composeApp/build/outputs/androidTest-results/connected/` or
   `composeApp/build/reports/androidTests/connected/` so the failure is visible without needing to
   open a browser.

## Notes

- This suite used to run on every PR in GitHub Actions CI (`android-instrumented` job in
  `.github/workflows/ci.yml`) but was moved to manual/scheduled-only to save Actions minutes — see
  `docs/agent/WORKFLOW.md` for the automation this command now backs (a Monday/Friday scheduled
  local run).
- Do not attempt to build or install an APK as part of this command beyond what Gradle's
  `connectedDebugAndroidTest` task does itself — it builds and installs the debug test APK
  automatically.
- If this is run as part of the scheduled routine, report results back per that routine's
  instructions rather than assuming an interactive user is watching.

$ARGUMENTS
