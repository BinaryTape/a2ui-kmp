package ai.littlepress.a2ui.compat.android.text.format

import ai.littlepress.a2ui.compat.android.content.Context

/** The part of android.text.format.DateFormat upstream uses. */
public object DateFormat {
    /**
     * Whether times should use the 24-hour clock. Android uses the user's 12/24-hour setting or, if
     * there is none, whether the locale's long time format has an `H` (is24HourLocale). iOS uses the
     * user's setting, as Foundation applies it to the current locale's hour format; desktop has no
     * such setting, so it checks the default locale's long time format as Android does.
     */
    public fun is24HourFormat(context: Context): Boolean = platformIs24HourFormat()
}

internal expect fun platformIs24HourFormat(): Boolean

/** Whether [pattern] has the letter [designator] outside quoted text (DateFormat.hasDesignator). */
internal fun hasDesignator(pattern: String, designator: Char): Boolean {
    var quoted = false
    for (c in pattern) {
        if (c == '\'') quoted = !quoted else if (!quoted && c == designator) return true
    }
    return false
}
