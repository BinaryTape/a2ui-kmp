package ai.littlepress.a2ui.compat.android.content

import java.awt.Desktop
import java.net.URI

internal actual fun openUrl(url: String) {
    check(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        "Opening URLs is not supported on this desktop"
    }
    Desktop.getDesktop().browse(URI(url))
}
