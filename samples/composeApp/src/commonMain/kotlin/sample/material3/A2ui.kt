package sample.material3

import ai.littlepress.a2ui.compat.android.content.Context
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.processor.A2uiMessageProcessor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The one thing every screen showing A2UI needs: a message processor that knows the components the
 * agent may use (here the basic catalog, drawn with Material 3) and keeps running while the screen
 * is shown. In an app this usually lives in a ViewModel.
 */
@Composable
fun rememberA2uiProcessor(): A2uiMessageProcessor {
    val processor = remember {
        val catalog = materialA2uiBasicCatalogV1(
            // The agents in these samples send no images, videos or audio, so nothing is drawn for
            // them. An app passes its own image loader and players here.
            image = MaterialA2uiBasicCatalogV1Defaults.image { _, _, _, _, _ -> },
            video = MaterialA2uiBasicCatalogV1Defaults.video { _, _, _ -> },
            audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, _, _ -> },
            // The catalog's functions (formatting, checks, openUrl with the platform's browser).
            functions = A2uiBasicCatalogV1.Functions(Context()),
        )
        A2uiMessageProcessor(catalogs = listOf(catalog))
    }
    LaunchedEffect(processor) { withContext(Dispatchers.Default) { processor.collectMessages() } }
    return processor
}

/** Every surface the agent has created, one below the other. */
@Composable
fun A2uiSurfaces(processor: A2uiMessageProcessor, modifier: Modifier = Modifier) {
    val surfaces by processor.activeSurfaces.collectAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        surfaces.forEach { surface -> key(surface.id) { A2uiSurface(surface) } }
    }
}
