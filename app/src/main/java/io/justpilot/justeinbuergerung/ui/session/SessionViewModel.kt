package io.justpilot.justeinbuergerung.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.justpilot.justeinbuergerung.domain.learning.LearningSession
import io.justpilot.justeinbuergerung.domain.learning.QuestionSelector
import io.justpilot.justeinbuergerung.domain.learning.SessionBalance
import io.justpilot.justeinbuergerung.domain.learning.SessionSelection
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.domain.repository.ProgressRepository
import io.justpilot.justeinbuergerung.domain.repository.QuestionRepository
import io.justpilot.justeinbuergerung.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import kotlin.random.Random

sealed interface SessionUiState {
    data object Loading : SessionUiState

    data class Asking(
        val question: Question,
        /** 1-based position within the current round. */
        val position: Int,
        val roundSize: Int,
        val isRetryRound: Boolean,
        /** Option the user picked, or null while the question is unanswered. */
        val selectedOptionId: String?,
        val isLastQuestion: Boolean,
        val showTranslation: Boolean,
        val translationLanguage: Language,
        val bookmarked: Boolean,
        val hapticsEnabled: Boolean,
    ) : SessionUiState

    data class Finished(val correctFirstTime: Int, val total: Int, val balance: SessionBalance = SessionBalance()) : SessionUiState
}

class SessionViewModel(
    private val selection: SessionSelection,
    private val questionRepository: QuestionRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val random: Random = Random.Default,
) : ViewModel() {
    private val session = MutableStateFlow<LearningSession?>(null)

    /** Box of each session question when the session started, null for new questions. */
    private var startBoxes: Map<String, Int?> = emptyMap()

    // Stays on across questions once the user asked for it.
    private val showTranslation = MutableStateFlow(false)

    val uiState: StateFlow<SessionUiState> = combine(
        session,
        showTranslation,
        settingsRepository.settings,
        progressRepository.progress(),
    ) { session, showTranslation, settings, progress ->
        when {
            session == null -> SessionUiState.Loading
            session.isFinished -> SessionUiState.Finished(
                session.correctFirstTime.size,
                session.size,
                SessionBalance.of(startBoxes, session.correctFirstTime),
            )
            else -> {
                val question = session.current!!
                SessionUiState.Asking(
                    question = question,
                    position = session.position + 1,
                    roundSize = session.round.size,
                    isRetryRound = session.isRetryRound,
                    selectedOptionId = session.selectedOptionId,
                    // Last of its round and no retry round follows (a wrong answer here would start one).
                    isLastQuestion = session.position == session.round.lastIndex && session.wrongInRound.isEmpty(),
                    showTranslation = showTranslation,
                    translationLanguage = settings.translationLanguage,
                    bookmarked = progress[question.id]?.bookmarked == true,
                    hapticsEnabled = settings.hapticsEnabled,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState.Loading)

    init {
        viewModelScope.launch {
            val state = settingsRepository.settings.first().federalState
            val pool = state?.let { questionRepository.questionPool(it).first() }.orEmpty()
            val progress = progressRepository.progress().first()
            val questions = QuestionSelector.select(selection, pool, progress, LocalDate.now(clock), random)
            startBoxes = questions.associate { question -> question.id to progress[question.id]?.takeIf { it.lastAnsweredAt != null }?.box }
            session.value = LearningSession(questions)
        }
    }

    fun answer(optionId: String) {
        val current = session.value?.takeUnless { it.isFinished || it.isAnswered } ?: return
        val question = current.current!!
        session.value = current.answer(optionId)
        if (current.isFirstAttempt) {
            viewModelScope.launch { progressRepository.recordAnswer(question.id, question.isCorrect(optionId), selection.source) }
        }
    }

    fun next() {
        val current = session.value?.takeIf { it.isAnswered } ?: return
        val next = current.next()
        session.value = next
        if (next.isFinished && selection == SessionSelection.Daily) {
            viewModelScope.launch { progressRepository.recordDailySession(next.correctFirstTime.size, next.size) }
        }
    }

    fun toggleTranslation() {
        showTranslation.update { !it }
    }

    fun toggleBookmark() {
        val state = uiState.value as? SessionUiState.Asking ?: return
        viewModelScope.launch { progressRepository.setBookmarked(state.question.id, !state.bookmarked) }
    }
}
