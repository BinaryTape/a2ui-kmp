// The KMP build of material3-a2ui, from ../../source/material3-a2ui, for desktop and iOS.
// This directory is its one module. It is a separate build from
// ../a2ui-kmp because upstream uses Material 3 APIs that Compose Multiplatform has only from 1.13,
// which needs Kotlin 2.4, while the other five packages need no more than 1.12 and Kotlin 2.3. It uses
// those packages as ../a2ui-kmp publishes them to ../../build/repo (run that first; scripts/verify.sh
// does), and publishes there too.
pluginManagement {
    // The build logic both builds share: the a2ui.publish plugin and the module helpers.
    includeBuild("../build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        maven { url = uri("../../build/repo") }
        google()
        mavenCentral()
    }
    // The repository's one version catalog.
    versionCatalogs { create("libs") { from(files("../../gradle/libs.versions.toml")) } }
}
rootProject.name = "material3-a2ui"

