package io.justpilot.justeinbuergerung.ui.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.justpilot.justeinbuergerung.domain.exam.Exam
import io.justpilot.justeinbuergerung.domain.exam.ExamResult
import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.domain.repository.ProgressRepository
import io.justpilot.justeinbuergerung.domain.repository.QuestionRepository
import io.justpilot.justeinbuergerung.domain.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

sealed interface ExamUiState {
    data object Loading : ExamUiState

    data class Running(
        val question: Question,
        /** 1-based position within the exam. */
        val position: Int,
        /** Option chosen for [question], or null while it is unanswered. */
        val selectedOptionId: String?,
        val flagged: Boolean,
        /** Status of every exam question, in exam order. */
        val overview: List<QuestionStatus>,
        val remaining: Duration,
    ) : ExamUiState {
        val total: Int get() = overview.size
        val unanswered: Int get() = overview.count { !it.answered }
        val isFirst: Boolean get() = position == 1
        val isLast: Boolean get() = position == total
    }

    data class Finished(val result: ExamResult) : ExamUiState

    data class QuestionStatus(val answered: Boolean, val flagged: Boolean)
}

/** Runs one exam simulation; the exam is submitted automatically when the time is up. */
class ExamViewModel(
    private val questionRepository: QuestionRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val random: Random = Random.Default,
) : ViewModel() {
    private val exam = MutableStateFlow<Exam?>(null)
    private val result = MutableStateFlow<ExamResult?>(null)
    private val remaining = MutableStateFlow(Exam.DURATION)
    private var timer: Job? = null

    val uiState: StateFlow<ExamUiState> = combine(exam, result, remaining) { exam, result, remaining ->
        when {
            result != null -> ExamUiState.Finished(result)
            exam == null -> ExamUiState.Loading
            else -> ExamUiState.Running(
                question = exam.current,
                position = exam.position + 1,
                selectedOptionId = exam.answers[exam.current.id],
                flagged = exam.current.id in exam.flagged,
                overview = exam.questions.map { ExamUiState.QuestionStatus(it.id in exam.answers, it.id in exam.flagged) },
                remaining = remaining,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExamUiState.Loading)

    init {
        viewModelScope.launch {
            val state = settingsRepository.settings.first().federalState
            val pool = state?.let { questionRepository.questionPool(it).first() }.orEmpty()
            exam.value = Exam.generate(pool, random)
            timer = launch { countDown(deadline = clock.instant() + Exam.DURATION) }
        }
    }

    private suspend fun countDown(deadline: Instant) {
        while (true) {
            val left = Duration.between(clock.instant(), deadline).coerceAtLeast(Duration.ZERO)
            remaining.value = left
            if (left.isZero) break
            // Wake up right after the next full second so the display never lags behind.
            delay((left.toMillis() - 1) % 1_000 + 1)
        }
        submit()
    }

    fun answer(optionId: String) = update { it.answer(optionId) }

    fun toggleFlag() = update { it.toggleFlag() }

    fun goTo(index: Int) = update { it.goTo(index) }

    fun previous() = update { it.goTo(it.position - 1) }

    fun next() = update { it.goTo(it.position + 1) }

    /** Scores the exam and stores the result; later calls do nothing. */
    fun submit() {
        val current = exam.value ?: return
        if (result.value != null) return
        val scored = current.result()
        result.value = scored
        timer?.cancel()
        viewModelScope.launch { progressRepository.recordExam(scored) }
    }

    private fun update(change: (Exam) -> Exam) {
        if (result.value != null) return
        exam.update { it?.let(change) }
    }
}
