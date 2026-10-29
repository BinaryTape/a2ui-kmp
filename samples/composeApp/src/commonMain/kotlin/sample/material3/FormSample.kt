package sample.material3

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * 2. Send what the user does back to the agent. The surface binds the text field to its data model;
 * the button's action becomes an event on [androidx.a2ui.model.processor.A2uiMessageProcessor.outboundEvents],
 * with the field's and the picker's values in its context. This sample lists the events; an app sends them to its agent.
 */
@Composable
fun FormSample() {
    val processor = rememberA2uiProcessor()
    var sent by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(processor) {
        val parser = A2uiMessageParser()
        AgentMessages.form.forEach { json -> processor.processInput(parser, json) }
        processor.outboundEvents.collect { message -> sent = sent + describe(message) }
    }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        A2uiSurfaces(processor)
        // This app's own UI: what would go to the agent.
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sent to the agent", style = MaterialTheme.typography.titleSmall)
                if (sent.isEmpty()) {
                    Text("Nothing yet", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                sent.forEach { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

private fun describe(message: A2uiClientToServerMessage): String = when (message) {
    is A2uiClientEventMessage -> "${message.type} from ${message.componentId} on ${message.surfaceId}: ${message.context}"
    is A2uiClientErrorMessage -> "error ${message.code} on ${message.surfaceId}: ${message.message}"
}
