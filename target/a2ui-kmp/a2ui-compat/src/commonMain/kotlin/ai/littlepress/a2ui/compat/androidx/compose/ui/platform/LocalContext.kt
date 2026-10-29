package ai.littlepress.a2ui.compat.androidx.compose.ui.platform

import ai.littlepress.a2ui.compat.android.content.Context

/**
 * Stands in for androidx.compose.ui.platform.LocalContext, which only Android has. Upstream reads
 * `LocalContext.current` to pass it to APIs that take a [Context]; on desktop and iOS nothing
 * provides one, so this is always a plain [Context].
 */
public object LocalContext {
    public val current: Context = Context()
}
