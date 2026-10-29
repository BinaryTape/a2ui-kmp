package ai.littlepress.a2ui.compat.android.text.format

internal actual fun platformIs24HourFormat(): Boolean {
    val natural = java.text.DateFormat.getTimeInstance(java.text.DateFormat.LONG, java.util.Locale.getDefault())
    return natural is java.text.SimpleDateFormat && hasDesignator(natural.toPattern(), 'H')
}
