package app.knotwork.android.domain.usecases

import app.knotwork.android.domain.engine.structured.GateResult
import app.knotwork.android.domain.engine.structured.StructuredInferenceClient
import app.knotwork.android.domain.engine.structured.StructuredOutputGate
import app.knotwork.android.domain.models.GeneratedPipelineDraft
import app.knotwork.android.domain.models.ImportProblem
import app.knotwork.android.domain.models.PipelineImportOutcome
import app.knotwork.android.domain.models.PipelineValidationError
import app.knotwork.android.domain.models.ToolSource
import app.knotwork.android.domain.pipelineio.PipelineJsonSerializer
import app.knotwork.android.domain.repositories.ToolRepository
import app.knotwork.android.domain.services.PipelineCompositionValidator
import app.knotwork.android.domain.services.PipelineGenerationModel
import app.knotwork.android.domain.services.PipelineGenerationPromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import javax.inject.Inject

/**
 * Drafts a pipeline from one sentence: prompt the model, validate the reply with the
 * app's own import and graph checks, and — when it is not runnable — hand the findings
 * back to the model for a bounded number of corrections.
 *
 * **Nothing is persisted here.** The result is a
 * [GeneratedPipelineDraft][app.knotwork.android.domain.models.GeneratedPipelineDraft]
 * the user reviews; only an explicit *Add to canvas* reaches
 * [SaveGeneratedPipelineUseCase]. A cancelled, failed or regenerated generation
 * therefore cannot overwrite a stored pipeline — it never touches the repository at
 * all.
 *
 * **Why the model gateway is injected.** The production gateway is DeepSeek Flash, but
 * a JVM test needs neither network nor key: it scripts replies through
 * [PipelineGenerationModel] and exercises every path of this loop.
 *
 * ### The retry budget (two layers, both bounded)
 *
 *  - **JSON shape** — [StructuredOutputGate] asks within [MAX_PARSE_REPAIRS] corrective
 *    re-inferences when the reply is not the requested object (fenced JSON, prose around
 *    it and a relaxed quoting are tolerated). If it still cannot produce a parseable
 *    reply, the generation fails with the last parse error: repeating the same prompt
 *    again would only spend more tokens.
 *  - **Graph validity** — when the reply parses but `PipelineJsonSerializer.parse`,
 *    `PipelineGraph.validate` or [PipelineCompositionValidator] rejects it, the findings
 *    are fed back into a corrective prompt. That happens at most
 *    [MAX_VALIDATION_ATTEMPTS] times (initial reply included), so one generation costs
 *    at most `MAX_VALIDATION_ATTEMPTS * (MAX_PARSE_REPAIRS + 1)` model calls.
 *
 * @property gate The validate-and-repair gate the prompt runs through.
 * @property model The one-call model seam (DeepSeek Flash in production).
 * @property compositionValidator Cross-pipeline checks, needed here because a reply can
 *   name another pipeline through a PIPELINE node.
 * @property toolRepository Supplies the connected MCP tools the prompt lists.
 */
class GeneratePipelineFromPromptUseCase @Inject constructor(
    private val gate: StructuredOutputGate,
    private val model: PipelineGenerationModel,
    private val compositionValidator: PipelineCompositionValidator,
    private val toolRepository: ToolRepository,
) {

    /**
     * Generates one draft.
     *
     * @param demand The user's one-sentence request.
     * @param revision An optional additional change stacked on [demand] ("再改一句");
     *   blank means none.
     * @return [Result.success] with the draft, or [Result.failure] carrying
     *   [PipelineGenerationException] (the reply never became a runnable pipeline) or
     *   the transport exception of the model call.
     * @throws CancellationException when the caller cancels — cancellation is never
     *   converted into a failed [Result].
     */
    suspend operator fun invoke(demand: String, revision: String = ""): Result<GeneratedPipelineDraft> {
        val demandText = demand.trim()
        if (demandText.isEmpty()) return Result.failure(PipelineGenerationException(BLANK_DEMAND_MESSAGE))

        val mcpTools = availableMcpTools()
        val inference = object : StructuredInferenceClient {
            override suspend fun infer(prompt: String, temperature: Float?): String = model.complete(prompt)
        }
        var prompt = PipelineGenerationPromptBuilder.buildInitial(demandText, revision, mcpTools)
        var lastProblems: List<String> = emptyList()

        for (attempt in 0 until MAX_VALIDATION_ATTEMPTS) {
            val outcome = try {
                gate.runJson(
                    inference = inference,
                    prompt = prompt,
                    serializer = GeneratedPipelineJson.serializer(),
                    nodeName = NODE_NAME,
                    maxRepairs = MAX_PARSE_REPAIRS,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return Result.failure(e)
            }
            when (outcome) {
                is GateResult.Failed -> return Result.failure(
                    PipelineGenerationException("The model did not return readable JSON: ${outcome.lastError}"),
                )
                is GateResult.Success -> {
                    when (val inspection = inspect(outcome.value)) {
                        is Inspection.Accepted -> return Result.success(inspection.draft)
                        is Inspection.Rejected -> {
                            lastProblems = inspection.problems
                            val moreAttempts = attempt < MAX_VALIDATION_ATTEMPTS - 1
                            if (moreAttempts) {
                                prompt = PipelineGenerationPromptBuilder.buildCorrection(
                                    demand = demandText,
                                    revision = revision,
                                    mcpTools = mcpTools,
                                    previousOutput = encodeJson(outcome.value),
                                    problems = inspection.problems,
                                )
                            }
                        }
                    }
                }
            }
        }
        return Result.failure(PipelineGenerationException(lastProblems.joinToString(separator = "; ")))
    }

    /**
     * Reads the connected MCP tools this device can offer a `TOOL` node.
     *
     * A discovery failure is not a generation failure — the prompt then says there are
     * no tools. Cancellation is never swallowed here.
     */
    private suspend fun availableMcpTools() = try {
        toolRepository.getAvailableTools().filter { it.source == ToolSource.MCP }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * Runs the app's own checks over one parsed reply.
     *
     * The model's answer is re-rendered into a schema-version-1 document exactly as it
     * answered, then handed to the same parser an imported file goes through
     * ([PipelineJsonSerializer.parse]) — so "the reply is a pipeline file this app can
     * read" is decided by the import path itself, not by a second interpretation.
     * Fields the format cannot represent are reported too: a reply that invents config
     * keys is corrected rather than silently stripped.
     *
     * @param reply The parsed reply.
     * @return The draft, or the findings to feed back.
     */
    private suspend fun inspect(reply: GeneratedPipelineJson): Inspection {
        val outcome = PipelineJsonSerializer.parse(reply.toDocumentJson())
        return when (outcome) {
            is PipelineImportOutcome.Success -> accept(outcome, reply.plan.trim())
            is PipelineImportOutcome.SchemaMismatch -> Inspection.Rejected(listOf(SCHEMA_VERSION_PROBLEM))
            is PipelineImportOutcome.Failure -> Inspection.Rejected(listOf(outcome.problem.describe()))
        }
    }

    /**
     * Runs the graph checks over a document the importer accepted.
     *
     * @param success The importer's accepted outcome.
     * @param plan The reply's design rationale, carried into the draft untouched.
     */
    private suspend fun accept(success: PipelineImportOutcome.Success, plan: String): Inspection {
        if (success.droppedFields.isNotEmpty()) {
            return Inspection.Rejected(success.droppedFields.map { DROPPED_FIELD_PROBLEM_PREFIX + it })
        }
        val graph = success.graph
        val errors = graph.validate() + compositionValidator.validate(graph)
        if (errors.isNotEmpty()) return Inspection.Rejected(errors.map { it.describe() })
        return Inspection.Accepted(
            GeneratedPipelineDraft(
                name = graph.name,
                plan = plan,
                nodes = graph.nodes,
                connections = graph.connections,
            ),
        )
    }

    /** The next attempt's corrective prompt input, derived from the reply text. */
    private fun encodeJson(reply: GeneratedPipelineJson): String = JSON.encodeToString(
        GeneratedPipelineJson.serializer(),
        reply,
    )

    /** What one reply turned out to be. */
    private sealed interface Inspection {
        /** The reply describes a runnable pipeline. */
        data class Accepted(val draft: GeneratedPipelineDraft) : Inspection

        /** The reply parsed but is not runnable; these are the findings to feed back. */
        data class Rejected(val problems: List<String>) : Inspection
    }

    /** Tunables, with the reason each number is what it is. */
    companion object {

        /**
         * How many replies one generation may ask for before giving up, the first one
         * included.
         *
         * Three is the smallest number that still gives the model a second chance after
         * a genuine correction: the first reply establishes intent, the second applies
         * the findings, and the third exists because a model that fixed one structural
         * error often leaves a second one (a missing OUTPUT next to a disconnected
         * node). Every attempt is a paid cloud call, so the budget does not grow.
         */
        const val MAX_VALIDATION_ATTEMPTS: Int = 3

        /**
         * JSON-shape repairs the gate may make **within one attempt**.
         *
         * Two is enough for the shapes a model actually produces (a fenced block, a
         * stray sentence, a truncated object) and keeps the worst case of one
         * generation at `MAX_VALIDATION_ATTEMPTS * (MAX_PARSE_REPAIRS + 1)` calls.
         */
        const val MAX_PARSE_REPAIRS: Int = 2

        /** Display name the gate's repair log lines attribute this consumer to. */
        private const val NODE_NAME = "pipeline-generator"

        /** Rejection text when the reply names another schema version than the app's. */
        private const val SCHEMA_VERSION_PROBLEM = "schemaVersion must be 1"

        /** Prefix of a finding that names a config key the format does not represent. */
        private const val DROPPED_FIELD_PROBLEM_PREFIX = "unsupported field in the reply: "

        /** Rejection text when there is nothing to generate from. */
        private const val BLANK_DEMAND_MESSAGE = "Describe what the pipeline should do first."

        /** Encoding of a previous reply fed back to the model. */
        private val JSON = Json { ignoreUnknownKeys = true }
    }
}

/**
 * One failed generation: every reply within the budget was unreadable or did not
 * describe a runnable pipeline.
 *
 * @constructor Builds the failure from the last attempt's findings, for the screen
 *   (untranslatable model-facing detail; the UI prefixes it with its own sentence).
 */
class PipelineGenerationException(message: String) : RuntimeException(message)

/** Feeds a plain-text finding back to the model. */
private fun ImportProblem.describe(): String = "pipeline JSON problem: $this"

/** Feeds a graph finding back to the model. */
private fun PipelineValidationError.describe(): String = "validation error: $this"

/** Placeholder identity for the document that goes through the importer. */
private const val DOCUMENT_PLACEHOLDER_ID = "generated-pipeline"

/**
 * The JSON object the generator asks for.
 *
 * `schemaVersion` has no default on purpose: a reply that omits it is a shape error the
 * gate can repair, instead of a value this code silently supplies.
 *
 * @property schemaVersion Always 1; anything else is rejected as a schema mismatch.
 * @property plan The forced design rationale ("先规划再输出").
 * @property name The pipeline's display name.
 * @property nodes The drafted nodes; coordinates are not part of the shape.
 * @property connections The drafted edges.
 */
@Serializable
private data class GeneratedPipelineJson(
    val schemaVersion: Int,
    val plan: String = "",
    val name: String,
    val nodes: List<GeneratedNodeJson>,
    val connections: List<GeneratedConnectionJson> = emptyList(),
) {

    /**
     * Re-renders this reply as a schema-version-1 pipeline document, so the app's own
     * importer decides whether it is readable.
     *
     * `plan` is dropped (the format has no such field) and coordinates are absent —
     * `AutoLayout` places the nodes when the draft is previewed.
     */
    fun toDocumentJson(): String = buildJsonObject {
        put("schemaVersion", schemaVersion)
        put("id", DOCUMENT_PLACEHOLDER_ID)
        put("name", name)
        putJsonArray("nodes") {
            nodes.forEach { node ->
                addJsonObject {
                    put("id", node.id)
                    put("type", node.type)
                    if (node.label.isNotBlank()) put("label", node.label)
                    node.config?.let { put("config", it) }
                }
            }
        }
        putJsonArray("connections") {
            connections.forEach { connection ->
                addJsonObject {
                    put("id", connection.id)
                    put("fromNodeId", connection.fromNodeId)
                    put("toNodeId", connection.toNodeId)
                    connection.label?.takeIf { it.isNotBlank() }?.let { put("label", it) }
                }
            }
        }
    }.toString()
}

/** One node of the requested reply shape. */
@Serializable
private data class GeneratedNodeJson(
    val id: String,
    val type: String,
    val label: String = "",
    val config: JsonObject? = null,
)

/** One edge of the requested reply shape. */
@Serializable
private data class GeneratedConnectionJson(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val label: String? = null,
)
