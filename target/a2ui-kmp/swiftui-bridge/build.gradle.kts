plugins { kotlin("multiplatform") }

kotlin {
    jvm("desktop")
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework { baseName = "SwiftA2UI"; isStatic = true }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":a2ui-engine"))
            implementation(project(":a2ui-compat"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies { implementation(kotlin("test")); implementation(libs.kotlinx.coroutines.test) }
    }
}
