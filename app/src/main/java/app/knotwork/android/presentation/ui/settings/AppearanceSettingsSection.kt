package app.knotwork.android.presentation.ui.settings

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knotwork.android.domain.repositories.AppearanceSettings
import app.knotwork.android.domain.repositories.AppearanceSettingsRepository
import app.knotwork.design.tokens.themePresets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(private val store: AppearanceSettingsRepository) : ViewModel() {
    val settings = store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppearanceSettings())
    fun update(change: (AppearanceSettings) -> AppearanceSettings) { viewModelScope.launch { store.update(change) } }
}

@Composable
fun AppearanceSettingsSection(viewModel: AppearanceSettingsViewModel = hiltViewModel()) {
    val value by viewModel.settings.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider()
        Text("外观", style = MaterialTheme.typography.titleMedium)
        Text("配色来源")
        ChoiceRow(listOf("wallpaper" to "跟随壁纸", "preset" to "预设", "custom" to "自选"), value.source) {
            viewModel.update { old -> old.copy(source = it) }
        }
        if (value.source == "preset") {
            themePresets.groupBy { it.group }.forEach { (group, presets) ->
                Text(group, style = MaterialTheme.typography.labelMedium)
                presets.chunked(3).forEach { row ->
                    ChoiceRow(row.map { it.id to it.label }, value.preset) { id ->
                        viewModel.update { it.copy(preset = id) }
                    }
                }
            }
        }
        if (value.source == "custom") {
            val color = Color(value.customColor)
            listOf("红" to color.red, "绿" to color.green, "蓝" to color.blue).forEachIndexed { index, (label, component) ->
                Text("$label ${(component * 255).toInt()}")
                Slider(value = component, onValueChange = { channel ->
                    viewModel.update { old ->
                        val current = Color(old.customColor)
                        val updated = when (index) {
                            0 -> current.copy(red = channel)
                            1 -> current.copy(green = channel)
                            else -> current.copy(blue = channel)
                        }
                        old.copy(customColor = updated.toArgb())
                    }
                })
            }
        }
        Text("色彩浓度")
        ChoiceRow(listOf("quiet" to "安静", "vivid" to "鲜亮"), if (value.vivid) "vivid" else "quiet") { selected ->
            viewModel.update { it.copy(vivid = selected == "vivid") }
        }
        Text("深色模式")
        ChoiceRow(listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色"), value.mode) { selected ->
            viewModel.update { it.copy(mode = selected) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("纯黑")
            Switch(checked = value.pureBlack, onCheckedChange = { checked -> viewModel.update { it.copy(pureBlack = checked) } })
        }
    }
}

@Composable
private fun ChoiceRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        options.forEach { (id, label) ->
            TextButton(onClick = { onSelect(id) }, modifier = Modifier.weight(1f)) {
                Text(if (id == selected) "✓ $label" else label)
            }
        }
    }
}
