package app.knotwork.android.domain.usecases

import app.knotwork.android.domain.models.GeneratedPipelineDraft
import app.knotwork.android.domain.models.PipelineGraph
import app.knotwork.android.domain.models.PipelineValidationException
import app.knotwork.android.domain.repositories.PipelineRepository
import app.knotwork.android.domain.services.PipelineCompositionValidator
import kotlinx.coroutines.CancellationException
import java.util.UUID
import javax.inject.Inject

/**
 * Persists a reviewed generation draft — the *Add to canvas* step.
 *
 * This is deliberately separate from
 * [GeneratePipelineFromPromptUseCase]: generating produces a value the user looks at,
 * and saving is the one explicit action that writes it. Being the only write path is
 * what makes "cancelled, failed and regenerated attempts never overwrite a pipeline"
 * true by construction rather than by remembering to check.
 *
 * **A fresh identity, every time.** The draft has no id; this use case mints a new UUID
 * when it is invoked. Saving therefore always inserts a new pipeline — even saving two
 * drafts in a row, or saving the same draft twice, cannot replace an existing one.
 *
 * The graph still passes the ordinary save checks ([PipelineGraph.validate] plus
 * [PipelineCompositionValidator]); a draft that was valid at preview time is checked
 * again here because the composition can have changed in between (a referenced
 * pipeline may have been deleted).
 *
 * @property pipelineRepository Persistence sink.
 * @property compositionValidator Cross-pipeline validator, the same one a manual save
 *   runs.
 */
class SaveGeneratedPipelineUseCase @Inject constructor(
    private val pipelineRepository: PipelineRepository,
    private val compositionValidator: PipelineCompositionValidator,
) {

    /**
     * Saves [draft] as a new pipeline.
     *
     * @param draft The draft the user accepted, with the preview's coordinates already
     *   applied by the presentation layer.
     * @return [Result.success] with the persisted graph (the id the editor should open),
     *   or [Result.failure] for an empty draft, a validation failure, or a storage
     *   error.
     */
    suspend operator fun invoke(draft: GeneratedPipelineDraft): Result<PipelineGraph> {
        if (draft.nodes.isEmpty()) return Result.failure(IllegalArgumentException(NO_NODES_MESSAGE))
        val graph = draft.toGraph(id = UUID.randomUUID().toString())
        val errors = graph.validate() + compositionValidator.validate(graph)
        if (errors.isNotEmpty()) return Result.failure(PipelineValidationException(errors))
        return try {
            pipelineRepository.savePipeline(graph)
            Result.success(graph)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        /** A draft with no nodes is not a pipeline; refused before it reaches storage. */
        const val NO_NODES_MESSAGE = "The generated pipeline has no nodes."
    }
}
