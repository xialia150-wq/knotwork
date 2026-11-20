package app.knotwork.android.presentation.ui.orchestrator.presets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.knotwork.android.R
import app.knotwork.design.components.text.Text
import app.knotwork.design.icons.AppIcons
import app.knotwork.design.theme.KnotworkTheme

/**
 * Three-action speed-dial replacement for the catalog `PipelineLibraryFab`. Tapping the
 * primary FAB toggles a stack of extended-FAB rows:
 *
 *  - `+ From preset` — opens the `PresetPickerSheet`.
 *  - `+ From a sentence` — opens the one-sentence generator, which drafts a pipeline
 *    and shows it as a preview before anything is saved.
 *  - `+ New pipeline` — preserves the existing "create blank pipeline" flow.
 *
 * The expansion is local — the screen does not need to thread state across the
 * navigation graph.
 *
 * @param onNewPipeline Invoked when the user taps the New pipeline entry.
 * @param onFromPreset Invoked when the user taps the From preset entry.
 * @param onGenerateFromSentence Invoked when the user taps the From a sentence entry.
 * @param modifier Modifier applied to the dial's column.
 */
@Composable
fun PipelineLibrarySpeedDial(
    onNewPipeline: () -> Unit,
    onFromPreset: () -> Unit,
    onGenerateFromSentence: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.testTag(tag = LIBRARY_SPEED_DIAL_TEST_TAG),
        verticalArrangement = Arrangement.spacedBy(KnotworkTheme.spacing.sp2),
        horizontalAlignment = androidx.compose.ui.Alignment.End,
    ) {
        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(KnotworkTheme.spacing.sp2),
                horizontalAlignment = androidx.compose.ui.Alignment.End,
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        expanded = false
                        onFromPreset()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(AppIcons.Bookmark, contentDescription = null) },
                    text = { Text(stringResource(R.string.orchestrator_library_speed_dial_from_preset)) },
                    modifier = Modifier.testTag(tag = SPEED_DIAL_FROM_PRESET_TEST_TAG),
                )
                ExtendedFloatingActionButton(
                    onClick = {
                        expanded = false
                        onGenerateFromSentence()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(AppIcons.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.orchestrator_library_speed_dial_from_sentence)) },
                    modifier = Modifier.testTag(tag = SPEED_DIAL_FROM_SENTENCE_TEST_TAG),
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = {
                if (expanded) {
                    expanded = false
                    onNewPipeline()
                } else {
                    expanded = true
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(AppIcons.Add, contentDescription = null) },
            text = {
                Text(
                    if (expanded) {
                        stringResource(R.string.orchestrator_library_speed_dial_new_pipeline)
                    } else {
                        stringResource(R.string.orchestrator_library_speed_dial_new)
                    },
                )
            },
            modifier = Modifier.testTag(tag = SPEED_DIAL_NEW_TEST_TAG),
        )
    }
}

/** Test-tag applied to the speed-dial root. */
internal const val LIBRARY_SPEED_DIAL_TEST_TAG = "library_speed_dial"

/** Test-tag applied to the primary "New" FAB. */
internal const val SPEED_DIAL_NEW_TEST_TAG = "library_speed_dial_new"

/** Test-tag applied to the secondary "From preset" FAB (visible when expanded). */
internal const val SPEED_DIAL_FROM_PRESET_TEST_TAG = "library_speed_dial_from_preset"

/** Test-tag applied to the "From a sentence" FAB (visible when expanded). */
internal const val SPEED_DIAL_FROM_SENTENCE_TEST_TAG = "library_speed_dial_from_sentence"
