package app.knotwork.android.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.knotwork.android.data.local.settings.AppearancePreferencesStore
import app.knotwork.android.domain.repositories.AppearanceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Verifies default appearance values and persistence. */
class AppearancePreferencesStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun defaultsAndUpdatesSurviveNewStoreInstances() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val data = PreferenceDataStoreFactory.create(scope = scope, produceFile = { folder.newFile("appearance.preferences_pb") })
            val store = AppearancePreferencesStore(data)
            assertEquals(AppearanceSettings(), store.settings.first())
            val updated = AppearanceSettings(source = "custom", preset = "blue", customColor = -12, vivid = true, mode = "dark", pureBlack = true)
            store.update { updated }
            assertEquals(updated, AppearancePreferencesStore(data).settings.first())
        } finally {
            scope.cancel()
        }
    }
}
