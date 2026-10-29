package ai.littlepress.a2ui.compat.java.text

import ai.littlepress.a2ui.compat.java.util.Locale
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterFullStyle
import platform.Foundation.NSDateFormatterLongStyle
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSDateFormatterStyle
import platform.Foundation.countryCode
import platform.Foundation.languageCode
import platform.Foundation.scriptCode

internal actual fun datePattern(style: Int, locale: Locale): String =
    NSDateFormatter().apply {
        this.locale = locale.ns
        dateStyle = nsStyle(style)
        timeStyle = NSDateFormatterNoStyle
    }.dateFormat

internal actual fun timePattern(style: Int, locale: Locale): String =
    NSDateFormatter().apply {
        this.locale = locale.ns
        dateStyle = NSDateFormatterNoStyle
        timeStyle = nsStyle(style)
    }.dateFormat

private fun nsStyle(style: Int): NSDateFormatterStyle = when (style) {
    DateFormat.FULL -> NSDateFormatterFullStyle
    DateFormat.LONG -> NSDateFormatterLongStyle
    DateFormat.MEDIUM -> NSDateFormatterMediumStyle
    else -> NSDateFormatterShortStyle
}

internal actual val Locale.languageTag: String
    get() = listOfNotNull(ns.languageCode, ns.scriptCode, ns.countryCode).joinToString("-")
