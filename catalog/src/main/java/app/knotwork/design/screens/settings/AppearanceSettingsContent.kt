package app.knotwork.design.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.knotwork.design.R
import app.knotwork.design.tokens.themePresets

private const val PRESETS_PER_ROW = 3
private const val RGB_CHANNEL_MAX = 255

/** Stateless appearance controls, shared by the app and screenshot tests. */
@Composable
fun AppearanceSettingsContent(
    source: String,
    preset: String,
    color: Color,
    vivid: Boolean,
    mode: String,
    pureBlack: Boolean,
    onSource: (String) -> Unit,
    onPreset: (String) -> Unit,
    onChannel: (Int, Float) -> Unit,
    onVivid: (Boolean) -> Unit,
    onMode: (String) -> Unit,
    onPureBlack: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider()
        Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.appearance_source))
        ChoiceRow(
            listOf(
                "wallpaper" to stringResource(R.string.appearance_wallpaper),
                "preset" to stringResource(R.string.appearance_presets),
                "custom" to stringResource(R.string.appearance_custom),
            ),
            source,
            onSource,
        )
        if (source == "preset") {
            themePresets.groupBy { it.group }.forEach { (group, presets) ->
                Text(stringResource(group), style = MaterialTheme.typography.labelMedium)
                presets.chunked(PRESETS_PER_ROW).forEach { row ->
                    ChoiceRow(row.map { it.id to stringResource(it.label) }, preset, onPreset)
                }
            }
        }
        if (source == "custom") {
            listOf(
                R.string.appearance_red_channel to color.red,
                R.string.appearance_green_channel to color.green,
                R.string.appearance_blue_channel to color.blue,
            ).forEachIndexed { index, (label, component) ->
                Text(stringResource(label, (component * RGB_CHANNEL_MAX).toInt()))
                Slider(value = component, onValueChange = { onChannel(index, it) })
            }
        }
        Text(stringResource(R.string.appearance_intensity))
        ChoiceRow(
            listOf(
                "quiet" to stringResource(R.string.appearance_quiet),
                "vivid" to stringResource(R.string.appearance_vivid),
            ),
            if (vivid) "vivid" else "quiet",
        ) { onVivid(it == "vivid") }
        Text(stringResource(R.string.appearance_mode))
        ChoiceRow(
            listOf(
                "system" to stringResource(R.string.appearance_system),
                "light" to stringResource(R.string.appearance_light),
                "dark" to stringResource(R.string.appearance_dark),
            ),
            mode,
            onMode,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.appearance_black))
            Switch(checked = pureBlack, onCheckedChange = onPureBlack)
        }
    }
}

@Composable
private fun ChoiceRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        options.forEach { (id, label) ->
            TextButton(onClick = { onSelect(id) }, modifier = Modifier.weight(1f)) {
                Text(if (id == selected) stringResource(R.string.appearance_selected, label) else label)
            }
        }
    }
}
