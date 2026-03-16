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
    alias(libs.plugins.kotlinCocoapods)
}

repositories {
    google()
    mavenCentral()
}

val includeAllTargets: Boolean = project.findProperty("includeAllTargets")?.toString()?.toBoolean() ?: true

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    // add ios targets
    listOf(
        iosArm64(),
        iosX64(),
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

    sourceSets {

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
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

        if (includeAllTargets) {

            // https://stackoverflow.com/questions/72474284/unresolved-reference-iosmain-kotlin-multiplatform
            val iosMain by creating {
                dependsOn(commonMain.get())

                dependencies {
                    implementation(libs.ktor.client.darwin)
                    implementation(libs.sqldelight.native.driver)
                }
            }

            // Excluding watchOS (maybe there's a better way)
            val iosArm64Main by getting {
                dependsOn(iosMain)
            }
            val iosX64Main by getting {
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
            val iosX64Test by getting { dependsOn(iosTest) }
            val iosSimulatorArm64Test by getting { dependsOn(iosTest) }
        }
    }

    cocoapods {
        homepage = "https://github.com/kmyllyvi/ReWinds"
        summary = "The Weather History App"
        version = "1.0"
        ios.deploymentTarget = "15.3"
        podfile = project.file("../iosApp/Podfile")

        framework {
            baseName = "composeApp"
            compilerOptions.optIn.add("-Xbinary=bundleId=com.km.rewinds.ReWinds")
            isStatic = true
        }

        pod("sqlite3") {
            version = "3.51.1"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
    }
}

android {
    namespace = "com.km.rewinds"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.km.rewinds"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
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
    }
}

tasks.register("buildAndroidOnly") {
    doFirst {
        project.extensions.extraProperties["includeAllTargets"] = false
    }
    finalizedBy("build")
}

tasks.register("buildWithIos") {
    doFirst {
        project.extensions.extraProperties["includeAllTargets"] = true
    }
    finalizedBy("build")
}

sqldelight {
  databases {
    create("AppDatabase") {
      packageName.set("com.km.rewinds.db")
    }
  }
}
