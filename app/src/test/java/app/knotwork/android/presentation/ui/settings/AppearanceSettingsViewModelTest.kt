package app.knotwork.android.presentation.ui.settings

import app.knotwork.android.domain.repositories.AppearanceSettings
import app.knotwork.android.domain.repositories.AppearanceSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Checks local RGB previews, final saves, and delayed persistence acknowledgements. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = TestAppearanceRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `drag previews immediately and saves only its final channel value`() = runTest(dispatcher) {
        val viewModel = AppearanceSettingsViewModel(repository)
        backgroundScope.launch { viewModel.settings.collect {} }
        runCurrent()

        viewModel.previewColorChannel(0, 0.25f)
        runCurrent()
        assertEquals(0xFF403040.toInt(), viewModel.settings.value.customColor)
        viewModel.previewColorChannel(0, 0.75f)
        viewModel.previewColorChannel(1, 0.5f)
        runCurrent()
        assertEquals(0xFFBF8040.toInt(), viewModel.settings.value.customColor)
        assertEquals(INITIAL_SETTINGS, repository.settings.value)
        assertEquals(0, repository.writes)

        viewModel.commitColorChannel(0)
        runCurrent()
        assertEquals(0xFFBF8040.toInt(), viewModel.settings.value.customColor)
        advanceTimeBy(SAVE_DELAY_MS)
        runCurrent()

        assertEquals(INITIAL_SETTINGS.copy(customColor = 0xFFBF3040.toInt()), repository.settings.value)
        assertEquals(0xFFBF8040.toInt(), viewModel.settings.value.customColor)
        assertEquals(1, repository.writes)
    }

    @Test
    fun `a delayed save cannot replace a newer drag preview`() = runTest(dispatcher) {
        val viewModel = AppearanceSettingsViewModel(repository)
        backgroundScope.launch { viewModel.settings.collect {} }
        runCurrent()

        viewModel.previewColorChannel(0, 0.25f)
        viewModel.commitColorChannel(0)
        runCurrent()
        viewModel.previewColorChannel(0, 0.75f)
        advanceTimeBy(SAVE_DELAY_MS)
        runCurrent()

        assertEquals(0xFF403040.toInt(), repository.settings.value.customColor)
        assertEquals(0xFFBF3040.toInt(), viewModel.settings.value.customColor)
        viewModel.commitColorChannel(0)
        runCurrent()
        advanceTimeBy(SAVE_DELAY_MS)
        runCurrent()

        assertEquals(INITIAL_SETTINGS.copy(customColor = 0xFFBF3040.toInt()), repository.settings.value)
        assertEquals(repository.settings.value, viewModel.settings.value)
    }

    private class TestAppearanceRepository : AppearanceSettingsRepository {
        override val settings = MutableStateFlow(INITIAL_SETTINGS)
        var writes = 0
            private set

        override suspend fun update(change: (AppearanceSettings) -> AppearanceSettings) {
            delay(SAVE_DELAY_MS)
            settings.value = change(settings.value)
            writes++
        }
    }

    private companion object {
        const val SAVE_DELAY_MS = 200L
        val INITIAL_SETTINGS = AppearanceSettings(source = "custom", customColor = 0xFF203040.toInt(), vivid = true)
    }
}
