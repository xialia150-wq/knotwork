package app.knotwork.android.domain.services

import app.knotwork.android.domain.engine.structured.CloudStructuredInferenceClientFactory
import app.knotwork.android.domain.engine.structured.StructuredInferenceClient
import app.knotwork.android.domain.models.CloudProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The production gateway pins the feature's model choice: DeepSeek, and only DeepSeek.
 * Both the success path and the "not configured" path are asserted here without a
 * network or a key.
 */
class PipelineGenerationModelTest {

    @Test
    fun `the default gateway asks DeepSeek and returns its reply`() = runTest {
        val factory = RecordingStructuredFactory(reply = "the draft")
        val model = DeepSeekPipelineGenerationModel(factory)

        val reply = model.complete("the prompt")

        assertEquals("the draft", reply)
        assertEquals(CloudProvider.DEEPSEEK, factory.provider)
        assertEquals("the prompt", factory.prompt)
    }

    @Test
    fun `an unavailable DeepSeek fails with a sentence that names the fix`() = runTest {
        val model = DeepSeekPipelineGenerationModel(RecordingStructuredFactory(reply = null))

        val thrown: Throwable? = try {
            model.complete("the prompt")
            null
        } catch (e: Exception) {
            e
        }

        assertTrue(thrown is PipelineGenerationUnavailableException)
        assertTrue(thrown?.message.orEmpty().contains("DeepSeek"))
        assertTrue(thrown?.message.orEmpty().contains("API key"))
    }

    /** A factory that records the call and returns a scripted client, or `null`. */
    private class RecordingStructuredFactory(private val reply: String?) : CloudStructuredInferenceClientFactory {
        var provider: CloudProvider? = null
        var prompt: String? = null

        override suspend fun create(
            provider: CloudProvider,
            onToken: suspend (String) -> Unit,
        ): StructuredInferenceClient? {
            this.provider = provider
            if (reply == null) return null
            return object : StructuredInferenceClient {
                override suspend fun infer(prompt: String, temperature: Float?): String {
                    this@RecordingStructuredFactory.prompt = prompt
                    return reply
                }
            }
        }
    }
}
