// A separate build that uses the a2ui-kmp artifacts only as published to ../../build/repo, the way an
// app would, never our source projects. What a customer gets is the published artifacts, and some of
// what they contain exists only there, so only a build like this one can check it:
//   - the Android variants that point to the official androidx.a2ui artifacts, which our publishing
//     adds to the Gradle module metadata (Android here must resolve Google's artifacts);
//   - the Kotlin version an iOS app needs, which is the klib ABI version written into the artifacts;
//   - the dependency versions passed on to the app (the README's "Requirements" table is measured here);
//   - that every platform is published with the right coordinates.
// Depending on source projects instead would skip all of these: if this build is ever part of a
// composite build, Gradle's substitution of included projects for our artifacts must stay off here.
// Two runs (scripts/verify.sh), chosen with -Pa2uiIntegration, each with the Kotlin version a user
// of those packages needs at least (../../gradle/libs.versions.toml):
//   minimum  the five packages of ../../target/a2ui-kmp, with its Kotlin (no material3-a2ui)
//   all      every package, with the Kotlin of ../../target/material3-a2ui-kmp
pluginManagement {
    val versions = file("../../gradle/libs.versions.toml").readLines()
    fun version(key: String) = versions.first { it.startsWith("$key = ") }.substringAfter('"').substringBefore('"')
    val kotlinVersion = when (providers.gradleProperty("a2uiIntegration").orNull) {
        "minimum" -> version("kotlin")
        "all" -> version("kotlinMaterial3")
        else -> error("Pass -Pa2uiIntegration=minimum or -Pa2uiIntegration=all (scripts/verify.sh does)")
    }
    plugins {
        kotlin("multiplatform") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.compose") version kotlinVersion
        id("com.android.kotlin.multiplatform.library") version version("androidGradlePlugin")
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        // ai.littlepress.a2ui comes from the artifacts just built only: a missing one fails.
        exclusiveContent {
            forRepository { maven { url = uri("../../build/repo") } }
            filter { includeGroup("ai.littlepress.a2ui") }
        }
        google()
        mavenCentral()
    }
    // The repository's one version catalog.
    versionCatalogs { create("libs") { from(files("../../gradle/libs.versions.toml")) } }
}
rootProject.name = "a2ui-kmp-integration"
