package ai.littlepress.a2ui.compat.android.net

/** The part of android.net.Uri that upstream uses: parsing a URL and lower-casing its scheme. */
public class Uri private constructor(private val text: String) {
    /** Lower-cases the scheme, as android.net.Uri.normalizeScheme() does. */
    public fun normalizeScheme(): Uri {
        val colon = text.indexOf(':')
        if (colon <= 0) return this
        val scheme = text.substring(0, colon)
        return if (scheme.all { it.isLetterOrDigit() || it in "+-." }) Uri(scheme.lowercase() + text.substring(colon)) else this
    }

    override fun toString(): String = text

    override fun equals(other: Any?): Boolean = other is Uri && other.text == text

    override fun hashCode(): Int = text.hashCode()

    public companion object {
        public fun parse(uriString: String): Uri = Uri(uriString)
    }
}
