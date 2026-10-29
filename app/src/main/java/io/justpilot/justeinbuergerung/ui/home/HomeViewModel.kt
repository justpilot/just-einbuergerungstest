package io.justpilot.justeinbuergerung.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.justpilot.justeinbuergerung.domain.learning.LearningOverview
import io.justpilot.justeinbuergerung.domain.repository.ProgressRepository
import io.justpilot.justeinbuergerung.domain.repository.QuestionRepository
import io.justpilot.justeinbuergerung.domain.repository.SettingsRepository
import io.justpilot.justeinbuergerung.ui.common.activePool
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

sealed interface HomeUiState {
    /** Settings or the catalog import are not ready yet. */
    data object Loading : HomeUiState

    data class Ready(val overview: LearningOverview) : HomeUiState
}

class HomeViewModel(
    questionRepository: QuestionRepository,
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(activePool(settingsRepository, questionRepository), progressRepository.progress()) { pool, progress ->
            if (pool.isEmpty()) HomeUiState.Loading
            else HomeUiState.Ready(LearningOverview.of(pool, progress, LocalDate.now(clock)))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)
}
