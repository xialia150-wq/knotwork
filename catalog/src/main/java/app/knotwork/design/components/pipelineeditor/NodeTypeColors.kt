@file:Suppress("MagicNumber") // Luminance bands ARE the data — naming the breakpoints would not clarify them.

package app.knotwork.design.components.pipelineeditor

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import app.knotwork.design.theme.KnotworkTheme
import app.knotwork.design.tokens.OklchColorMath
import app.knotwork.design.tokens.Srgb

/** Quiet header background, composed before contrast is measured. */
fun quietHeaderBackground(tint: Color, surface: Color): Color = tint.copy(alpha = 0.14f).compositeOver(surface)

/** Choose the higher-contrast opaque foreground for a vivid node header. */
fun vividHeaderForeground(strip: Color): Color {
    val luminance = strip.luminance()
    val darkContrast = (luminance + 0.05f) / 0.05f
    val lightContrast = 1.05f / (luminance + 0.05f)
    return if (darkContrast >= lightContrast) Color.Black else Color.White
}

/** Actual elevated card surface, shared by rendering and contrast tests. */
fun nodeCardSurfaceColor(scheme: ColorScheme, elevation: Dp): Color = scheme.surfaceColorAtElevation(elevation)

/** Protect a port against both the card and the surrounding canvas at 3:1. */
fun portForeground(tint: Color, surface: Color, canvas: Color): Color {
    fun contrast(a: Color, b: Color): Double {
        val x = a.luminance().toDouble()
        val y = b.luminance().toDouble()
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }
    fun readable(color: Color): Boolean = contrast(color, surface) >= 3.0 && contrast(color, canvas) >= 3.0
    if (readable(tint)) return tint
    val rgb = Srgb(tint.red.toDouble(), tint.green.toDouble(), tint.blue.toDouble())
    val original = OklchColorMath.toOklch(OklchColorMath.toOklab(OklchColorMath.toLinear(rgb)))
    val target = if (maxOf(surface.luminance(), canvas.luminance()) < 0.18f) 1.0 else 0.0
    var low = 0.0
    var high = 1.0
    var result = tint
    repeat(24) {
        val amount = (low + high) / 2.0
        val lightness = original.lightness + (target - original.lightness) * amount
        val mapped = OklchColorMath.gamutMap(original.copy(lightness = lightness))
        val candidate = Color(mapped.red.toFloat(), mapped.green.toFloat(), mapped.blue.toFloat())
        if (readable(candidate)) {
            result = candidate
            high = amount
        } else {
            low = amount
        }
    }
    return result
}

/** Retains the node hue, adjusting lightness only when the original cannot reach 4.5:1. */
fun quietHeaderForeground(tint: Color, background: Color): Color {
    fun contrast(a: Color, b: Color): Double {
        val x = a.luminance().toDouble()
        val y = b.luminance().toDouble()
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }
    if (contrast(tint, background) >= 4.5) return tint
    val rgb = Srgb(tint.red.toDouble(), tint.green.toDouble(), tint.blue.toDouble())
    val lab = OklchColorMath.toOklab(OklchColorMath.toLinear(rgb))
    val original = OklchColorMath.toOklch(lab)
    val target = if (background.luminance() < 0.18f) 1.0 else 0.0
    var low = 0.0
    var high = 1.0
    var result = tint
    repeat(24) {
        val amount = (low + high) / 2.0
        val lightness = original.lightness + (target - original.lightness) * amount
        val color = OklchColorMath.gamutMap(original.copy(lightness = lightness))
        val candidate = Color(color.red.toFloat(), color.green.toFloat(), color.blue.toFloat())
        if (contrast(candidate, background) >= 4.51) {
            result = candidate
            high = amount
        } else {
            low = amount
        }
    }
    return result
}

/**
 * Reads the header-strip tint for [this] node type from the
 * [KnotworkTheme.extended] palette. The 12 hues are pinned identically in
 * light and dark themes by design (see `tokens/ExtendedColors.kt`) so a
 * pipeline reads the same on either palette.
 *
 * @return the [Color] from `extended.node*` matching [this] enum entry.
 */
@Composable
@ReadOnlyComposable
fun NodeType.headerTint(): Color {
    val extended = KnotworkTheme.extended
    return when (this) {
        NodeType.INPUT -> extended.nodeInput
        NodeType.OUTPUT -> extended.nodeOutput
        NodeType.LITE_RT -> extended.nodeLiteRt
        NodeType.CLOUD -> extended.nodeCloud
        NodeType.INTENT_ROUTER -> extended.nodeIntentRouter
        NodeType.IF_CONDITION -> extended.nodeIfCondition
        NodeType.CLARIFICATION -> extended.nodeClarification
        NodeType.TOOL -> extended.nodeTool
        NodeType.DECOMPOSITION -> extended.nodeDecomposition
        NodeType.QUEUE_PROCESSOR -> extended.nodeQueueProcessor
        NodeType.EVALUATION -> extended.nodeEvaluation
        NodeType.SUMMARY -> extended.nodeSummary
        NodeType.PIPELINE -> extended.nodePipeline
        // Skills have no dedicated node hue: they are branded with the amber
        // `primary` everywhere in the app (Skill library, AppIcons.Skill), so
        // the editor header strip reuses it rather than introducing a 13th hue.
        NodeType.SKILL -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Picks an on-tone foreground colour (glyph + label) that reads on top of
 * the given header [strip] tint. Implements the L-band rule from
 * NodeCard tints:
 *
 *  - `L < 0.5` → `Color.White`.
 *  - `L ≥ 0.7` → `MaterialTheme.colorScheme.onSurface`.
 *  - Middle band (`0.5..0.7`) → `MaterialTheme.colorScheme.onPrimary`, the
 *    canonical Knotwork inverse foreground.
 *
 * The bands are the palette's look, not a contrast guarantee. Every node hue sits
 * below L 0.5 and gets white, which measures 6.7–7.2:1 on OUTPUT and PIPELINE but
 * 2.3–4.4:1 on the others (TOOL 2.34, SKILL in the dark theme 2.03) — under the
 * 4.5:1 WCAG AA asks of the 11 sp label. Choosing the higher-contrast of white and
 * a dark ink would reach it on all but LITE_RT; it was tried and the white labels
 * were kept as a design choice.
 *
 * Returning a single foreground avoids the otherwise tempting per-hue lookup
 * table — every node hue is run through the same band check so adding a
 * future 13th hue does not require a token edit.
 *
 * @param strip the header-strip background colour returned by [headerTint].
 * @return the foreground tone to use for the glyph + uppercase type label.
 */
@Composable
@ReadOnlyComposable
fun headerOnColor(strip: Color): Color {
    val l = strip.luminance()
    return when {
        l < 0.5f -> Color.White
        l >= 0.7f -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onPrimary
    }
}

/**
 * Uppercase display label for a node type rendered in the header strip
 * (`LabelSm` tracking 0.08 em per spec). Kept here next to [headerTint]
 * so a contributor adding a node type only edits one file group.
 *
 * @return human-readable two-or-three-word label.
 */
fun NodeType.displayLabel(): String = when (this) {
    NodeType.INPUT -> "INPUT"
    NodeType.OUTPUT -> "OUTPUT"
    NodeType.LITE_RT -> "LITE-RT"
    NodeType.CLOUD -> "CLOUD"
    NodeType.INTENT_ROUTER -> "INTENT ROUTER"
    NodeType.IF_CONDITION -> "IF"
    NodeType.CLARIFICATION -> "CLARIFY"
    NodeType.TOOL -> "TOOL"
    NodeType.DECOMPOSITION -> "DECOMPOSE"
    NodeType.QUEUE_PROCESSOR -> "QUEUE"
    NodeType.EVALUATION -> "EVAL"
    NodeType.SUMMARY -> "SUMMARY"
    NodeType.PIPELINE -> "PIPELINE"
    NodeType.SKILL -> "SKILL"
}
