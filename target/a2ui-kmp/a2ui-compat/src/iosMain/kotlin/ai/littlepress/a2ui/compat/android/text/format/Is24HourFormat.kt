package ai.littlepress.a2ui.compat.android.text.format

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

// Foundation applies the user's 12/24-hour setting to the current locale's "j" (preferred hour).
internal actual fun platformIs24HourFormat(): Boolean =
    NSDateFormatter.dateFormatFromTemplate("j", 0u, NSLocale.currentLocale)?.let { hasDesignator(it, 'H') } ?: false
