package io.justpilot.justeinbuergerung.ui.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.justpilot.justeinbuergerung.domain.model.SessionKind
import io.justpilot.justeinbuergerung.domain.model.SessionResult
import io.justpilot.justeinbuergerung.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface ExamHistoryUiState {
    data object Loading : ExamHistoryUiState

    /** @param results finished exams, newest first */
    data class Ready(val results: List<SessionResult>) : ExamHistoryUiState
}

class ExamHistoryViewModel(progressRepository: ProgressRepository) : ViewModel() {
    val uiState: StateFlow<ExamHistoryUiState> = progressRepository.sessionResults(SessionKind.EXAM)
        .map<List<SessionResult>, ExamHistoryUiState> { ExamHistoryUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExamHistoryUiState.Loading)
}
