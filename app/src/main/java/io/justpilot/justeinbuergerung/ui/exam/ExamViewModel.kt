package io.justpilot.justeinbuergerung.ui.exam

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.map
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

    /** @param bookmarked ids of the bookmarked questions, so mistakes can be bookmarked from the result */
    data class Finished(val result: ExamResult, val bookmarked: Set<String> = emptySet()) : ExamUiState

    data class QuestionStatus(val answered: Boolean, val flagged: Boolean)
}

/**
 * Runs one exam simulation; the exam is submitted automatically when the time is up.
 *
 * The exam lives in [savedState], so it survives the system ending the process while the app is
 * in the background. The deadline is stored too: the time keeps running meanwhile, as in a real test.
 */
class ExamViewModel(
    private val savedState: SavedStateHandle,
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

    private val bookmarked = progressRepository.progress().map { progress -> progress.filterValues { it.bookmarked }.keys }

    val uiState: StateFlow<ExamUiState> = combine(exam, result, remaining, bookmarked) { exam, result, remaining, bookmarked ->
        when {
            result != null -> ExamUiState.Finished(result, bookmarked)
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
            val restored = restore(pool)
            val started = restored ?: Exam.generate(pool, random)
            val deadline = savedState.get<Long>(DEADLINE)?.takeIf { restored != null }?.let(Instant::ofEpochMilli)
                ?: (clock.instant() + Exam.DURATION)
            savedState[DEADLINE] = deadline.toEpochMilli()
            exam.value = started
            save(started)
            if (savedState.get<Boolean>(SUBMITTED) == true) {
                // Already scored and recorded before the process ended; only show the result again.
                result.value = started.result()
            } else {
                timer = launch { countDown(deadline) }
            }
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
        savedState[SUBMITTED] = true
        timer?.cancel()
        viewModelScope.launch { progressRepository.recordExam(scored) }
    }

    fun toggleBookmark(questionId: String) {
        val finished = uiState.value as? ExamUiState.Finished ?: return
        viewModelScope.launch { progressRepository.setBookmarked(questionId, questionId !in finished.bookmarked) }
    }

    private fun update(change: (Exam) -> Exam) {
        if (result.value != null) return
        exam.update { it?.let(change) }
        exam.value?.let(::save)
    }

    private fun save(exam: Exam) {
        savedState[QUESTIONS] = ArrayList(exam.questions.map { it.id })
        savedState[ANSWERS] = ArrayList(exam.answers.map { (id, option) -> "$id=$option" })
        savedState[FLAGGED] = ArrayList(exam.flagged)
        savedState[POSITION] = exam.position
    }

    /** The saved exam, or null if there is none or its questions are no longer in the pool. */
    private fun restore(pool: List<Question>): Exam? {
        val ids = savedState.get<List<String>>(QUESTIONS) ?: return null
        val byId = pool.associateBy { it.id }
        val questions = ids.map { byId[it] ?: return null }
        if (questions.isEmpty()) return null
        return Exam(
            questions = questions,
            position = (savedState.get<Int>(POSITION) ?: 0).coerceIn(questions.indices),
            answers = savedState.get<List<String>>(ANSWERS).orEmpty().associate { it.substringBefore('=') to it.substringAfter('=') },
            flagged = savedState.get<List<String>>(FLAGGED).orEmpty().toSet(),
        )
    }

    private companion object {
        const val QUESTIONS = "exam_questions"
        const val ANSWERS = "exam_answers"
        const val FLAGGED = "exam_flagged"
        const val POSITION = "exam_position"
        const val DEADLINE = "exam_deadline"
        const val SUBMITTED = "exam_submitted"
    }
}
