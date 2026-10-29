package ai.littlepress.a2ui.integration

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.compose.ui.toJsonSchemaString
import androidx.compose.material3.a2ui.catalog.A2uiAudioPlayerRenderer
import androidx.compose.material3.a2ui.catalog.A2uiImageRenderer
import androidx.compose.material3.a2ui.catalog.A2uiVideoRenderer
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

/**
 * material3-a2ui's catalog, from the published artifacts: the same schema as the official one
 * (on iOS the same JSON with keys in another order, see [jsonKeyOrderMatchesAndroid]).
 */
class Material3CatalogTest {
    @Test
    fun catalogSchemaIsTheSameEverywhere() {
        val functions = basicFunctions() ?: return
        val catalog = materialA2uiBasicCatalogV1(
            image = MaterialA2uiBasicCatalogV1Defaults.image(A2uiImageRenderer { _, _, _, _, _ -> }),
            video = MaterialA2uiBasicCatalogV1Defaults.video(A2uiVideoRenderer { _, _, _ -> }),
            audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer(A2uiAudioPlayerRenderer { _, _, _, _ -> }),
            functions = functions,
        )
        val schema = catalog.toJsonSchemaString()
        // In parts: Android's log truncates long lines.
        schema.chunked(1000).forEachIndexed { i, part -> println("A2UI-M3-SCHEMA ${platformName()} | $i | $part") }
        if (jsonKeyOrderMatchesAndroid) {
            assertEquals(ANDROID_MATERIAL3_CATALOG_SCHEMA, schema)
        } else {
            assertEquals(Json.parseToJsonElement(ANDROID_MATERIAL3_CATALOG_SCHEMA), Json.parseToJsonElement(schema))
        }
    }
}
