package ai.littlepress.a2ui.compat.java.text

import ai.littlepress.a2ui.compat.java.util.Locale

internal actual fun datePattern(style: Int, locale: Locale): String =
    (java.text.DateFormat.getDateInstance(style, locale) as java.text.SimpleDateFormat).toPattern()

internal actual fun timePattern(style: Int, locale: Locale): String =
    (java.text.DateFormat.getTimeInstance(style, locale) as java.text.SimpleDateFormat).toPattern()

internal actual val Locale.languageTag: String
    get() = toLanguageTag()
