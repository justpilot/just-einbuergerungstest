package io.justpilot.justeinbuergerung.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.justpilot.justeinbuergerung.domain.learning.LearningOverview
import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.FederalState
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

sealed interface CatalogUiState {
    data object Loading : CatalogUiState

    data class Ready(
        val state: FederalState,
        /** One entry per topic area, in [Category] order. */
        val topics: Map<Category, LearningOverview>,
        val mistakes: Int,
        val bookmarks: Int,
    ) : CatalogUiState
}

class CatalogViewModel(
    questionRepository: QuestionRepository,
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {
    val uiState: StateFlow<CatalogUiState> = combine(
        settingsRepository.settings,
        activePool(settingsRepository, questionRepository),
        progressRepository.progress(),
    ) { settings, pool, progress ->
        val state = settings.federalState
        if (state == null || pool.isEmpty()) return@combine CatalogUiState.Loading
        val today = LocalDate.now(clock)
        val all = LearningOverview.of(pool, progress, today)
        CatalogUiState.Ready(
            state = state,
            topics = Category.entries.associateWith { category ->
                LearningOverview.of(pool.filter { it.category == category }, progress, today)
            },
            mistakes = all.mistakes,
            bookmarks = all.bookmarked,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState.Loading)
}
