package io.justpilot.justeinbuergerung.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Palette
import io.justpilot.justeinbuergerung.domain.model.ThemeMode
import io.justpilot.justeinbuergerung.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppUiState {
    data object Loading : AppUiState

    /** No federal state was picked yet, so the question pool is unknown. */
    data object Onboarding : AppUiState

    data object Ready : AppUiState
}

data class Appearance(val themeMode: ThemeMode, val palette: Palette)

class AppViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<AppUiState> = settingsRepository.settings
        .map { if (it.federalState == null) AppUiState.Onboarding else AppUiState.Ready }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState.Loading)

    /** Null until the settings are read, so a chosen light or dark mode is not reset meanwhile. */
    val appearance: StateFlow<Appearance?> = settingsRepository.settings
        .map { Appearance(it.themeMode, it.palette) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectFederalState(state: FederalState) {
        viewModelScope.launch { settingsRepository.setFederalState(state) }
    }
}
