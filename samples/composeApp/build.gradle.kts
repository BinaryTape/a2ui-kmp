plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm("desktop")
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        // The framework the Xcode project in ../iosApp links (embedAndSignAppleFrameworkForXcode).
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.a2ui.material3.kmp)
            implementation(libs.a2ui.compose.ui.kmp)
            implementation(libs.compose.material3)
            implementation(libs.kotlinx.coroutines.core)
        }
        named("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
        named("desktopTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
        }
    }
}

compose.desktop {
    application {
        mainClass = "sample.MainKt"
    }
}
