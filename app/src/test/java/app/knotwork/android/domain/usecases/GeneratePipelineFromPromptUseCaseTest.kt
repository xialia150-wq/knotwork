package app.knotwork.android.domain.usecases

import app.knotwork.android.domain.engine.structured.StructuredOutputGate
import app.knotwork.android.domain.models.AgentTool
import app.knotwork.android.domain.models.NodeType
import app.knotwork.android.domain.models.ToolSource
import app.knotwork.android.domain.repositories.ToolRepository
import app.knotwork.android.domain.services.PipelineGenerationModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * The sentence-to-pipeline loop, driven by a scripted gateway instead of DeepSeek — the
 * production model is unavailable in CI, and every interesting path here is the loop's,
 * not the provider's.
 *
 * Covered: a valid reply, malformed JSON retried by the gate, a parsed-but-unrunnable
 * reply corrected with the findings, both retry budgets exhausted, and cancellation
 * escaping as cancellation rather than as a failed `Result`.
 */
class GeneratePipelineFromPromptUseCaseTest {

    @Test
    fun `a valid reply becomes a draft`() = runTest {
        val model = ScriptedModel(listOf(VALID_REPLY))

        val result = useCase(model)("把长文总结成三点")

        assertTrue(result.isSuccess)
        val draft = requireNotNull(result.getOrNull())
        assertEquals("长文三点总结", draft.name)
        assertTrue(draft.plan.contains("CLOUD"))
        assertEquals(3, draft.nodes.size)
        assertEquals(2, draft.connections.size)
        assertEquals(
            "把输入总结成三点。",
            draft.nodes.first { it.type == NodeType.CLOUD }.systemPrompt,
        )
        assertEquals(1, model.prompts.size)
        assertTrue(model.prompts.first().contains("把长文总结成三点"))
    }

    @Test
    fun `a connected MCP tool is named in the prompt`() = runTest {
        val model = ScriptedModel(listOf(VALID_REPLY))
        val tool = AgentTool(
            name = "search_web",
            description = "Search the web",
            parameters = "{}",
            source = ToolSource.MCP,
        )

        useCase(model, tools = listOf(tool))("查一下资料")

        assertTrue(model.prompts.first().contains("search_web"))
        assertTrue(model.prompts.first().contains("Search the web"))
    }

    @Test
    fun `malformed JSON is repaired by the gate before it counts as a failure`() = runTest {
        val model = ScriptedModel(listOf(GARBAGE, GARBAGE, VALID_REPLY))

        val result = useCase(model)("把长文总结成三点")

        assertTrue(result.isSuccess)
        assertEquals(3, model.prompts.size)
    }

    @Test
    fun `malformed JSON that stays malformed fails after the gate's repair budget`() = runTest {
        val model = ScriptedModel(listOf(GARBAGE))

        val result = useCase(model)("把长文总结成三点")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PipelineGenerationException)
        assertEquals(GeneratePipelineFromPromptUseCase.MAX_PARSE_REPAIRS + 1, model.prompts.size)
    }

    @Test
    fun `an unrunnable reply is corrected with the validation findings`() = runTest {
        val model = ScriptedModel(listOf(MISSING_OUTPUT_REPLY, VALID_REPLY))

        val result = useCase(model)("把长文总结成三点")

        assertTrue(result.isSuccess)
        assertEquals(2, model.prompts.size)
        // The first prompt is the plain request; the second one carries what was wrong.
        assertFalse(model.prompts.first().contains("MissingOutput"))
        assertTrue(model.prompts[1].contains("MissingOutput"))
        assertTrue(model.prompts[1].contains("校验错误"))
    }

    @Test
    fun `a wrong schema version is fed back as a correction`() = runTest {
        val wrongVersion = VALID_REPLY.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")
        val model = ScriptedModel(listOf(wrongVersion, VALID_REPLY))

        val result = useCase(model)("把长文总结成三点")

        assertTrue(result.isSuccess)
        assertEquals(2, model.prompts.size)
        assertTrue(model.prompts[1].contains("schemaVersion must be 1"))
    }

    @Test
    fun `an unrunnable reply fails after the validation budget is spent`() = runTest {
        val model = ScriptedModel(listOf(MISSING_OUTPUT_REPLY))

        val result = useCase(model)("把长文总结成三点")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PipelineGenerationException)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("MissingOutput"))
        assertEquals(GeneratePipelineFromPromptUseCase.MAX_VALIDATION_ATTEMPTS, model.prompts.size)
    }

    @Test
    fun `a reply the pipeline format cannot read is fed back as a correction`() = runTest {
        val unknownType = VALID_REPLY.replace("\"type\": \"CLOUD\"", "\"type\": \"MAGIC\"")
        val model = ScriptedModel(listOf(unknownType, VALID_REPLY))

        val result = useCase(model)("把长文总结成三点")

        assertTrue(result.isSuccess)
        assertEquals(2, model.prompts.size)
        assertTrue(model.prompts[1].contains("pipeline JSON problem"))
        assertTrue(model.prompts[1].contains("MAGIC"))
    }

    @Test
    fun `a config field the format cannot carry is fed back as a correction`() = runTest {
        val extraField = VALID_REPLY.replace(
            "\"systemPrompt\": \"把输入总结成三点。\"",
            "\"systemPrompt\": \"把输入总结成三点。\", \"temperature\": 0.2",
        )
        val model = ScriptedModel(listOf(extraField, VALID_REPLY))

        val result = useCase(model)("把长文总结成三点")

        assertTrue(result.isSuccess)
        assertEquals(2, model.prompts.size)
        assertTrue(model.prompts[1].contains("unsupported field"))
        assertTrue(model.prompts[1].contains("temperature"))
    }

    @Test
    fun `a transport failure is returned as a failure, not as a crash`() = runTest {
        val model = object : PipelineGenerationModel {
            override suspend fun complete(prompt: String): String = throw IOException("network down")
        }

        val result = useCase(model)("把长文总结成三点")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `cancellation escapes instead of being turned into a failure`() = runTest {
        val model = object : PipelineGenerationModel {
            override suspend fun complete(prompt: String): String = throw CancellationException("stop")
        }

        var thrown: Throwable? = null
        try {
            useCase(model)("把长文总结成三点")
        } catch (e: CancellationException) {
            thrown = e
        }

        assertNotNull(thrown)
        assertTrue(thrown is CancellationException)
    }

    /** Builds the use case with a real gate, a scripted model and no-op collaborators. */
    private fun useCase(model: PipelineGenerationModel, tools: List<AgentTool> = emptyList()) =
        GeneratePipelineFromPromptUseCase(
            gate = StructuredOutputGate(),
            model = model,
            compositionValidator = mockk {
                coEvery { validate(any()) } returns emptyList()
            },
            toolRepository = mockk<ToolRepository>(relaxed = true).also { repository ->
                coEvery { repository.getAvailableTools() } returns tools
            },
        )

    /** A gateway that replays a script, one reply per call. */
    private class ScriptedModel(private val replies: List<String>) : PipelineGenerationModel {
        val prompts = mutableListOf<String>()
        private var index = 0

        override suspend fun complete(prompt: String): String {
            prompts += prompt
            return replies.getOrElse(index++) { replies.last() }
        }
    }

    private companion object {
        val GARBAGE = "I could not design that pipeline."

        val VALID_REPLY = """
            {
              "schemaVersion": 1,
              "plan": "用一个 CLOUD 节点做摘要。",
              "name": "长文三点总结",
              "nodes": [
                { "id": "in", "type": "INPUT", "label": "输入" },
                { "id": "sum", "type": "CLOUD", "label": "摘要",
                  "config": { "systemPrompt": "把输入总结成三点。" } },
                { "id": "out", "type": "OUTPUT", "label": "输出" }
              ],
              "connections": [
                { "id": "e1", "fromNodeId": "in", "toNodeId": "sum" },
                { "id": "e2", "fromNodeId": "sum", "toNodeId": "out" }
              ]
            }
        """.trimIndent()

        val MISSING_OUTPUT_REPLY = """
            {
              "schemaVersion": 1,
              "plan": "只做一个摘要节点。",
              "name": "缺输出",
              "nodes": [
                { "id": "in", "type": "INPUT", "label": "输入" },
                { "id": "sum", "type": "CLOUD", "label": "摘要",
                  "config": { "systemPrompt": "把输入总结成三点。" } }
              ],
              "connections": [
                { "id": "e1", "fromNodeId": "in", "toNodeId": "sum" }
              ]
            }
        """.trimIndent()
    }
}
