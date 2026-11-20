package app.knotwork.android.domain.services

import app.knotwork.android.domain.engine.structured.CloudStructuredInferenceClientFactory
import app.knotwork.android.domain.models.CloudProvider
import javax.inject.Inject

/**
 * The model seam behind
 * [app.knotwork.android.domain.usecases.GeneratePipelineFromPromptUseCase].
 *
 * One prompt in, one whole reply out — no schema, no repairs, no persistence. The use
 * case owns the
 * [app.knotwork.android.domain.engine.structured.StructuredOutputGate], the retry
 * budget and every check, so a JVM test can stand in a scripted reply without a
 * network, a device or a real provider.
 *
 * The composition root binds the production implementation
 * ([DeepSeekPipelineGenerationModel]) through
 * `app.knotwork.android.di.LocalToolsModule`.
 *
 * A plain interface rather than a `fun interface`: every consumer spells out its own
 * implementation (the scripted test gateways and the DeepSeek gateway), so there is no
 * SAM conversion to preserve — and the file map's KDoc-seed rule reads a
 * `fun interface` declaration's name as `interface`, which would leave this file
 * without a description seed.
 */
interface PipelineGenerationModel {

    /**
     * Runs one completion for [prompt] and returns the model's complete reply.
     *
     * @param prompt The fully-rendered generation prompt.
     * @return The whole reply text (never `null`; an empty string is a valid — and
     *   validation-failing — answer).
     */
    suspend fun complete(prompt: String): String
}

/**
 * The app's default sentence-to-pipeline model: **DeepSeek Flash**, reached through the
 * cloud structured-inference path the other structured-output consumers already use.
 *
 * The provider is fixed to [CloudProvider.DEEPSEEK] on purpose. *Generate a pipeline
 * from a sentence* is a product decision rather than a user-tunable engine choice: no
 * new provider is introduced, no settings key is added, and the reply rides the same
 * retry-wrapped client as a CLOUD node. The concrete model id still resolves through
 * the app's normal per-provider default, which for DeepSeek is
 * `DeepSeekModels.DeepSeekV4Flash`.
 *
 * The prompt and the reply travel through the same cloud path a CLOUD node uses: the
 * request is recorded for the privacy indicator by the client factory that actually
 * sends it, and the "Block network from local model" restriction and the address rules
 * apply unchanged. When DeepSeek is not set up, the factory hands back `null` and this
 * gateway fails with a sentence that names the fix.
 *
 * @property structuredClientFactory Builds the cloud structured-inference client; it
 *   returns `null` when the provider cannot be reached right now.
 */
class DeepSeekPipelineGenerationModel @Inject constructor(
    private val structuredClientFactory: CloudStructuredInferenceClientFactory,
) : PipelineGenerationModel {

    override suspend fun complete(prompt: String): String {
        val client = structuredClientFactory.create(CloudProvider.DEEPSEEK, onToken = {})
            ?: throw PipelineGenerationUnavailableException(UNAVAILABLE_MESSAGE)
        return client.infer(prompt, temperature = null)
    }

    private companion object {
        /**
         * Shown when the DeepSeek client cannot be built. The endpoint rules and the
         * "Block network from local model" restriction refuse the same way, so the
         * sentence names the most common fix rather than guessing the cause.
         */
        const val UNAVAILABLE_MESSAGE =
            "The DeepSeek model cannot be reached. Add a DeepSeek API key in Settings, " +
                "or check the network restrictions there."
    }
}

/**
 * The generator's model cannot be reached right now — most often because this device has
 * no DeepSeek API key configured.
 *
 * @constructor Builds the failure from a complete, user-facing sentence, never a stack
 *   trace.
 */
class PipelineGenerationUnavailableException(message: String) : IllegalStateException(message)
