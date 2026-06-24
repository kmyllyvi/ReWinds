Generate the JaCoCo code coverage report (opt-in, not part of normal builds).

## Usage

`/coverage` — runs the unit test suite with coverage instrumentation and generates the HTML report

## What this does

1. Run `./coverage.command` (the standalone script at repo root, which runs `./gradlew coverageReport -PenableCoverage=true`)
2. This runs `testDebugUnitTest`, produces the JaCoCo XML report, then runs `generate_coverage_metrics.py` to render `docs/coverage/detailed.html`
3. Report the overall line/branch coverage percentage and grade from the script output, and point to `docs/coverage/detailed.html` for the detailed breakdown. The script also appends this run's metrics to `docs/coverage/history.json` (last 50 runs) and renders inline SVG trend charts in `detailed.html` showing line-coverage-% and total-lines over time — no network/JS needed, the charts render fully offline

## Notes

- Coverage is opt-in: `-PenableCoverage=true` enables bytecode instrumentation (`enableUnitTestCoverage`) so the `.exec` file is produced. Without it, `coverageReport` would run against an empty exec file.
- Requires Python 3 (for `generate_coverage_metrics.py`, at repo root).
- In sandboxed/remote environments without access to Google's Maven repo (`dl.google.com` / `maven.google.com`), the build fails at configuration time because the Android Gradle Plugin can't be resolved. This is a network policy issue, not a code issue — run locally or in CI instead.
- This only runs Gradle/Python tooling — no source files are touched, so it does not need to go through the Randy (developer) agent.
