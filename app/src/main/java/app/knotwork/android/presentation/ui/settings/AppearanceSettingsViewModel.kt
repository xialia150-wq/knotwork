package app.knotwork.android.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.knotwork.android.domain.repositories.AppearanceSettings
import app.knotwork.android.domain.repositories.AppearanceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val APPEARANCE_STOP_TIMEOUT_MS = 5_000L

/** Exposes persisted appearance choices and atomically applies edits. */
@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(private val store: AppearanceSettingsRepository) : ViewModel() {
    val settings = store.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(APPEARANCE_STOP_TIMEOUT_MS),
        AppearanceSettings(),
    )

    fun update(change: (AppearanceSettings) -> AppearanceSettings) {
        viewModelScope.launch { store.update(change) }
    }
}
