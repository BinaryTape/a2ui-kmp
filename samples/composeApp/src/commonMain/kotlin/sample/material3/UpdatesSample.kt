package sample.material3

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.model.processor.processInput
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 3. The agent keeps changing the surface after it is shown: new data and new components arrive as
 * more messages, and the surface follows. Here they come at fixed intervals.
 */
@Composable
fun UpdatesSample() {
    var run by remember { mutableIntStateOf(0) }
    Column(Modifier.padding(16.dp)) {
        OutlinedButton(onClick = { run++ }) { Text("Replay") }
        key(run) {
            val processor = rememberA2uiProcessor()
            LaunchedEffect(processor) {
                val parser = A2uiMessageParser()
                AgentMessages.updates.forEach { (delayMillis, json) ->
                    delay(delayMillis)
                    processor.processInput(parser, json)
                }
            }
            A2uiSurfaces(processor, Modifier.padding(top = 16.dp))
        }
    }
}
