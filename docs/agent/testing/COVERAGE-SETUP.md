# Code Coverage Setup (KIM-115)

**Status**: ✅ Complete
**Date Completed**: March 18, 2026
**Task**: KIM-115 - JaCoCo Coverage Plugin

## Summary

Implemented JaCoCo code coverage plugin with automated HTML report generation.

> **Note (June 2026):** The 92.2% / A+ figures below are the original March 2026
> snapshot and are stale — that report was generated before the metrics script parsed
> real JaCoCo XML and before presentational code was excluded from the denominator (see
> **Exclusions** below). Treat the percentages in this doc as historical; run
> `./coverage.command` (or `./gradlew :composeApp:coverageReport -PenableCoverage=true`)
> for the current numbers.

## Exclusions

The coverage denominator excludes two kinds of code (configured in the
`jacocoTestReport` task's `excludes` list in `composeApp/build.gradle.kts`):

1. **Generated / compiler scaffolding** — SQLDelight `db` classes, `R`/`BuildConfig`,
   serializers, and the synthetic `ComposableSingletons$` classes Compose emits.
2. **Presentational Compose code** — `*View.kt` screens (`*ViewKt.class`), `App.kt`, and
   the `components/` and `ui/` packages. Per
   [ARCHITECTURE-RULES.md](../ARCHITECTURE-RULES.md) the project keeps Views untested by
   design (all logic lives in ViewModels, MV*), so counting them would deflate the real
   coverage of the business logic.

ViewModels and `core/` (e.g. `Router.kt`'s tested `isShowingPlacesPush`,
`TabRoutingNavigator`) are **not** excluded — that logic is exactly what coverage measures.

## Quick Start

Generate the coverage report:
```bash
./gradlew :composeApp:coverageReport
```

View the report:
```bash
open docs/coverage/detailed.html
```

## Implementation Details

### JaCoCo Configuration
- **Version**: 0.8.11
- **Location**: `composeApp/build.gradle.kts` (lines 241-269)
- **Features**:
  - Line coverage measurement
  - Branch coverage measurement
  - Per-file coverage breakdown

### Gradle Task: `coverageReport`

**Command**: `./gradlew :composeApp:coverageReport`

**What it does**:
1. Runs `testDebugUnitTest` (137 unit tests)
2. Collects coverage data from JaCoCo execution file
3. Executes `generate_coverage_metrics.py` script
4. Generates HTML report with percentages and grades

**Output**: `docs/coverage/detailed.html`

### Python Script: `generate_coverage_metrics.py`

Parses JaCoCo execution data and generates professional HTML report with:
- Overall line/branch coverage percentages
- Per-file coverage breakdown
- Letter grades (A+, A, B, C, etc.)
- Visual progress bars
- Summary statistics

## Test Results

**Total Tests**: 137
**Pass Rate**: 100%
**Line Coverage**: 92.2%
**Branch Coverage**: 91.3%
**Overall Grade**: A+ (Excellent)

## Configuration Files

### `composeApp/build.gradle.kts`
```kotlin
jacoco {
    toolVersion = "0.8.11"
}

android {
    buildTypes.all {
        enableUnitTestCoverage = true
    }
}

tasks.register("coverageReport") {
    group = "verification"
    description = "Generate detailed code coverage report with percentages"
    dependsOn("testDebugUnitTest")

    doLast {
        val scriptPath = "${project.rootProject.projectDir}/generate_coverage_metrics.py"
        val scriptFile = File(scriptPath)
        if (scriptFile.exists()) {
            exec {
                commandLine("python3", scriptFile.absolutePath)
                workingDir(project.rootProject.projectDir)
            }
            println("\n✅ Coverage report generated!")
            println("   Open: docs/coverage/detailed.html")
        }
    }
}
```

## Report Location

- **Generated HTML**: `docs/coverage/detailed.html`
- **Execution Data**: `composeApp/build/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec`
- **Script**: `generate_coverage_metrics.py` (root project directory)

## Development Workflow

### Running Coverage Reports

```bash
# Generate fresh coverage report (runs tests + analysis)
./gradlew :composeApp:coverageReport

# View the report
open docs/coverage/detailed.html
```

### Continuous Integration

Coverage report generation is suitable for CI/CD pipelines:
```yaml
# Example for CI
./gradlew :composeApp:testDebugUnitTest  # Run tests
./gradlew :composeApp:coverageReport     # Generate report
```

## Notes

- Coverage task depends on `testDebugUnitTest`, so all tests must pass
- Requires Python 3 for HTML report generation
- Report is user-friendly with visual metrics and grades
- No manual configuration needed after initial setup

## Related Issues

- KIM-102: Extract Visual Crossing API key from source code
- KIM-116: Monthly stats text color in dark mode

## Commit History

- `cec11a1` - Refactor: Fix coverageReport task and remove duplicate JaCoCo config (KIM-115)
- `a43d7dc` - Feature: Add JaCoCo code coverage (KIM-115)
