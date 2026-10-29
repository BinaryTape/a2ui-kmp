import org.gradle.api.provider.Property

/** The `a2uiPublishing { }` block of a module that applies the `a2ui.publish` plugin. */
abstract class A2uiPublishingExtension {
    /**
     * The official AndroidX artifact ("<group>:<module>") that this module's Android variants depend
     * on, in the AndroidX version this release follows (a2uiUpstream in the version catalog). Unset: no Android variants.
     */
    abstract val androidArtifact: Property<String>
}
