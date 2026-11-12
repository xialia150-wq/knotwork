package app.knotwork.android.data.local.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.knotwork.android.domain.repositories.AppearanceSettings
import app.knotwork.android.domain.repositories.AppearanceSettingsRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the appearance settings in the shared preferences DataStore. */
@Singleton
class AppearancePreferencesStore @Inject constructor(private val dataStore: DataStore<Preferences>) : AppearanceSettingsRepository {
    private object Keys {
        val SOURCE = stringPreferencesKey("appearance_source")
        val PRESET = stringPreferencesKey("appearance_preset")
        val CUSTOM = intPreferencesKey("appearance_custom_color")
        val VIVID = booleanPreferencesKey("appearance_vivid")
        val MODE = stringPreferencesKey("appearance_mode")
        val BLACK = booleanPreferencesKey("appearance_pure_black")
    }

    companion object {
        internal fun reset(preferences: MutablePreferences) {
            val defaults = AppearanceSettings()
            preferences[Keys.SOURCE] = defaults.source
            preferences[Keys.PRESET] = defaults.preset
            preferences[Keys.CUSTOM] = defaults.customColor
            preferences[Keys.VIVID] = defaults.vivid
            preferences[Keys.MODE] = defaults.mode
            preferences[Keys.BLACK] = defaults.pureBlack
        }
    }

    override val settings = dataStore.preferencesOrEmpty().map { p ->
        val defaults = AppearanceSettings()
        AppearanceSettings(
            source = p[Keys.SOURCE] ?: defaults.source,
            preset = p[Keys.PRESET] ?: defaults.preset,
            customColor = p[Keys.CUSTOM] ?: defaults.customColor,
            vivid = p[Keys.VIVID] ?: defaults.vivid,
            mode = p[Keys.MODE] ?: defaults.mode,
            pureBlack = p[Keys.BLACK] ?: defaults.pureBlack,
        )
    }

    override suspend fun update(change: (AppearanceSettings) -> AppearanceSettings) {
        dataStore.edit { p ->
            val current = AppearanceSettings(
                p[Keys.SOURCE] ?: "wallpaper", p[Keys.PRESET] ?: "mist_blue", p[Keys.CUSTOM] ?: -8943361,
                p[Keys.VIVID] ?: false, p[Keys.MODE] ?: "system", p[Keys.BLACK] ?: false,
            )
            val next = change(current)
            p[Keys.SOURCE] = next.source
            p[Keys.PRESET] = next.preset
            p[Keys.CUSTOM] = next.customColor
            p[Keys.VIVID] = next.vivid
            p[Keys.MODE] = next.mode
            p[Keys.BLACK] = next.pureBlack
        }
    }
}
