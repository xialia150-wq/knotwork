package app.knotwork.android.presentation.ui.orchestrator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knotwork.android.R
import app.knotwork.android.domain.models.GeneratedPipelineDraft
import app.knotwork.android.domain.models.PipelineGraph
import app.knotwork.android.domain.usecases.GeneratePipelineFromPromptUseCase
import app.knotwork.android.domain.usecases.SaveGeneratedPipelineUseCase
import app.knotwork.android.presentation.ui.common.UiText
import app.knotwork.android.presentation.ui.pipeline.editor.core.AutoLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State of the *generate a pipeline from one sentence* dialog.
 *
 * The two halves are deliberately separate: [draft] is what the user is reviewing and
 * [savedPipelineId] is the only signal that something was written. Until the user
 * presses *Add to canvas*, [savedPipelineId][PipelineGenerationUiState.savedPipelineId]
 * stays `null` — a cancelled, failed or regenerated attempt leaves no trace in the
 * library.
 *
 * @property isOpen Whether the dialog is shown.
 * @property demand The one-sentence request being edited.
 * @property revision The accumulated "再改一句" changes, newline-joined.
 * @property isGenerating A generation call is in flight.
 * @property isSaving *Add to canvas* is writing the draft.
 * @property draft The reviewed draft, with the preview's layout coordinates applied.
 * @property previewGraph The same draft as a [PipelineGraph] with a throw-away id, for
 *   the dialog's flow preview. Never persisted.
 * @property plan The model's design rationale, truncated for display.
 * @property errorMessage One-shot failure text for the dialog.
 * @property feedbackMessage One-shot success text for the host's snackbar.
 * @property savedPipelineId Id of the pipeline just persisted; the host consumes it to
 *   open the editor.
 */
data class PipelineGenerationUiState(
    val isOpen: Boolean = false,
    val demand: String = "",
    val revision: String = "",
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val draft: GeneratedPipelineDraft? = null,
    val previewGraph: PipelineGraph? = null,
    val plan: String? = null,
    val errorMessage: UiText? = null,
    val feedbackMessage: UiText? = null,
    val savedPipelineId: String? = null,
)

/**
 * ViewModel behind the pipeline library's *From a sentence* entry.
 *
 * Owns the dialog state and the two use-case calls, and does the one presentation-only
 * step the domain cannot: running the editor's [AutoLayout] over the draft so the
 * preview and the saved pipeline share coordinates. The model never sees or emits
 * positions.
 */
@HiltViewModel
class PipelineGenerationViewModel @Inject constructor(
    private val generatePipeline: GeneratePipelineFromPromptUseCase,
    private val saveGeneratedPipeline: SaveGeneratedPipelineUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PipelineGenerationUiState())

    /** Dialog state, collected by `PipelineLibraryScreen`. */
    val uiState: StateFlow<PipelineGenerationUiState> = _uiState.asStateFlow()

    /** The in-flight generation, cancelled when the dialog closes or a new one starts. */
    private var generationJob: Job? = null

    /** Opens the dialog with a clean draft, keeping the text the user already wrote. */
    fun open() {
        generationJob?.cancel()
        generationJob = null
        _uiState.update { previous -> PipelineGenerationUiState(isOpen = true, demand = previous.demand) }
    }

    /**
     * Closes the dialog and throws away the draft.
     *
     * Cancelling the in-flight generation is the *only* thing that happens to it — no
     * repository is involved, so there is nothing to roll back.
     */
    fun dismiss() {
        generationJob?.cancel()
        generationJob = null
        _uiState.update { previous -> PipelineGenerationUiState(demand = previous.demand) }
    }

    /** Updates the request text. */
    fun updateDemand(demand: String) {
        _uiState.update { it.copy(demand = demand) }
    }

    /**
     * Asks for a draft (or a fresh one) from the current demand and accumulated
     * revision.
     *
     * A blank demand is refused with a field message before any model call.
     */
    fun generate() {
        val state = _uiState.value
        val demand = state.demand.trim()
        if (demand.isEmpty()) {
            _uiState.update { it.copy(errorMessage = UiText(R.string.orchestrator_generate_blank_demand)) }
            return
        }
        val revision = state.revision.trim()
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    isSaving = false,
                    errorMessage = null,
                    draft = null,
                    previewGraph = null,
                    plan = null,
                )
            }
            val result = generatePipeline(demand, revision)
            result.fold(
                onSuccess = { draft -> showDraft(draft) },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isGenerating = false, errorMessage = generationFailureText(error))
                    }
                },
            )
        }
    }

    /**
     * Stacks one more sentence onto the request and regenerates ("再改一句").
     *
     * The sentence is appended to the previous revision, not substituted for it, so
     * successive refinements accumulate instead of forgetting the earlier ones.
     *
     * @param sentence The additional change, as typed by the user. Blank text is
     *   ignored; the dialog disables its confirm action for it.
     */
    fun revise(sentence: String) {
        val addition = sentence.trim()
        if (addition.isEmpty()) return
        val accumulated = listOf(_uiState.value.revision.trim(), addition)
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")
        _uiState.update { it.copy(revision = accumulated) }
        generate()
    }

    /**
     * Persists the reviewed draft under a fresh id and hands the id to the host.
     *
     * On failure the draft stays on screen — nothing was written, so the user can retry
     * or keep editing.
     */
    fun addToCanvas() {
        val state = _uiState.value
        val draft = state.draft ?: return
        if (state.isGenerating || state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = saveGeneratedPipeline(draft)
            result.fold(
                onSuccess = { saved ->
                    _uiState.update { previous ->
                        PipelineGenerationUiState(
                            isOpen = false,
                            demand = previous.demand,
                            feedbackMessage = UiText(R.string.orchestrator_generate_success),
                            savedPipelineId = saved.id,
                        )
                    }
                },
                onFailure = { error ->
                    val preview = state.previewGraph ?: draft.toGraph(id = PREVIEW_PIPELINE_ID)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = OrchestratorErrorText.forSave(error, preview)
                                ?: UiText(R.string.errors_generic_unexpected),
                        )
                    }
                },
            )
        }
    }

    /** Host acknowledgement: the saved pipeline id has been acted on. */
    fun consumeSavedPipeline() {
        _uiState.update { it.copy(savedPipelineId = null) }
    }

    /** Host acknowledgement: the success snackbar has been shown. */
    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    /**
     * Applies the editor's layout to [draft] and publishes it as the preview.
     *
     * The model is never asked for coordinates; this is where a graph of nodes at
     * `(0, 0)` becomes the canvas arrangement the editor also produces for an imported
     * pipeline.
     */
    private fun showDraft(draft: GeneratedPipelineDraft) {
        val positioned = position(draft)
        _uiState.update {
            it.copy(
                isGenerating = false,
                draft = positioned,
                previewGraph = positioned.toGraph(id = PREVIEW_PIPELINE_ID),
                plan = positioned.plan.take(MAX_PREVIEW_PLAN_CHARS).takeIf { text -> text.isNotBlank() },
            )
        }
    }

    /** [draft] with every node moved to its `AutoLayout` position. */
    private fun position(draft: GeneratedPipelineDraft): GeneratedPipelineDraft {
        val positions = AutoLayout.compute(draft.toGraph(id = PREVIEW_PIPELINE_ID)).positions
        if (positions.isEmpty()) return draft
        return draft.copy(
            nodes = draft.nodes.map { node ->
                val point = positions[node.id] ?: return@map node
                node.copy(x = point.first, y = point.second)
            },
        )
    }

    /** The dialog's sentence for a failed generation, with the detail kept as data. */
    private fun generationFailureText(error: Throwable): UiText {
        val detail = error.message?.trim().orEmpty()
        return if (detail.isEmpty()) {
            UiText(R.string.errors_generic_unexpected)
        } else {
            UiText.of(R.string.orchestrator_generate_failed, detail)
        }
    }

    private companion object {

        /** Identity the preview graph carries. Never saved, so never collides. */
        const val PREVIEW_PIPELINE_ID = "generated-preview"

        /** Longest slice of the model's plan the preview shows. */
        const val MAX_PREVIEW_PLAN_CHARS = 600
    }
}
