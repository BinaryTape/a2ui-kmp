plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("a2ui.publish")
}

a2uiPublishing {
    androidArtifact = "androidx.a2ui.compose:compose-runtime"
}

kotlin {
    a2uiTargets()
    upstreamSources(this, "a2ui/compose/compose-runtime")

    sourceSets {
        commonMain {
            dependencies {
                api(project(":a2ui-model"))
                api(libs.androidx.collection)
                api(libs.compose.runtime)
                implementation(project(":a2ui-engine"))
                implementation(libs.androidx.annotation)
            }
        }
        named("desktopTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit)
                implementation(libs.truth)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
