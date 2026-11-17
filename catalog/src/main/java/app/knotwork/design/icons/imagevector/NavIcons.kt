package app.knotwork.design.icons.imagevector

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The bottom-navigation glyphs, drawn as one rounded single-line family.
 *
 * The four tabs read as one stroke family at 22 dp: every path below is stroked
 * with round caps and round joins, no shape is filled, and all four sit on the
 * shared 24-unit grid. Selection is carried by the accent colour and by the dot
 * under the glyph, so the family does not need a second, heavier look to read as
 * active — the 2.0-stroke variants exist only to keep the established active
 * weight available.
 *
 * They are a family of their own rather than a redraw of the general `I.*` glyphs
 * (`Chat`, `Flow`, `Tool`, `More2`): those also label list rows, dialogs and
 * onboarding steps, where this task does not own the look, so the nav glyphs are
 * added beside them instead of substituted for them.
 */
internal object NavIcons {

    /** Chat tab — a round speech bubble with a tail, the shorthand for the conversation surface. */
    val Chat: ImageVector by lazy { buildNavChat(IconStroke.DEFAULT) }

    /** Chat tab at the active 2.0 stroke — the selected bottom-nav item. */
    val ChatActive: ImageVector by lazy { buildNavChat(IconStroke.ACTIVE) }

    /** Pipelines tab — two rounded pipeline stages joined by one connector. */
    val Flow: ImageVector by lazy { buildNavFlow(IconStroke.DEFAULT) }

    /** Pipelines tab at the active 2.0 stroke — the selected bottom-nav item. */
    val FlowActive: ImageVector by lazy { buildNavFlow(IconStroke.ACTIVE) }

    /** Tools tab — a toolbox: a body with a lid seam and a rounded handle. */
    val Tool: ImageVector by lazy { buildNavTool(IconStroke.DEFAULT) }

    /** Tools tab at the active 2.0 stroke — the selected bottom-nav item. */
    val ToolActive: ImageVector by lazy { buildNavTool(IconStroke.ACTIVE) }

    /** More tab — four rounded tiles, the "everything else" affordance of the last tab. */
    val More: ImageVector by lazy { buildNavMore(IconStroke.DEFAULT) }

    /** More tab at the active 2.0 stroke — the selected bottom-nav item. */
    val MoreActive: ImageVector by lazy { buildNavMore(IconStroke.ACTIVE) }
}

// The bubble outline is one closed path: top edge, right corner, right edge, bottom-right
// corner, bottom edge up to the tail, the tail itself, then the two left corners. Kept as
// two string pieces because the whole path does not fit one ktlint-width line.
private const val NAV_CHAT_PATH: String =
    "M7 4h10a3.5 3.5 0 0 1 3.5 3.5v5a3.5 3.5 0 0 1-3.5 3.5h-5.5L7.5 20V16H7a3.5 3.5 0 0 1-3.5-3.5v-5" +
        "A3.5 3.5 0 0 1 7 4Z"

private val NavFlowTop: String = roundedRectPath(x = 4.5f, y = 3.5f, w = 15f, h = 5.5f, rx = 2.2f)

private val NavFlowBottom: String = roundedRectPath(x = 4.5f, y = 15f, w = 15f, h = 5.5f, rx = 2.2f)

private const val NAV_FLOW_LINK: String = "M12 9.5V14.5"

private const val NAV_TOOL_HANDLE: String = "M9 9.5V7.5a3 3 0 0 1 6 0v2"

private const val NAV_TOOL_LID: String = "M3.5 12.5h17"

private fun buildNavChat(strokeWidth: Float): ImageVector = iconBuilder("NavChat")
    .strokePath(NAV_CHAT_PATH, strokeWidth = strokeWidth)
    .build()

private fun buildNavFlow(strokeWidth: Float): ImageVector = iconBuilder("NavFlow")
    .strokePath(NavFlowTop, strokeWidth = strokeWidth)
    .strokePath(NAV_FLOW_LINK, strokeWidth = strokeWidth)
    .strokePath(NavFlowBottom, strokeWidth = strokeWidth)
    .build()

private fun buildNavTool(strokeWidth: Float): ImageVector = iconBuilder("NavTool")
    .strokePath(NAV_TOOL_HANDLE, strokeWidth = strokeWidth)
    .strokePath(roundedRectPath(x = 3.5f, y = 9.5f, w = 17f, h = 9.5f, rx = 2f), strokeWidth = strokeWidth)
    .strokePath(NAV_TOOL_LID, strokeWidth = strokeWidth)
    .build()

private fun buildNavMore(strokeWidth: Float): ImageVector = iconBuilder("NavMore")
    .strokePath(roundedRectPath(x = 4f, y = 4f, w = 7f, h = 7f, rx = 2f), strokeWidth = strokeWidth)
    .strokePath(roundedRectPath(x = 13f, y = 4f, w = 7f, h = 7f, rx = 2f), strokeWidth = strokeWidth)
    .strokePath(roundedRectPath(x = 4f, y = 13f, w = 7f, h = 7f, rx = 2f), strokeWidth = strokeWidth)
    .strokePath(roundedRectPath(x = 13f, y = 13f, w = 7f, h = 7f, rx = 2f), strokeWidth = strokeWidth)
    .build()
