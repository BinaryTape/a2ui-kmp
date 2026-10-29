// material3-a2ui. The other packages come from their published artifacts (see settings.gradle.kts),
// in the version this build publishes.
plugins {
    // This build's plugin versions (Kotlin 2.4), also for ../build-logic.
    alias(libs.plugins.kotlinMaterial3.multiplatform)
    alias(libs.plugins.kotlinMaterial3.compose)
    alias(libs.plugins.mavenPublish) apply false
    id("a2ui.publish")
}

val a2ui = "ai.littlepress.a2ui"
val a2uiVersion = libs.versions.a2uiKmp.get()

a2uiPublishing {
    androidArtifact = "androidx.compose.material3:material3-a2ui"
}

kotlin {
    a2uiTargets()
    upstreamSources(this, "material3-a2ui")
    upstreamUiTestsOnDesktop(this, "material3-a2ui")
    // R.string and the strings of every resource locale, generated from upstream's res/.
    sourceSets.getByName("commonMain").kotlin.srcDir("resources-generated")
    compilerOptions {
        // DatePickerDialog is stable in Google's material3 but experimental in JetBrains' build of it
        // for desktop and iOS.
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
    }

    sourceSets {
        commonMain {
            dependencies {
                api("$a2ui:a2ui-model:$a2uiVersion")
                api("$a2ui:compose-runtime:$a2uiVersion")
                api("$a2ui:compose-ui:$a2uiVersion")
                api(libs.material3.compose.animation)
                implementation("$a2ui:a2ui-engine:$a2uiVersion")
                implementation(libs.material3.compose.foundation)
                implementation(libs.material3.compose.material3)
                implementation(libs.material3.compose.runtime)
                implementation(libs.material3.compose.ui)
            }
        }
        named("desktopTest") {
            dependencies {
                implementation("$a2ui:compose-ui-testing:$a2uiVersion")
                implementation(libs.material3.compose.ui.test)
                implementation(desktopRuntime(libs.versions.composeMultiplatformMaterial3.get()))
                implementation(kotlin("test"))
                implementation(libs.junit)
                implementation(libs.truth)
            }
        }
    }
}

tasks.named<Test>("desktopTest") {
    // DateTimeInput's tests expect the text java.text.DateFormat gives on the test's platform. The
    // port formats as Android does; for en-US short times the JDK writes U+202F where Android writes a
    // space, so the tests run in en-GB, where the JDK and Android give the same text.
    systemProperty("user.language", "en")
    systemProperty("user.country", "GB")
    filter {
        // StateRestorationTester is not implemented in Compose Multiplatform on desktop.
        listOf(
            "MaterialA2uiBasicCatalogV1DateTimeInputTest.datePickerDialog_stateRestoration_dialogRemainsOpen",
            "MaterialA2uiBasicCatalogV1DateTimeInputTest.timePickerDialog_stateRestoration_dialogRemainsOpen",
            "MaterialA2uiBasicCatalogV1ModalTest.openModal_survivesStateRestoration",
            "MaterialA2uiBasicCatalogV1TabsTest.tabs_stateRestoration_preservesSelectedTab",
        ).forEach { excludeTestsMatching("androidx.compose.material3.a2ui.catalog.$it") }
    }
}
