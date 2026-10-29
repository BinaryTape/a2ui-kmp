package ai.littlepress.a2ui.compat.android.content.res

/**
 * What Resources.getString(id, formatArgs) does with a string resource (java.lang.String.format),
 * for the conversions upstream's string resources use: `%s` and `%1$s`, plus `%%` and `%n`. Any other
 * conversion, flag or width throws [IllegalArgumentException], as does a missing argument.
 */
public fun formatString(format: String, vararg args: Any?): String {
    val out = StringBuilder()
    var ordinary = 0
    var i = 0
    while (i < format.length) {
        val c = format[i]
        if (c != '%') {
            out.append(c)
            i++
            continue
        }
        val match = SPECIFIER.matchAt(format, i)
            ?: throw IllegalArgumentException("Unsupported format specifier at $i in \"$format\"")
        val (index, conversion) = match.destructured
        when (conversion) {
            "%" -> out.append('%')
            "n" -> out.append('\n')
            "s" -> {
                val position = if (index.isNotEmpty()) index.dropLast(1).toInt() - 1 else ordinary++
                if (position !in args.indices) throw IllegalArgumentException("Format specifier '${match.value}' has no argument")
                out.append(args[position].toString())
            }
        }
        i += match.value.length
    }
    return out.toString()
}

private val SPECIFIER = Regex("""%(\d+\$)?([s%n])""")
