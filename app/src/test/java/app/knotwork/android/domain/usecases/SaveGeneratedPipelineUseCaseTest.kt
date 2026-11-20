package app.knotwork.android.domain.usecases

import app.knotwork.android.domain.models.ConnectionModel
import app.knotwork.android.domain.models.GeneratedPipelineDraft
import app.knotwork.android.domain.models.NodeContextConfig
import app.knotwork.android.domain.models.NodeModel
import app.knotwork.android.domain.models.NodeType
import app.knotwork.android.domain.models.PipelineGraph
import app.knotwork.android.domain.models.PipelineValidationError
import app.knotwork.android.domain.models.PipelineValidationException
import app.knotwork.android.domain.repositories.PipelineRepository
import app.knotwork.android.domain.services.PipelineCompositionValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CancellationException

/**
 * *Add to canvas* is the only write path of the sentence-to-pipeline feature, so these
 * tests pin the properties that make it safe: a fresh id every time (so nothing stored
 * is ever replaced), the ordinary save checks, and no write at all when the draft is not
 * a pipeline.
 */
class SaveGeneratedPipelineUseCaseTest {

    private val pipelineRepository: PipelineRepository = mockk(relaxed = true)

    private val compositionValidator: PipelineCompositionValidator = mockk {
        coEvery { validate(any()) } returns emptyList()
    }

    private val useCase = SaveGeneratedPipelineUseCase(pipelineRepository, compositionValidator)

    @Test
    fun `a valid draft is saved under a new id`() = runTest {
        val saved = slot<PipelineGraph>()
        coEvery { pipelineRepository.savePipeline(capture(saved)) } returns Unit

        val result = useCase(draft())

        assertTrue(result.isSuccess)
        val graph = requireNotNull(result.getOrNull())
        assertEquals(graph, saved.captured)
        assertEquals(3, graph.nodes.size)
        assertEquals(2, graph.connections.size)
        assertTrue(graph.id.isNotBlank())
        assertEquals("长文三点总结", graph.name)
        coVerify(exactly = 1) { pipelineRepository.savePipeline(any()) }
    }

    @Test
    fun `two saves of one draft are two pipelines`() = runTest {
        coEvery { pipelineRepository.savePipeline(any()) } returns Unit

        val first = requireNotNull(useCase(draft()).getOrNull())
        val second = requireNotNull(useCase(draft()).getOrNull())

        assertNotEquals(first.id, second.id)
        coVerify(exactly = 2) { pipelineRepository.savePipeline(any()) }
    }

    @Test
    fun `an unrunnable draft is refused before storage`() = runTest {
        val noOutput = draft().copy(nodes = draft().nodes.filter { it.type != NodeType.OUTPUT })

        val result = useCase(noOutput)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is PipelineValidationException)
        assertTrue(
            (error as PipelineValidationException).errors.contains(PipelineValidationError.MissingOutput),
        )
        coVerify(exactly = 0) { pipelineRepository.savePipeline(any()) }
    }

    @Test
    fun `an empty draft is refused before storage`() = runTest {
        val result = useCase(GeneratedPipelineDraft(name = "empty"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { pipelineRepository.savePipeline(any()) }
    }

    @Test
    fun `a storage failure is returned, not thrown`() = runTest {
        coEvery { pipelineRepository.savePipeline(any()) } throws IllegalStateException("disk full")

        val result = useCase(draft())

        assertTrue(result.isFailure)
        assertEquals("disk full", result.exceptionOrNull()?.message)
    }

    @Test
    fun `cancellation during the write escapes as cancellation`() = runTest {
        coEvery { pipelineRepository.savePipeline(any()) } throws CancellationException("stop")

        var thrown: Throwable? = null
        try {
            useCase(draft())
        } catch (e: CancellationException) {
            thrown = e
        }

        assertNotNull(thrown)
    }

    /** A minimal, runnable draft: `INPUT → CLOUD → OUTPUT`, as the generator would yield. */
    private fun draft(): GeneratedPipelineDraft = GeneratedPipelineDraft(
        name = "长文三点总结",
        plan = "用一个 CLOUD 节点做摘要。",
        nodes = listOf(
            NodeModel(
                id = "in",
                type = NodeType.INPUT,
                x = 0f,
                y = 0f,
                label = "输入",
                systemPrompt = null,
                contextConfig = NodeContextConfig.defaultForType(NodeType.INPUT),
            ),
            NodeModel(
                id = "sum",
                type = NodeType.CLOUD,
                x = 240f,
                y = 0f,
                label = "摘要",
                systemPrompt = "把输入总结成三点。",
                contextConfig = NodeContextConfig.defaultForType(NodeType.CLOUD),
            ),
            NodeModel(
                id = "out",
                type = NodeType.OUTPUT,
                x = 480f,
                y = 0f,
                label = "输出",
                systemPrompt = null,
                contextConfig = NodeContextConfig.defaultForType(NodeType.OUTPUT),
            ),
        ),
        connections = listOf(
            ConnectionModel(id = "e1", sourceNodeId = "in", targetNodeId = "sum"),
            ConnectionModel(id = "e2", sourceNodeId = "sum", targetNodeId = "out"),
        ),
    )
}
