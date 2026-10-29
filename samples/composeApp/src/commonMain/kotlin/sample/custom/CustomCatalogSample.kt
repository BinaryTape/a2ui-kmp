package sample.custom

import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Your own components instead of Material 3: one surface from the agent, drawn with [MyCatalog]
 * (needs only compose-ui), and the events that go back to the agent.
 */
@Composable
fun CustomCatalogSample() {
    // In an app this usually lives in a ViewModel.
    val processor = remember { A2uiMessageProcessor(catalogs = listOf(MyCatalog)) }
    var sent by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(processor) {
        launch { withContext(Dispatchers.Default) { processor.collectMessages() } }
        // What the user does: send it to your agent. This sample lists it.
        launch { processor.outboundEvents.collect { message -> sent = sent + describe(message) } }
        val parser = A2uiMessageParser()
        // In an app, each message as it arrives from your agent (SSE, WebSocket, ...).
        AgentMessages.order.forEach { (delayMillis, json) ->
            delay(delayMillis)
            processor.processInput(parser, json)
        }
    }

    val surfaces by processor.activeSurfaces.collectAsState()
    Column(
        Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        surfaces.forEach { surface ->
            key(surface.id) {
                // A surface is its root component; the catalog's components draw the rest.
                when (val state = observeA2uiComponentState(surface)) {
                    is A2uiComponentState.Success -> A2uiComponent(state.component)
                    is A2uiComponentState.Loading -> BasicText("Loading…")
                    is A2uiComponentState.Error -> BasicText("Could not show the surface: ${state.exception.message}")
                }
            }
        }
        BasicText("Sent to the agent", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold))
        if (sent.isEmpty()) BasicText("Nothing yet", style = TextStyle(Color.Gray, 13.sp))
        sent.forEach { BasicText(it, style = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace)) }
    }
}

private fun describe(message: A2uiClientToServerMessage): String = when (message) {
    is A2uiClientEventMessage -> "${message.type} from ${message.componentId} on ${message.surfaceId}: ${message.context}"
    is A2uiClientErrorMessage -> "error ${message.code} on ${message.surfaceId}: ${message.message}"
}
