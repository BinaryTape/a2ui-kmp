plugins {
    // Versions come from settings.gradle.kts (-Pa2uiIntegration).
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The version under test: the one the repository builds (gradle/libs.versions.toml).
val a2uiKmpVersion = libs.versions.a2uiKmp.get()
// "all" adds material3-a2ui and its tests (settings.gradle.kts).
val withMaterial3 = providers.gradleProperty("a2uiIntegration").get() == "all"
// Separate outputs per run: the two use different Kotlin compilers.
layout.buildDirectory.set(layout.projectDirectory.dir("build/${providers.gradleProperty("a2uiIntegration").get()}"))

kotlin {
    android {
        namespace = "ai.littlepress.a2ui.integration"
        // What the official artifacts require (../../gradle/libs.versions.toml).
        compileSdk {
            version = release(libs.versions.androidCompileSdk.get().toInt()) {
                minorApiLevel = libs.versions.androidCompileSdkMinor.get().toInt()
            }
        }
        minSdk = libs.versions.androidMinSdk.get().toInt()
        // One string per material3-a2ui resource directory (src/androidMain/res, generated), so the
        // device tests can ask Android which directory serves a locale.
        androidResources { enable = true }
        withHostTest {}
        // Share commonTest with the device tests too.
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    jvm("desktop")
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("ai.littlepress.a2ui:a2ui-model:$a2uiKmpVersion")
            implementation("ai.littlepress.a2ui:a2ui-engine:$a2uiKmpVersion")
            implementation("ai.littlepress.a2ui:compose-runtime:$a2uiKmpVersion")
            implementation("ai.littlepress.a2ui:compose-ui:$a2uiKmpVersion")
            if (withMaterial3) implementation("ai.littlepress.a2ui:material3-a2ui:$a2uiKmpVersion")
        }
        if (withMaterial3) commonTest { kotlin.srcDir("src/material3Test/kotlin") }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("ai.littlepress.a2ui:compose-ui-testing:$a2uiKmpVersion")
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.serialization.json)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.junit)
        }
    }
}
