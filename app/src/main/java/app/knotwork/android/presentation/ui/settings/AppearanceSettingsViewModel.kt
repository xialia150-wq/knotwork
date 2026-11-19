package app.knotwork.android.presentation.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knotwork.android.domain.repositories.AppearanceSettings
import app.knotwork.android.domain.repositories.AppearanceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val APPEARANCE_STOP_TIMEOUT_MS = 5_000L

/** Exposes persisted appearance choices and atomically applies edits. */
@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(private val store: AppearanceSettingsRepository) : ViewModel() {
    private val persistedSettings = store.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(APPEARANCE_STOP_TIMEOUT_MS),
        AppearanceSettings(),
    )
    private val channelDrafts = MutableStateFlow<Map<Int, Float>>(emptyMap())
    private val channelCommits = mutableMapOf<Int, Job>()

    val settings = combine(persistedSettings, channelDrafts) { saved, drafts ->
        val displayedColor = drafts.entries.fold(saved.customColor) { color, (index, value) ->
            color.withChannel(index, value)
        }
        saved.copy(customColor = displayedColor)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), AppearanceSettings())

    fun previewColorChannel(index: Int, value: Float) {
        channelDrafts.update { it + (index to value) }
    }

    fun commitColorChannel(index: Int) {
        val value = channelDrafts.value[index] ?: return
        channelCommits[index]?.cancel()
        channelCommits[index] = viewModelScope.launch {
            store.update { saved -> saved.copy(customColor = saved.customColor.withChannel(index, value)) }
            // Keep the thumb at its new position while the saved value propagates.
            persistedSettings.first { it.customColor.withChannel(index, value) == it.customColor }
            channelDrafts.update { if (it[index] == value) it - index else it }
        }
    }

    fun update(change: (AppearanceSettings) -> AppearanceSettings) {
        viewModelScope.launch { store.update(change) }
    }
}

private fun Int.withChannel(index: Int, value: Float): Int {
    val color = Color(this)
    return when (index) {
        0 -> color.copy(red = value)
        1 -> color.copy(green = value)
        else -> color.copy(blue = value)
    }.toArgb()
}
