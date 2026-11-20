package app.knotwork.android.presentation.ui.orchestrator

import app.knotwork.android.R
import app.knotwork.android.domain.models.ConnectionModel
import app.knotwork.android.domain.models.GeneratedPipelineDraft
import app.knotwork.android.domain.models.NodeContextConfig
import app.knotwork.android.domain.models.NodeModel
import app.knotwork.android.domain.models.NodeType
import app.knotwork.android.domain.models.PipelineValidationError
import app.knotwork.android.domain.models.PipelineValidationException
import app.knotwork.android.domain.usecases.GeneratePipelineFromPromptUseCase
import app.knotwork.android.domain.usecases.PipelineGenerationException
import app.knotwork.android.domain.usecases.SaveGeneratedPipelineUseCase
import app.knotwork.android.presentation.ui.common.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The dialog's ViewModel: a draft is a preview until *Add to canvas* says otherwise,
 * the layout comes from `AutoLayout` rather than from the model, and closing the dialog
 * cancels the in-flight generation without writing anything.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PipelineGenerationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var generatePipeline: GeneratePipelineFromPromptUseCase
    private lateinit var saveGeneratedPipeline: SaveGeneratedPipelineUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        generatePipeline = mockk()
        saveGeneratedPipeline = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a generated draft is previewed with auto-layout positions and nothing is saved`() = runTest {
        coEvery { generatePipeline(any(), any<String>()) } returns Result.success(draft())

        val viewModel = viewModel()
        viewModel.updateDemand("把长文总结成三点")
        viewModel.generate()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        val preview = requireNotNull(state.previewGraph)
        assertEquals("长文三点总结", preview.name)
        assertEquals(3, preview.nodes.size)
        // AutoLayout puts the second layer below the first; the model never supplies this.
        assertTrue(requireNotNull(state.draft).nodes.first { it.id == "sum" }.y > 0f)
        assertNotNull(state.plan)
        assertNull(state.savedPipelineId)
        coVerify(exactly = 0) { saveGeneratedPipeline(any()) }
    }

    @Test
    fun `a blank demand is refused before any model call`() = runTest {
        val viewModel = viewModel()
        viewModel.updateDemand("   ")
        viewModel.generate()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(UiText(R.string.orchestrator_generate_blank_demand), viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { generatePipeline(any(), any()) }
    }

    @Test
    fun `a failed generation is reported as text`() = runTest {
        coEvery { generatePipeline(any(), any<String>()) } returns
            Result.failure(PipelineGenerationException("no valid reply"))

        val viewModel = viewModel()
        viewModel.updateDemand("把长文总结成三点")
        viewModel.generate()
        testDispatcher.scheduler.advanceUntilIdle()

        val error = viewModel.uiState.value.errorMessage
        assertTrue(error is UiText.Resource)
        assertEquals(R.string.orchestrator_generate_failed, (error as UiText.Resource).id)
        assertNull(viewModel.uiState.value.savedPipelineId)
    }

    @Test
    fun `add to canvas writes only after the user confirms`() = runTest {
        val saved = draft().toGraph(id = "saved-pipeline")
        coEvery { generatePipeline(any(), any<String>()) } returns Result.success(draft())
        coEvery { saveGeneratedPipeline(any()) } returns Result.success(saved)

        val viewModel = viewModel()
        viewModel.updateDemand("把长文总结成三点")
        viewModel.generate()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { saveGeneratedPipeline(any()) }

        viewModel.addToCanvas()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("saved-pipeline", viewModel.uiState.value.savedPipelineId)
        assertFalse(viewModel.uiState.value.isOpen)
        assertNull(viewModel.uiState.value.draft)
        coVerify(exactly = 1) { saveGeneratedPipeline(any()) }

        viewModel.consumeSavedPipeline()
        assertNull(viewModel.uiState.value.savedPipelineId)
    }

    @Test
    fun `a failed save keeps the draft on screen and writes nothing`() = runTest {
        coEvery { generatePipeline(any(), any<String>()) } returns Result.success(draft())
        coEvery { saveGeneratedPipeline(any()) } returns
            Result.failure(PipelineValidationException(listOf(PipelineValidationError.MissingOutput)))

        val viewModel = viewModel()
        viewModel.updateDemand("把长文总结成三点")
        viewModel.generate()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.addToCanvas()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.savedPipelineId)
        assertNotNull(viewModel.uiState.value.draft)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `dismissing cancels an in-flight generation and saves nothing`() = runTest {
        coEvery { generatePipeline(any(), any<String>()) } coAnswers {
            delay(IN_FLIGHT_DELAY_MS)
            Result.success(draft())
        }

        val viewModel = viewModel()
        viewModel.updateDemand("把长文总结成三点")
        viewModel.generate()
        testDispatcher.scheduler.advanceTimeBy(100)
        viewModel.dismiss()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isOpen)
        assertFalse(state.isGenerating)
        assertNull(state.draft)
        assertNull(state.savedPipelineId)
        coVerify(exactly = 0) { saveGeneratedPipeline(any()) }
    }

    @Test
    fun `change one sentence accumulates and regenerates`() = runTest {
        coEvery { generatePipeline(any(), any<String>()) } returns Result.success(draft())

        val viewModel = viewModel()
        viewModel.updateDemand("总结长文")
        viewModel.generate()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.revise("输出三点")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("输出三点", viewModel.uiState.value.revision)
        coVerify { generatePipeline("总结长文", "输出三点") }
    }

    private fun viewModel() = PipelineGenerationViewModel(generatePipeline, saveGeneratedPipeline)

    /** A runnable draft, as the generator would return it (coordinates not set). */
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
                x = 0f,
                y = 0f,
                label = "摘要",
                systemPrompt = "把输入总结成三点。",
                contextConfig = NodeContextConfig.defaultForType(NodeType.CLOUD),
            ),
            NodeModel(
                id = "out",
                type = NodeType.OUTPUT,
                x = 0f,
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

    private companion object {
        /** Long enough that the generation is still in flight when the dialog closes. */
        const val IN_FLIGHT_DELAY_MS = 10_000L
    }
}
