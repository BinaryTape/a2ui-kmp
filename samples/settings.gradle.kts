// A Compose Multiplatform app (desktop and iOS) that shows A2UI surfaces, with Google's Material 3
// components and with its own, depending on a2ui-kmp the way any app does: through its published
// artifacts.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        // Only for building against a local build of a2ui-kmp (README.md): -Pa2uiRepo=<repository>,
        // or ORG_GRADLE_PROJECT_a2uiRepo for the Xcode build. Then ai.littlepress.a2ui comes from
        // that repository only, so a missing package fails instead of coming from Maven Central.
        providers.gradleProperty("a2uiRepo").orNull?.let { repo ->
            exclusiveContent {
                forRepository { maven { url = uri(repo) } }
                filter { includeGroup("ai.littlepress.a2ui") }
            }
        }
        google()
        mavenCentral()
    }
}
rootProject.name = "a2ui-samples"
include(":composeApp")
