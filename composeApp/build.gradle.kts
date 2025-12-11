import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    // doesn't work until this issue fixed (ksp)
    // https://issuetracker.google.com/issues/343408758
    // alias(libs.plugins.roomDb)
    alias(libs.plugins.realm)
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
        }
    }
    // Room / ksp
    sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/metadata")
    }

    sourceSets {

        commonMain.dependencies {
            // compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            // for async/API stuff
            implementation(libs.kotlinx.coroutines.core)
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

            // Database - Realm
            implementation(libs.realm.base)

            // Database - Room
//            implementation(libs.room.runtime)
//            implementation(libs.sqlite.bundled)
            // implementation(libs.room.ktx)
            // implementation(libs.room.compiler)


            // other
            implementation(libs.kotlinx.datetime)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.androidx.compose)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.android)
        }

        if (includeAllTargets) {

            // https://stackoverflow.com/questions/72474284/unresolved-reference-iosmain-kotlin-multiplatform
            val iosMain by creating {
                dependsOn(commonMain.get())

                dependencies {
                    implementation(libs.ktor.client.darwin)
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
        }
        // workaround
        // https://stackoverflow.com/questions/78133592/kmm-project-build-error-testclasses-not-found-in-project-shared
        task("testClasses")
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
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
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

// Room set up
// https://issuetracker.google.com/issues/343408758
//room {
//   schemaDirectory("$projectDir/schemas")
//}

//dependencies {
//    ksp(libs.room.compiler)
//}

// hack for Room / ksp
//tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach {
//    if (name != "kspCommonMainKotlinMetadata") {
//        dependsOn("kspCommonMainKotlinMetadata")
//    }
//}
