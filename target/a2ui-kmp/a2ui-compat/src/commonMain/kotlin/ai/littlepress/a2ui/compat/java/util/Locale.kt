package ai.littlepress.a2ui.compat.java.util

/**
 * java.util.Locale for common code. On the JVM (desktop, and Android consumers) this is
 * java.util.Locale itself, so public upstream signatures that take or return a Locale are the same as
 * in the official Android artifacts.
 */
public expect class Locale

/** The ISO 639 language code, e.g. "en" (java.util.Locale.getLanguage). */
public expect val Locale.language: String

/** The ISO 3166 country code, e.g. "US", or "" (java.util.Locale.getCountry). */
public expect val Locale.country: String

/**
 * The static members of java.util.Locale that upstream uses (`Locale.US`, `Locale.getDefault()`),
 * and forLanguageTag for creating a Locale in common code.
 */
public expect object Locales {
    public val US: Locale

    public fun getDefault(): Locale

    /** A locale for an IETF BCP 47 language tag, e.g. "de-DE". */
    public fun forLanguageTag(tag: String): Locale
}
