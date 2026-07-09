Run the project's test suites and refresh the always-on "what ran last" summary artifact.

## Usage

`/run-full-tests` — runs unit tests (always) and instrumented tests (if an emulator is connected)

## What this does

1. Run `./run-full-tests.sh` from the repo root.
2. This runs `./gradlew :composeApp:testDebugUnitTest` (always), then
   `./gradlew :composeApp:connectedDebugAndroidTest` only if `adb devices` shows a booted emulator
   (skipped, not failed, otherwise).
3. Writes a timestamped summary (suite, pass/fail, test count, duration) to
   `docs/human/test-runs/latest.md` — **on every run, pass or fail**. This is the cheap, no-coverage
   counterpart to `/coverage`: no bytecode instrumentation, no HTML report, no trend history — just a
   glanceable "did anything break, how long did it take" artifact that exists even when green, so you
   don't have to wait for a failure notification to know something ran at all.
4. Report the summary table contents back (suite/result/tests/duration) and point to the file.

## Notes

- This only runs Gradle/shell tooling — no source files are touched, so it does not need to go through
  the Randy (developer) agent.
- `docs/human/test-runs/latest.md` is overwritten each run, not appended — it's a "latest state" file,
  not a history log. If a trend over time is ever wanted, that's a separate addition (see
  `docs/coverage/history.json` for what that pattern looks like for coverage).
- The scheduled Mon/Fri UI-test sweep (see `docs/agent/WORKFLOW.md`) calls this script directly rather
  than invoking Gradle itself, so the same artifact stays current from both manual and scheduled runs.

$ARGUMENTS
