import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.sqlDelight)
    jacoco
}

repositories {
    google()
    mavenCentral()
}

// Whether to declare the Kotlin/Native iOS targets. Declaring them makes a plain
// `./gradlew build` on a Mac pull in the slow compileKotlinIos*/linkFrameworkIos* tasks,
// so we keep them off by default and only enable them when actually building for iOS:
//   * -PincludeAllTargets=true is passed explicitly (CI full builds), OR
//   * the build is invoked from Xcode/CocoaPods — detected via PLATFORM_NAME, which Xcode
//     sets for both the embedAndSign and the podspec syncFramework script phases. Detecting
//     the env (rather than only a flag) keeps this working even after the generated
//     composeApp.podspec is regenerated.
// NOTE: manual CocoaPods setup run outside Xcode (e.g.
//   ./gradlew :composeApp:generateDummyFramework) must pass -PincludeAllTargets=true.
val includeAllTargets: Boolean =
    project.findProperty("includeAllTargets")?.toString()?.toBoolean()
        ?: (System.getenv("PLATFORM_NAME") != null)

// kotlinCocoapods is intentionally NOT in plugins {} above. The plugins {} block is
// static — the plugin would always be applied and its tasks (podspec, generateDummyFramework)
// always created. Those tasks eagerly evaluate frameworkName from the first iOS framework
// binary; when iOS targets are absent (Android-only build / Android Studio sync) the
// collection is empty and Gradle crashes *before* any onlyIf guard is checked.
// Applying the plugin imperatively here means it only exists when iOS targets are present.
if (includeAllTargets) {
    apply(plugin = "org.jetbrains.kotlin.native.cocoapods")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    // iOS targets — only declared when includeAllTargets is true (see definition above).
    // Gating the declaration keeps the default Gradle/IDE build Android-only and fast.
    // iosX64 (Intel-Mac simulator) removed — Apple Silicon machines use iosSimulatorArm64.
    if (includeAllTargets) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
                binaryOption("bundleId", "com.km.rewinds.ReWinds")
                // Disable all memory-heavy optimizations
                freeCompilerArgs += listOf(
                    "-Xno-devirtualization",
                    "-Xno-objc-generics",
                    "-Xallocator=std"
                )
            }
        }
    }

    sourceSets {

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
            // Shared test doubles (e.g. FakeNavigator, KIM-292) reused by both the JVM unit
            // tests here and the Android Compose UI tests (KIM-293). Kept in one place so the
            // two test layers never drift. The same dir is added to the Android androidTest
            // source set below.
            kotlin.srcDir("src/commonTestFixtures/kotlin")
        }

        // JVM-only unit tests (run by :composeApp:testDebugUnitTest). Hosts DB-backed tests
        // that need a real SQLDelight driver — the JDBC in-memory driver is JVM-only, so it
        // cannot live in commonTest (KIM-321).
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.sqldelight.coroutines.extensions)
            }
        }

        commonMain.dependencies {
            // compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            // for async/API stuff
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.sqldelight.coroutines.extensions)
            // Networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.json)
            implementation(libs.ktor.client.cio)
            // logging
            implementation(libs.logger.napier)
            // implementation(libs.napier.antilog)

            // DI by koin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.multiplatform.compose)
            implementation(libs.koin.annotations)
            // Navigation https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html
            implementation(libs.koin.composeVM)
            // View model
            implementation(libs.lifecycle.viewmodel)
            // navigation compose
            implementation(libs.navigation.compose)

            // WebView for map embedding
            implementation("io.github.kevinnzou:compose-webview-multiplatform:1.9.40")
            // other
            implementation(libs.kotlinx.datetime)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.android.driver)
        }

        // JVM-only unit-test dependencies. testDebugUnitTest runs on the JVM, so these go on the
        // Android unit-test source set rather than commonTest — that keeps them off the iOS
        // (Kotlin/Native) test compilation, which has no JVM/JDBC artifacts for them. The tests
        // that use them (SqlDelightDatabaseTest, NetworkServiceTest) likewise live in
        // src/androidUnitTest/kotlin and run under testDebugUnitTest.
        androidUnitTest.dependencies {
            // In-memory SQLite (JDBC) driver — exercises SqlDelightDatabase merge/transaction logic.
            implementation(libs.sqldelight.sqlite.driver)
            // Ktor MockEngine — exercises NetworkService HTTP error mapping without real I/O.
            implementation(libs.ktor.client.mock)
        }

        // iOS source sets — gated together with the target declaration above. The `by getting`
        // accessors require the targets to exist, so these must only run when includeAllTargets
        // is true; otherwise plain Android-only builds would fail to resolve iosArm64Main etc.
        // https://stackoverflow.com/questions/72474284/unresolved-reference-iosmain-kotlin-multiplatform
        if (includeAllTargets) {
            val iosMain by creating {
                dependsOn(commonMain.get())

                dependencies {
                    implementation(libs.ktor.client.darwin)
                    implementation(libs.sqldelight.native.driver)
                }
            }

            val iosArm64Main by getting {
                dependsOn(iosMain)
            }
            val iosSimulatorArm64Main by getting {
                dependsOn(iosMain)
            }

            // iOS test source set
            val iosTest by creating {
                dependsOn(commonTest.get())
            }

            val iosArm64Test by getting { dependsOn(iosTest) }
            val iosSimulatorArm64Test by getting { dependsOn(iosTest) }
        }
    }

    // CocoaPods configuration — only reachable when the plugin was applied above.
    // cocoapods { } is a generated Kotlin DSL accessor that only exists when the plugin
    // is declared in plugins {}. Since we apply it imperatively, use getByName + cast.
    if (includeAllTargets) {
        @Suppress("UNCHECKED_CAST")
        val cpe = (this as org.gradle.api.plugins.ExtensionAware)
            .extensions.getByName("cocoapods")
                as org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
        cpe.apply {
            homepage = "https://github.com/kmyllyvi/ReWinds"
            summary = "The Weather History App"
            version = "1.0"
            ios.deploymentTarget = "15.3"
            podfile = project.file("../iosApp/Podfile")

            framework {
                baseName = "composeApp"
                isStatic = true
            }

            pod("sqlite3") {
                version = "3.51.1"
                extraOpts += listOf("-compiler-option", "-fmodules")
            }
        }
    }
}

android {
    namespace = "com.km.rewinds"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")
    sourceSets["debug"].manifest.srcFile("src/androidDebug/AndroidManifest.xml")
    sourceSets["androidTest"].java.srcDirs(
        "src/androidInstrumentedTest/kotlin",
        // Shared test doubles (FakeNavigator, KIM-292) — same dir commonTest uses, so the
        // Compose UI tests and the JVM unit tests assert against one navigator fake.
        "src/commonTestFixtures/kotlin"
    )

    defaultConfig {
        applicationId = "com.km.rewinds"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            // Pass API key from gradle.properties or CI environment to BuildConfig.
            // Always emit the field so Platform.android.kt can reference it; empty means
            // the runtime code will fall back to the placeholder.
            val apiKey = rootProject.findProperty("ANTHROPIC_API_KEY")?.toString()
                ?: System.getenv("ANTHROPIC_API_KEY") ?: ""
            buildConfigField("String", "ANTHROPIC_API_KEY", "\"$apiKey\"")
            val weatherKey = rootProject.findProperty("VISUAL_CROSSING_API_KEY")?.toString()
                ?: System.getenv("VISUAL_CROSSING_API_KEY") ?: ""
            buildConfigField("String", "VISUAL_CROSSING_API_KEY", "\"$weatherKey\"")
        }
        getByName("release") {
            isMinifyEnabled = false
            val apiKey = rootProject.findProperty("ANTHROPIC_API_KEY")?.toString()
                ?: System.getenv("ANTHROPIC_API_KEY") ?: ""
            buildConfigField("String", "ANTHROPIC_API_KEY", "\"$apiKey\"")
            val weatherKey = rootProject.findProperty("VISUAL_CROSSING_API_KEY")?.toString()
                ?: System.getenv("VISUAL_CROSSING_API_KEY") ?: ""
            buildConfigField("String", "VISUAL_CROSSING_API_KEY", "\"$weatherKey\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
        val composeVersion = libs.versions.compose.plugin.get()
        androidTestImplementation("org.jetbrains.compose.ui:ui-test-junit4:$composeVersion")
        androidTestImplementation(libs.androidx.test.junit)
        // Keep runner/core in the same release train as androidx.test.ext:junit (1.3.0 -> 1.7.x).
        // Mixing trains (e.g. runner 1.6.2 with ext:junit 1.3.0) makes AndroidJUnit4 fail to
        // instantiate the moment a test touches an androidx.test:core class (ApplicationProvider).
        androidTestImplementation("androidx.test:runner:1.7.0")
        androidTestImplementation("androidx.test:core:1.7.0")
    }
}

tasks.register("buildAndroidOnly") {
    description = "Build Android targets only (default; iOS targets excluded unless -PincludeAllTargets=true is set)."
    dependsOn("build")
}

tasks.register("buildWithIos") {
    description = "Build all targets including iOS. Requires -PincludeAllTargets=true on the command line."
    doFirst {
        check(includeAllTargets) {
            "buildWithIos requires iOS targets to be declared. Re-run with: ./gradlew buildWithIos -PincludeAllTargets=true"
        }
    }
    dependsOn("build")
}

sqldelight {
  databases {
    create("AppDatabase") {
      packageName.set("com.km.rewinds.db")
    }
  }
}

// ============================================
// JaCoCo Code Coverage Configuration (KIM-115)
// ============================================

jacoco {
    toolVersion = "0.8.11"
}

// Only instrument bytecode when coverage is explicitly requested.
// Normal builds and test runs skip the overhead.
// Usage: ./gradlew coverageReport -PenableCoverage=true
val enableCoverage: Boolean = project.findProperty("enableCoverage")?.toString()?.toBoolean() ?: false

android {
    buildTypes.all {
        enableUnitTestCoverage = enableCoverage
    }
}

// Two categories are excluded from the coverage denominator:
//
// 1. Generated / compiler scaffolding — code we never wrote, so counting it is noise:
//    SQLDelight db classes, R/BuildConfig, serializers, the synthetic ComposableSingletons$
//    lambda-holder classes the Compose compiler emits per file, and the compose-resources
//    generated string/plural accessor tables (String0/Plurals0/ActualResourceCollectors).
//
// 2. Presentational Compose code — Views and reusable UI components. Per
//    docs/agent/ARCHITECTURE-RULES.md the project deliberately keeps these untested:
//    all logic lives in ViewModels (MV*), and "Views are hard to test - keep them
//    simple so they don't need testing". Leaving their bytecode in the denominator
//    inflates it with code that is untested *by design*, hiding the real coverage of
//    the business logic. So we drop the *View.kt files (compiled to <Name>ViewKt.class),
//    the *Modal/*Sheet/*Screen/*Switcher.kt presentational files that escape the *View
//    naming, and the components/ packages.
//
// Globs match compiled .class paths under tmp/kotlin-classes/debug and
// intermediates/javac/debug — i.e. package-qualified paths, not source paths.
//
// NOT excluded: core/ (Router.kt carries the unit-tested isShowingPlacesPush; the
// TabRoutingNavigator is real, tested navigation logic) and every *ViewModel — those
// are exactly what coverage is meant to measure.
//
// Shared so jacocoTestReport (measure) and jacocoTestCoverageVerification (enforce floor)
// always use the same denominator and can never drift apart.
val coverageExcludes = listOf(
    // ── Generated / scaffolding ──
    "**/com/km/rewinds/db/**",   // SQLDelight-generated
    "**/*\$Companion*",
    "**/R.class", "**/R$*.class", "**/BuildConfig.*",
    "**/*ComposableSingletons*", "**/*\$\$serializer*",
    "**/rewinds/composeapp/generated/resources/**", // compose-resources String0/Plurals0/ActualResourceCollectors
    // ── Presentational: *View.kt screens (MV* — deliberately untested) ──
    "**/*ViewKt.class",          // ChatView, HomeView, SettingsView, PlaceSummaryView, MonthlyStatisticsView
    "**/*ModalKt.class",         // StationMapModal
    "**/*SheetKt.class",         // DayDetailSheet
    "**/*ScreenKt.class",        // VcKeyOnboardingScreen
    "**/*SwitcherKt.class",      // ChatSessionSwitcher
    "**/AppKt.class",            // App.kt — root Compose wiring, logic lives in AppViewModel
    // ── Presentational: reusable UI component packages ──
    "**/components/**",          // components/, place/components/, settings/components/
    "**/ui/**"                   // ui/components/, ui/theme/
)

// Class directories with the shared excludes applied, reused by both coverage tasks.
val coverageClassDirs = files(
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) { exclude(coverageExcludes) },
    fileTree(layout.buildDirectory.dir("intermediates/javac/debug")) { exclude(coverageExcludes) }
)

// Real JaCoCo XML report over the debug unit-test execution data. The Python script parses this
// XML — no hardcoded numbers. Requires -PenableCoverage=true so the .exec file is produced.
val jacocoTestReport = tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generate the JaCoCo XML coverage report from debug unit tests."
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    classDirectories.setFrom(coverageClassDirs)
    sourceDirectories.setFrom(files("src/commonMain/kotlin", "src/androidMain/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("**/testDebugUnitTest.exec", "**/*UnitTest*.exec")
        }
    )
}

tasks.register("coverageReport") {
    group = "verification"
    description = "Generate detailed code coverage report with percentages"
    dependsOn(jacocoTestReport)

    // Capture inputs at configuration time so the task is configuration-cache compatible.
    // Accessing Task.project or calling Project.exec at execution time is unsupported.
    val rootDir = project.rootProject.projectDir
    val scriptFile = File(rootDir, "generate_coverage_metrics.py")
    val xmlReport = layout.buildDirectory.file("reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
    val execOps = project.serviceOf<org.gradle.process.ExecOperations>()

    doLast {
        if (scriptFile.exists()) {
            execOps.exec {
                commandLine("python3", scriptFile.absolutePath, xmlReport.get().asFile.absolutePath)
                workingDir(rootDir)
            }
            println("\n✅ Coverage report generated!")
            println("   Open: docs/coverage/detailed.html")
        } else {
            println("⚠ generate_coverage_metrics.py not found")
            println("   Expected at: ${scriptFile.absolutePath}")
        }
    }
}

// Enforced global floor (ratchet, not stretch target). Uses the SAME execution data and the
// SAME class-directory excludes (coverageClassDirs) as jacocoTestReport, so the measured number
// and the enforced number can never diverge. Thresholds are set at/just-below the cleaned
// baseline (KIM-324) so this fails only on regression, not on today's code. Bump them upward as
// coverage improves — never downward without a deliberate decision. Requires -PenableCoverage=true.
tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Fail the build if line/branch coverage drops below the enforced floor."
    dependsOn("testDebugUnitTest")

    classDirectories.setFrom(coverageClassDirs)
    sourceDirectories.setFrom(files("src/commonMain/kotlin", "src/androidMain/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("**/testDebugUnitTest.exec", "**/*UnitTest*.exec")
        }
    )

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal() // cleaned baseline 63.6% (KIM-324); ~4pt regression buffer
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.35".toBigDecimal() // cleaned baseline 38.4% (KIM-324); ~3pt regression buffer
            }
        }
    }
}
