plugins {
    kotlin("multiplatform")
    id("a2ui.publish")
}

// The small part of the JDK that the upstream A2UI sources use, under ai.littlepress.a2ui.compat.java.*,
// for desktop and iOS. Overlay files are the upstream files with their `java.` imports pointed here
// (scripts/sync-upstream). On desktop every class delegates to the JDK, so behaviour is the JDK's;
// on iOS it is implemented on Foundation. Locale is the one type in the upstream public API: on the
// JVM it is java.util.Locale itself, so signatures match the official Android artifacts.
kotlin {
    a2uiTargets()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
