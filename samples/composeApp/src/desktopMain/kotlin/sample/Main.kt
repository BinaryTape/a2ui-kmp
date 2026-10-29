package sample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** `-tab Form` opens that tab first, as on iOS. */
fun main(args: Array<String>) = application {
    val tab = args.toList().zipWithNext().firstOrNull { it.first == "-tab" }?.second ?: SampleTabs.first()
    Window(
        onCloseRequest = ::exitApplication,
        title = "A2UI KMP Sample",
        state = rememberWindowState(width = 480.dp, height = 640.dp),
    ) { App(tab) }
}
