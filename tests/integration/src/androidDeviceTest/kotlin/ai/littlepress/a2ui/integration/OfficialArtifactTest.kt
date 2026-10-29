package ai.littlepress.a2ui.integration

import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfficialArtifactTest {
    /** The A2UI classes on the device come from the official AAR, which has no compat references. */
    @Test
    fun localeProviderIsJavaUtilLocale() {
        val method = A2uiLocaleProvider::class.java.getMethod("getLocale")
        assertEquals(java.util.Locale::class.java, method.returnType)
        assertNotNull(Class.forName("androidx.a2ui.model.catalog.functions.A2uiFormatDateFunction"))
    }
}
