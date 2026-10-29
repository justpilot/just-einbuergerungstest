package io.justpilot.justeinbuergerung.ui.settings

import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.Palette
import io.justpilot.justeinbuergerung.domain.model.Settings
import io.justpilot.justeinbuergerung.domain.model.ThemeMode
import io.justpilot.justeinbuergerung.testing.FakeProgressRepository
import io.justpilot.justeinbuergerung.testing.FakeSettingsRepository
import io.justpilot.justeinbuergerung.testing.MainDispatcherRule
import io.justpilot.justeinbuergerung.ui.Appearance
import io.justpilot.justeinbuergerung.ui.AppUiState
import io.justpilot.justeinbuergerung.ui.AppViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val settings = FakeSettingsRepository(Settings(null, Language.ENGLISH, hapticsEnabled = true))
    private val progress = FakeProgressRepository()

    @Test
    fun `onboarding ends once a federal state is picked`() = runTest {
        val viewModel = AppViewModel(settings)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(AppUiState.Onboarding, viewModel.uiState.value)

        viewModel.selectFederalState(FederalState.SAXONY)
        assertEquals(AppUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun `the appearance is unknown until the settings are read`() = runTest {
        val viewModel = AppViewModel(settings)
        assertNull(viewModel.appearance.value)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.appearance.collect {} }
        assertEquals(Appearance(ThemeMode.SYSTEM, Palette.DYNAMIC), viewModel.appearance.value)

        settings.setThemeMode(ThemeMode.DARK)
        assertEquals(Appearance(ThemeMode.DARK, Palette.DYNAMIC), viewModel.appearance.value)
    }

    @Test
    fun `changes are written to the repositories`() = runTest {
        val viewModel = SettingsViewModel(settings, progress)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.settings.collect {} }

        viewModel.setFederalState(FederalState.HAMBURG)
        viewModel.setTranslationLanguage(Language.UKRAINIAN)
        viewModel.setHapticsEnabled(false)
        viewModel.setThemeMode(ThemeMode.LIGHT)
        viewModel.setPalette(Palette.BLUE)
        assertEquals(
            Settings(FederalState.HAMBURG, Language.UKRAINIAN, hapticsEnabled = false, ThemeMode.LIGHT, Palette.BLUE),
            viewModel.settings.value,
        )

        var done = false
        viewModel.resetProgress { done = true }
        assertEquals(1, progress.resetCount)
        assertTrue(done)
    }
}
