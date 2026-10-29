package ai.littlepress.a2ui.compat.java.util

public actual typealias Locale = java.util.Locale

public actual val Locale.language: String
    get() = getLanguage()

public actual val Locale.country: String
    get() = getCountry()

public actual object Locales {
    public actual val US: Locale = java.util.Locale.US

    public actual fun getDefault(): Locale = java.util.Locale.getDefault()

    public actual fun forLanguageTag(tag: String): Locale = java.util.Locale.forLanguageTag(tag)
}

/**
 * This locale with the numbering system Android's java.text uses for it, unless one is given
 * ("-u-nu-"): the JDK's defaults differ for some locales (e.g. Latin digits for pa-PK).
 */
internal val Locale.withAndroidNumbering: Locale
    get() = if (getUnicodeLocaleType("nu") != null) this else java.util.Locale.Builder().setLocale(this)
        .setUnicodeLocaleKeyword("nu", androidNumbering(language, script.ifEmpty { null }, country.ifEmpty { null }))
        .build()
