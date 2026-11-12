package app.knotwork.android.domain.repositories

import kotlinx.coroutines.flow.Flow

/** Persisted appearance choices. Unknown values are handled by their consumers. */
data class AppearanceSettings(
    val source: String = "wallpaper",
    val preset: String = "mist_blue",
    val customColor: Int = -8943361,
    val vivid: Boolean = false,
    val mode: String = "system",
    val pureBlack: Boolean = false,
)

interface AppearanceSettingsRepository {
    val settings: Flow<AppearanceSettings>
    suspend fun update(change: (AppearanceSettings) -> AppearanceSettings)
}
