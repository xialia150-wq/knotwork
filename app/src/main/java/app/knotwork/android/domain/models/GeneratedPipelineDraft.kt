package app.knotwork.android.domain.models

/**
 * A pipeline a model drafted from a one-sentence description, before the user accepts it.
 *
 * It deliberately has **no id**: an identity is minted only when the user presses
 * *Add to canvas*, in
 * [app.knotwork.android.domain.usecases.SaveGeneratedPipelineUseCase]. Until then a
 * draft is a value the user is looking at — regenerating, refining or dismissing it can
 * therefore never overwrite a stored pipeline, because there is nothing to overwrite.
 *
 * Node coordinates are whatever the model left them as (the prompt forbids coordinates
 * entirely). The presentation layer runs the editor's `AutoLayout` before showing the
 * preview, so the preview and the saved pipeline carry the same positions.
 *
 * @property name Display name the model proposed; normalised by the pipeline JSON
 *   parser (one line, bounded length) before this draft is built.
 * @property plan The model's short design rationale, forced out of the reply so the
 *   user can see *why* the draft looks the way it does. Display-only; never saved.
 * @property nodes The drafted nodes, in the order the model listed them.
 * @property connections The drafted edges.
 */
data class GeneratedPipelineDraft(
    val name: String,
    val plan: String = "",
    val nodes: List<NodeModel> = emptyList(),
    val connections: List<ConnectionModel> = emptyList(),
) {

    /**
     * This draft as a [PipelineGraph] carrying [id].
     *
     * Used for the preview (with a throw-away identity) and by the save use case (with
     * a freshly minted one).
     *
     * @param id Identity to carry.
     * @param updatedAt Modification timestamp to record.
     * @return The graph; empty drafts produce a graph with no nodes.
     */
    fun toGraph(id: String, updatedAt: Long = System.currentTimeMillis()): PipelineGraph = PipelineGraph(
        id = id,
        name = name,
        nodes = nodes,
        connections = connections,
        updatedAt = updatedAt,
    )
}
