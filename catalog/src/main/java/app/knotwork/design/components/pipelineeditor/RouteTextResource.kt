package app.knotwork.design.components.pipelineeditor

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.knotwork.design.R

/** Display-only resource lookup; routing and persisted labels are unchanged. */
@StringRes
internal fun routeTextResource(label: String): Int? = when (label.lowercase()) {
    "true" -> R.string.canvas_route_true
    "false" -> R.string.canvas_route_false
    "item" -> R.string.canvas_route_item
    "done" -> R.string.canvas_route_done
    "pass" -> R.string.canvas_route_pass
    "retry" -> R.string.canvas_route_retry
    "fail" -> R.string.canvas_route_fail
    else -> null
}

/** User-defined intent labels are never interpreted as built-in routes. */
internal fun OutboundPort.displayResource(): Int? = if (this is OutboundPort.Custom) null else routeTextResource(label)

/** Display the localized built-in label or the exact user-defined label. */
@Composable
internal fun OutboundPort.displayText(): String = displayResource()?.let { stringResource(it) } ?: label
