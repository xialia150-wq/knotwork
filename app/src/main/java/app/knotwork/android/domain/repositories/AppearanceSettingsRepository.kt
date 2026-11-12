package app.knotwork.android.domain.repositories

import kotlinx.coroutines.flow.Flow

/** Default ARGB custom color used until the user chooses one. */
private const val DEFAULT_CUSTOM_COLOR = -8943361

/**
 * Persisted appearance choices. Unknown values are handled by their consumers.
 *
 * @property source Wallpaper, preset, or custom color source.
 * @property preset Stable identifier of the chosen preset.
 * @property customColor Selected opaque ARGB color.
 * @property vivid Whether to use the higher chroma option.
 * @property mode System, light, or dark mode.
 * @property pureBlack Whether to use black as the dark background.
 */
data class AppearanceSettings(
    val source: String = "wallpaper",
    val preset: String = "mist_blue",
    val customColor: Int = DEFAULT_CUSTOM_COLOR,
    val vivid: Boolean = false,
    val mode: String = "system",
    val pureBlack: Boolean = false,
)

/** Reads and updates the user's appearance settings. */
interface AppearanceSettingsRepository {
    /** Current saved choices, including defaults for missing preferences. */
    val settings: Flow<AppearanceSettings>

    /** Applies [change] atomically to the latest saved choices. */
    suspend fun update(change: (AppearanceSettings) -> AppearanceSettings)
}
