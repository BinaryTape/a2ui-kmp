// The a2ui.publish plugin, applied by every published module in target/a2ui-kmp and
// target/material3-a2ui-kmp. Publishes to build/repo (at the repository root) for local verification.
//
// A module that sets `a2uiPublishing { androidArtifact = "<group>:<module>" }` gets Android variants
// that depend on the official AndroidX artifact (a2uiUpstream in the version catalog),
// plus the compat module for the JDK types in the common API.
// This is the layout JetBrains publishes for org.jetbrains.androidx.lifecycle. The official Android
// artifact therefore has to expose the same public API as our common code, which is the case while
// the overlays only change imports.
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

apply(plugin = "maven-publish")
apply(plugin = "com.vanniktech.maven.publish")

group = "ai.littlepress.a2ui"
// This release's versions, from the repository's version catalog (gradle/libs.versions.toml).
val versions = the<VersionCatalogsExtension>().named("libs")
fun catalogVersion(name: String) = versions.findVersion(name).get().requiredVersion
version = catalogVersion("a2uiKmp")

configure<PublishingExtension> {
    repositories {
        maven {
            name = "local"
            // One local repository for both builds, at the repository root: build/repo.
            url = rootDir.resolve("../../build/repo").canonicalFile.toURI()
        }
    }
}

// The release workflow supplies Maven Central credentials and the signing key as Gradle properties.
// publishAllPublicationsToLocalRepository writes artifacts to build/repo for local verification.
// The public repository of this project, for the POM.
val repoUrl = "https://github.com/BinaryTape/a2ui-kmp"
val upstreamRelease = catalogVersion("a2uiUpstream")
configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    publishToMavenCentral(automaticRelease = false)
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates("ai.littlepress.a2ui", project.name, version.toString())
    pom {
        name.set("A2UI for Kotlin Multiplatform: ${project.name}")
        description.set(
            "Unofficial Kotlin Multiplatform build of AndroidX A2UI $upstreamRelease (desktop JVM and iOS); " +
                "on Android it resolves to the official androidx.a2ui artifacts. Sponsored by LittlePress.AI."
        )
        url.set(repoUrl)
        inceptionYear.set("2026")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("binarytape")
                name.set("BinaryTape")
                url.set("https://github.com/BinaryTape")
            }
        }
        scm {
            url.set(repoUrl)
            connection.set("scm:git:$repoUrl.git")
            developerConnection.set("scm:git:$repoUrl.git")
        }
    }
}

val a2uiPublishing = extensions.create<A2uiPublishingExtension>("a2uiPublishing")
tasks.withType<GenerateModuleMetadata>().configureEach {
    if (name != "generateMetadataFileForKotlinMultiplatformPublication") return@configureEach
    val androidArtifact = a2uiPublishing.androidArtifact
    val compat = mapOf("group" to "ai.littlepress.a2ui", "module" to "a2ui-compat",
        "version" to mapOf("requires" to version.toString()))
    doLast {
        val (androidGroup, androidModule) = androidArtifact.orNull?.split(":") ?: return@doLast
        val official = mapOf("group" to androidGroup, "module" to androidModule,
            "version" to mapOf("requires" to upstreamRelease))
        val file = outputFile.get().asFile
        @Suppress("UNCHECKED_CAST")
        val module = JsonSlurper().parse(file) as MutableMap<String, Any>
        @Suppress("UNCHECKED_CAST")
        val variants = module["variants"] as MutableList<Any>
        for (usage in listOf("java-api", "java-runtime")) {
            variants += mapOf(
                "name" to (if (usage == "java-api") "androidApiElements-published" else "androidRuntimeElements-published"),
                "attributes" to mapOf(
                    "org.gradle.category" to "library",
                    "org.gradle.jvm.environment" to "android",
                    "org.gradle.usage" to usage,
                    "org.jetbrains.kotlin.platform.type" to "androidJvm",
                ),
                "dependencies" to listOf(official, compat),
            )
        }
        file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(module)))
    }
}
