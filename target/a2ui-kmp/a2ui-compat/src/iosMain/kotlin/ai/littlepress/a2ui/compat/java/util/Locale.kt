package ai.littlepress.a2ui.compat.java.util

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.languageCode
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier

/**
 * [id] is the identifier as requested: Foundation drops a language's default script
 * ("pa_Guru_PK" becomes "pa_PK"), which decides the digits Android uses.
 */
public actual class Locale internal constructor(internal val ns: NSLocale, internal val id: String) {
    public constructor(ns: NSLocale) : this(ns, ns.localeIdentifier)

    override fun toString(): String = ns.localeIdentifier

    override fun equals(other: Any?): Boolean = other is Locale && other.ns.localeIdentifier == ns.localeIdentifier

    override fun hashCode(): Int = ns.localeIdentifier.hashCode()
}

public actual val Locale.language: String
    get() = ns.languageCode

public actual val Locale.country: String
    get() = ns.countryCode ?: ""

/**
 * Language, script and region of the identifier as requested (NSLocale's scriptCode is not reliable
 * for every locale).
 */
internal val Locale.parts: Triple<String, String?, String?>
    get() {
        val parts = id.substringBefore('@').split('_')
        val script = parts.drop(1).firstOrNull { it.length == 4 && it.all(Char::isLetter) }
        val region = parts.drop(1).firstOrNull {
            (it.length == 2 && it.all(Char::isLetter)) || (it.length == 3 && it.all(Char::isDigit))
        }
        return Triple(parts[0], script, region)
    }

/**
 * This locale for formatting numbers and dates, with the numbering system Android would use unless
 * one is given ("@numbers=" or "-u-nu-"): Foundation's defaults differ for some locales (it uses
 * Latin digits for "ar", where Android uses Arabic-Indic).
 */
internal val Locale.withAndroidNumbering: NSLocale
    get() {
        if ("numbers=" in id) return ns
        val (language, script, region) = parts
        val numbering = androidNumbering(language, script, region)
        // The requested identifier, so that Foundation formats with that locale's data.
        return NSLocale(localeIdentifier = id + (if ("@" in id) ";" else "@") + "numbers=$numbering")
    }

/**
 * An ICU locale identifier ("ar_EG@numbers=latn") for a BCP 47 tag ("ar-EG-u-nu-latn"), which
 * Foundation does not convert: language, script, region and variants, and the Unicode extension's
 * numbering system (nu) and calendar (ca).
 */
internal fun icuLocaleId(tag: String): String {
    val parts = tag.split('-', '_').filter { it.isNotEmpty() }
    if (parts.isEmpty()) return ""
    val id = StringBuilder(parts[0].lowercase())
    val keywords = mutableListOf<String>()
    var i = 1
    while (i < parts.size) {
        val part = parts[i]
        when {
            part.length == 1 -> break
            part.length == 4 && part.all { it.isLetter() } ->
                id.append('_').append(part.lowercase().replaceFirstChar { it.uppercaseChar() })
            (part.length == 2 && part.all { it.isLetter() }) || (part.length == 3 && part.all { it.isDigit() }) ->
                id.append('_').append(part.uppercase())
            else -> id.append('_').append(part.uppercase())
        }
        i++
    }
    while (i < parts.size) {
        val singleton = parts[i++].lowercase()
        if (singleton == "x") break
        while (i < parts.size && parts[i].length > 1) {
            val key = parts[i++].lowercase()
            if (singleton != "u" || key.length != 2) continue
            val value = parts.getOrNull(i)?.takeIf { it.length > 2 }?.lowercase()
            if (value != null) i++
            when (key) {
                "nu" -> value?.let { keywords += "numbers=$it" }
                "ca" -> value?.let { keywords += "calendar=$it" }
            }
        }
    }
    return if (keywords.isEmpty()) id.toString() else "$id@${keywords.joinToString(";")}"
}

public actual object Locales {
    public actual val US: Locale = Locale(NSLocale(localeIdentifier = "en_US"))

    public actual fun getDefault(): Locale = Locale(NSLocale.currentLocale)

    public actual fun forLanguageTag(tag: String): Locale =
        icuLocaleId(tag).let { Locale(NSLocale(localeIdentifier = it), it) }
}
