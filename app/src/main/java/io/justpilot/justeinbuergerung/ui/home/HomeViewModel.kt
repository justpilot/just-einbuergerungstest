package io.justpilot.justeinbuergerung.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.justpilot.justeinbuergerung.domain.learning.Activity
import io.justpilot.justeinbuergerung.domain.learning.LearningOverview
import io.justpilot.justeinbuergerung.domain.model.SessionKind
import io.justpilot.justeinbuergerung.domain.repository.ProgressRepository
import io.justpilot.justeinbuergerung.domain.repository.QuestionRepository
import io.justpilot.justeinbuergerung.domain.repository.SettingsRepository
import io.justpilot.justeinbuergerung.ui.common.activePool
import io.justpilot.justeinbuergerung.ui.common.currentDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

sealed interface HomeUiState {
    /** Settings or the catalog import are not ready yet. */
    data object Loading : HomeUiState

    data class Ready(val overview: LearningOverview, val activity: Activity) : HomeUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    questionRepository: QuestionRepository,
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    clock: Clock,
) : ViewModel() {
    // The screen stays alive across midnight, so "today" has to move on with the clock.
    val uiState: StateFlow<HomeUiState> = currentDay(clock).flatMapLatest { today ->
        combine(
            activePool(settingsRepository, questionRepository),
            progressRepository.progress(),
            progressRepository.answersPerDay(today.minusDays(Activity.DAYS - 1L)),
            progressRepository.sessionResults(SessionKind.DAILY).map { results -> results.map { it.day }.toSet() },
        ) { pool, progress, answersPerDay, dailyDays ->
            if (pool.isEmpty()) HomeUiState.Loading
            else HomeUiState.Ready(LearningOverview.of(pool, progress, today), Activity.of(answersPerDay, dailyDays, today))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)
}
