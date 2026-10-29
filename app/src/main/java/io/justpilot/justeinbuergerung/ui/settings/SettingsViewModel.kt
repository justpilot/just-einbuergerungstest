package io.justpilot.justeinbuergerung.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.Settings
import io.justpilot.justeinbuergerung.domain.repository.ProgressRepository
import io.justpilot.justeinbuergerung.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {
    /** Null until the settings were read. */
    val settings: StateFlow<Settings?> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setFederalState(state: FederalState) {
        viewModelScope.launch { settingsRepository.setFederalState(state) }
    }

    fun setTranslationLanguage(language: Language) {
        viewModelScope.launch { settingsRepository.setTranslationLanguage(language) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticsEnabled(enabled) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    /** Deletes all learning progress; [onDone] runs once it is gone. */
    fun resetProgress(onDone: () -> Unit) {
        viewModelScope.launch {
            progressRepository.reset()
            onDone()
        }
    }
}
