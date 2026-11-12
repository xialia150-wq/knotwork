package app.knotwork.design.components.pipelineeditor

import androidx.annotation.StringRes
import app.knotwork.design.R

/**
 * The string resources holding one node type's name and its one-line
 * description — the app's own text for the type.
 *
 * Both texts are written once, in `res/values/strings_node_types.xml`. The
 * public `docs/cookbook.md` opens each node's entry with the description and
 * the browser editor's palette uses the name and the description; both are
 * generated from that file and `check` fails when either drifts from it. So a
 * reworded description is a reworded cookbook entry, never a second text.
 *
 * Distinct from [displayLabel], the short uppercase label on a node card's
 * header strip (`LITE-RT`, `IF`, `CLARIFY`), which is sized for the strip.
 *
 * @property name The node type's name, e.g. *Intent Router*.
 * @property description One sentence saying what the node is for.
 */
data class NodeTypeText(@StringRes val name: Int, @StringRes val description: Int)

/**
 * The name and one-line description of [this] node type.
 *
 * Exhaustive over [NodeType], so a type added without its two texts does not
 * compile; the generators that read the resource file fail the same way for
 * the documents.
 *
 * @return the resources of this type's two texts.
 */
val NodeType.text: NodeTypeText
    get() = when (this) {
        NodeType.INPUT -> NodeTypeText(
            R.string.knotwork_node_type_input_name,
            R.string.knotwork_node_type_input_description,
        )
        NodeType.OUTPUT -> NodeTypeText(
            R.string.knotwork_node_type_output_name,
            R.string.knotwork_node_type_output_description,
        )
        NodeType.LITE_RT -> NodeTypeText(
            R.string.knotwork_node_type_lite_rt_name,
            R.string.knotwork_node_type_lite_rt_description,
        )
        NodeType.CLOUD -> NodeTypeText(
            R.string.knotwork_node_type_cloud_name,
            R.string.knotwork_node_type_cloud_description,
        )
        NodeType.INTENT_ROUTER -> NodeTypeText(
            R.string.knotwork_node_type_intent_router_name,
            R.string.knotwork_node_type_intent_router_description,
        )
        NodeType.IF_CONDITION -> NodeTypeText(
            R.string.knotwork_node_type_if_condition_name,
            R.string.knotwork_node_type_if_condition_description,
        )
        NodeType.CLARIFICATION -> NodeTypeText(
            R.string.knotwork_node_type_clarification_name,
            R.string.knotwork_node_type_clarification_description,
        )
        NodeType.TOOL -> NodeTypeText(
            R.string.knotwork_node_type_tool_name,
            R.string.knotwork_node_type_tool_description,
        )
        NodeType.DECOMPOSITION -> NodeTypeText(
            R.string.knotwork_node_type_decomposition_name,
            R.string.knotwork_node_type_decomposition_description,
        )
        NodeType.QUEUE_PROCESSOR -> NodeTypeText(
            R.string.knotwork_node_type_queue_processor_name,
            R.string.knotwork_node_type_queue_processor_description,
        )
        NodeType.EVALUATION -> NodeTypeText(
            R.string.knotwork_node_type_evaluation_name,
            R.string.knotwork_node_type_evaluation_description,
        )
        NodeType.SUMMARY -> NodeTypeText(
            R.string.knotwork_node_type_summary_name,
            R.string.knotwork_node_type_summary_description,
        )
        NodeType.PIPELINE -> NodeTypeText(
            R.string.knotwork_node_type_pipeline_name,
            R.string.knotwork_node_type_pipeline_description,
        )
        NodeType.SKILL -> NodeTypeText(
            R.string.knotwork_node_type_skill_name,
            R.string.knotwork_node_type_skill_description,
        )
    }

/** Localized short name for the canvas and its node configuration header. */
@get:StringRes
val NodeType.canvasName: Int
    get() = when (this) {
        NodeType.INPUT -> R.string.canvas_node_input
        NodeType.OUTPUT -> R.string.canvas_node_output
        NodeType.LITE_RT -> R.string.canvas_node_lite_rt
        NodeType.CLOUD -> R.string.canvas_node_cloud
        NodeType.INTENT_ROUTER -> R.string.canvas_node_intent_router
        NodeType.IF_CONDITION -> R.string.canvas_node_if_condition
        NodeType.CLARIFICATION -> R.string.canvas_node_clarification
        NodeType.TOOL -> R.string.canvas_node_tool
        NodeType.DECOMPOSITION -> R.string.canvas_node_decomposition
        NodeType.QUEUE_PROCESSOR -> R.string.canvas_node_queue_processor
        NodeType.EVALUATION -> R.string.canvas_node_evaluation
        NodeType.SUMMARY -> R.string.canvas_node_summary
        NodeType.PIPELINE -> R.string.canvas_node_pipeline
        NodeType.SKILL -> R.string.canvas_node_skill
    }
