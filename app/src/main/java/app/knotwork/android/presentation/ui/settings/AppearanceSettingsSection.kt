package app.knotwork.android.presentation.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.knotwork.design.screens.settings.AppearanceSettingsContent

/** Displays appearance controls and applies edits to the settings repository. */
@Composable
fun AppearanceSettingsSection(viewModel: AppearanceSettingsViewModel = hiltViewModel()) {
    val value by viewModel.settings.collectAsState()
    AppearanceSettingsContent(
        source = value.source,
        preset = value.preset,
        color = Color(value.customColor),
        vivid = value.vivid,
        mode = value.mode,
        pureBlack = value.pureBlack,
        onSource = { selected -> viewModel.update { it.copy(source = selected) } },
        onPreset = { selected -> viewModel.update { it.copy(preset = selected) } },
        onChannel = { index, channel ->
            viewModel.update { old ->
                val current = Color(old.customColor)
                val updated = when (index) {
                    0 -> current.copy(red = channel)
                    1 -> current.copy(green = channel)
                    else -> current.copy(blue = channel)
                }
                old.copy(customColor = updated.toArgb())
            }
        },
        onVivid = { selected -> viewModel.update { it.copy(vivid = selected) } },
        onMode = { selected -> viewModel.update { it.copy(mode = selected) } },
        onPureBlack = { selected -> viewModel.update { it.copy(pureBlack = selected) } },
    )
}
