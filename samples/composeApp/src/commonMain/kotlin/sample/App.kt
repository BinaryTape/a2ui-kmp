package sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import sample.custom.CustomCatalogSample
import sample.material3.FormSample
import sample.material3.HelloSample
import sample.material3.UpdatesSample

/** The tabs, in order. */
val SampleTabs = listOf("Hello", "Form", "Updates", "Custom")

/**
 * The samples, one per tab, shared by the desktop and the iOS app: three with the Material 3 catalog
 * (sample.material3), one with the app's own components (sample.custom). [initialTab] is one of
 * [SampleTabs]; the iOS app takes it from the `-tab` launch argument.
 */
@Composable
fun App(initialTab: String = SampleTabs.first()) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            var tab by remember { mutableIntStateOf(SampleTabs.indexOf(initialTab).coerceAtLeast(0)) }
            Column(Modifier.safeDrawingPadding()) {
                PrimaryTabRow(selectedTabIndex = tab) {
                    SampleTabs.forEachIndexed { index, title ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                    }
                }
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    when (tab) {
                        0 -> HelloSample()
                        1 -> FormSample()
                        2 -> UpdatesSample()
                        else -> CustomCatalogSample()
                    }
                }
            }
        }
    }
}
