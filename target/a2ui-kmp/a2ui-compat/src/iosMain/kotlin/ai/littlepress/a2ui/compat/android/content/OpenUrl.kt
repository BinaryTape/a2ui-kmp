package ai.littlepress.a2ui.compat.android.content

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

internal actual fun openUrl(url: String) {
    val nsUrl = requireNotNull(NSURL.URLWithString(url)) { "Not a URL: $url" }
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
}
