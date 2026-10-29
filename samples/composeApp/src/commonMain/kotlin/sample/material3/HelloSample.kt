package sample.material3

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.model.processor.processInput
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 1. Show what the agent sends: parse each JSON message and give it to the processor. */
@Composable
fun HelloSample() {
    val processor = rememberA2uiProcessor()
    LaunchedEffect(processor) {
        val parser = A2uiMessageParser()
        // In an app, each message as it arrives from your agent (SSE, WebSocket, ...).
        AgentMessages.hello.forEach { json -> processor.processInput(parser, json) }
    }
    A2uiSurfaces(processor, Modifier.padding(16.dp))
}
