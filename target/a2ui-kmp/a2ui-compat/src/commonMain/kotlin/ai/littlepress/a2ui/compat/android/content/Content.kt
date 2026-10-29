package ai.littlepress.a2ui.compat.android.content

import ai.littlepress.a2ui.compat.android.net.Uri

/**
 * Stands in for android.content.Context on desktop and iOS, where upstream only uses it to open
 * URLs: `startActivity` with an ACTION_VIEW [Intent] opens the URL with the platform (the default
 * browser on desktop, UIApplication on iOS). Create one with `Context()`.
 *
 * It is not android.content.Context: on Android, common code cannot pass it to the official
 * artifacts, so call APIs that take a Context from Android code there.
 */
public open class Context {
    public open val applicationContext: Context
        get() = this

    public open fun startActivity(intent: Intent) {
        require(intent.action == Intent.ACTION_VIEW) { "Only ACTION_VIEW intents are supported: ${intent.action}" }
        openUrl(requireNotNull(intent.data) { "ACTION_VIEW intent without a URL" }.toString())
    }
}

/** Opens [url] with the platform. */
internal expect fun openUrl(url: String)

/** The part of android.content.Intent that upstream uses. */
public class Intent(public val action: String, public val data: Uri?) {
    private val categories = mutableSetOf<String>()
    private var flags = 0

    public fun addCategory(category: String): Intent = apply { categories += category }

    public fun addFlags(flags: Int): Intent = apply { this.flags = this.flags or flags }

    public companion object {
        public const val ACTION_VIEW: String = "android.intent.action.VIEW"
        public const val CATEGORY_BROWSABLE: String = "android.intent.category.BROWSABLE"
        public const val FLAG_ACTIVITY_NEW_TASK: Int = 0x10000000
    }
}
