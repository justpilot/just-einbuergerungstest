package io.justpilot.justeinbuergerung.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.justpilot.justeinbuergerung.domain.learning.Activity
import io.justpilot.justeinbuergerung.domain.learning.LearningOverview
import io.justpilot.justeinbuergerung.domain.model.FederalState
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
import java.time.LocalTime

sealed interface HomeUiState {
    /** Settings or the catalog import are not ready yet. */
    data object Loading : HomeUiState

    data class Ready(
        val overview: LearningOverview,
        val activity: Activity,
        val federalState: FederalState,
        val greeting: Greeting,
    ) : HomeUiState
}

/** Greeting at the top of Home, by the time of day. */
enum class Greeting {
    MORNING,
    DAY,
    EVENING,
    ;

    companion object {
        fun of(time: LocalTime): Greeting = when (time.hour) {
            in 4..10 -> MORNING
            in 11..17 -> DAY
            else -> EVENING
        }
    }
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
            settingsRepository.settings,
        ) { pool, progress, answersPerDay, dailyDays, settings ->
            val state = settings.federalState
            if (pool.isEmpty() || state == null) HomeUiState.Loading
            else HomeUiState.Ready(
                overview = LearningOverview.of(pool, progress, today),
                activity = Activity.of(answersPerDay, dailyDays, today),
                federalState = state,
                // Taken whenever the data changes; an hour-old greeting is fine.
                greeting = Greeting.of(LocalTime.now(clock)),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)
}
