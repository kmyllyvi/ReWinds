# Coverage / JaCoCo (composeApp/build.gradle.kts)

- Coverage is opt-in: `./gradlew coverageReport -PenableCoverage=true`. Without the flag
  the `.exec` data isn't produced and reports/verification are meaningless.
- JaCoCo excludes match **compiled .class paths** under `tmp/kotlin-classes/debug` and
  `intermediates/javac/debug` — package-qualified, NOT source paths. A `*View.kt` source
  compiles to `<Name>ViewKt.class`; a top-level Compose file `Foo.kt` → `FooKt.class`.
- compose-resources generated accessors (String0/Plurals0/ActualResourceCollectors) live in
  package `rewinds/composeapp/generated/resources/` → exclude with
  `**/rewinds/composeapp/generated/resources/**`.
- Presentational Compose files that escape `*ViewKt.class`: excluded by suffix-family globs
  `*ModalKt`/`*SheetKt`/`*ScreenKt`/`*SwitcherKt`. Before excluding, VERIFY the file is pure
  `@Composable` (no class/object/ViewModel) — only exclude genuinely presentational files.
- Two coverage tasks share `coverageExcludes` + `coverageClassDirs` vals so the measured
  (`jacocoTestReport`) and enforced (`jacocoTestCoverageVerification`) denominators can't drift.
- Cleaned baseline as of KIM-324: 63.6% line / 38.4% branch. Floor set at 60% line / 35%
  branch (ratchet just below baseline — bump up as coverage grows, never silently down).

## CI coverage gates (.github/workflows/ci.yml, KIM-324)
- "Enforce coverage floor" step runs `jacocoTestCoverageVerification` after `coverageReport`.
- Patch coverage via `diff-cover` (pip), `pull_request` only, `--fail-under 70` on changed
  lines, consuming `composeApp/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`.
  Needs `fetch-depth: 0` on checkout for the diff base. diff-cover no-ops (exit 0) when a diff
  has no measured `.kt` lines (e.g. config/doc-only PRs).
- Local pip is PEP-668 externally-managed; test diff-cover in a venv, not system pip.
- NOT achievable in code: making the job a required status check needs GitHub branch
  protection (repo admin only).

## Writing tests for the data/network layer (KIM-325)
- Ktor clients (`NetworkService`, `AnthropicClient`) expose an `internal constructor(.., HttpClient)`
  test seam + a production secondary ctor that builds the platform client. Inject a `MockEngine`
  client in tests. Pure seam, no behaviour change. AnthropicClient got this seam in KIM-325.
- `MockEngine` (ktor-client-mock) and the JDBC SQLite driver are wired in **androidUnitTest only**,
  not commonTest. Tests needing either live in `composeApp/src/androidUnitTest/`.
- Real in-memory `AppDatabase`: `JdbcSqliteDriver(IN_MEMORY)` + `AppDatabase.Schema.create(driver)`
  + Day/Hour adapters using `core.listOfStringAdapter`; close driver in tearDown. See
  `core.SqlDelightDatabaseTest`. `ChatRepositoryImpl` tested directly against this.
- `WeatherRepositoryImpl` is fully fakeable in commonTest (both deps are interfaces). Only global
  is `WeatherApiKeyManager` — set a valid key in @BeforeTest, reset "" in @AfterTest.
- KIM-325 results: WeatherRepositoryImpl 0→87.6%, ChatRepositoryImpl 0→100%, AnthropicClient
  7.4→81.1%; overall 63.6→72.4% line, 38.4→45.0% branch.
