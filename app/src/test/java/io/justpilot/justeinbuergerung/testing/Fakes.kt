package io.justpilot.justeinbuergerung.testing

import io.justpilot.justeinbuergerung.domain.model.AnswerSource
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.Progress
import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.domain.model.Settings
import io.justpilot.justeinbuergerung.domain.repository.ProgressRepository
import io.justpilot.justeinbuergerung.domain.repository.QuestionRepository
import io.justpilot.justeinbuergerung.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class FakeQuestionRepository(questions: List<Question> = emptyList()) : QuestionRepository {
    val questions = MutableStateFlow(questions)

    override fun questionPool(state: FederalState): Flow<List<Question>> =
        questions.map { all -> all.filter { it.state == null || it.state == state } }

    override suspend fun questions(ids: Collection<String>): List<Question> = questions.value.filter { it.id in ids }
}

/** Keeps progress in memory; records answers instead of scheduling them. */
class FakeProgressRepository(progress: Map<String, Progress> = emptyMap()) : ProgressRepository {
    data class Answer(val questionId: String, val correct: Boolean, val source: AnswerSource)

    val progress = MutableStateFlow(progress)
    val answers = mutableListOf<Answer>()
    var resetCount = 0

    override fun progress(): Flow<Map<String, Progress>> = progress

    override suspend fun recordAnswer(questionId: String, correct: Boolean, source: AnswerSource) {
        answers += Answer(questionId, correct, source)
    }

    override suspend fun setBookmarked(questionId: String, bookmarked: Boolean) {
        progress.update { current ->
            val entry = current[questionId] ?: Progress(questionId, 1, null, null, bookmarked = false)
            current + (questionId to entry.copy(bookmarked = bookmarked))
        }
    }

    override fun answersPerDay(from: LocalDate): Flow<Map<LocalDate, Int>> = MutableStateFlow(emptyMap())

    override suspend fun reset() {
        resetCount++
        progress.value = emptyMap()
    }
}

class FakeSettingsRepository(
    initial: Settings = Settings(FederalState.BAVARIA, Language.ENGLISH, hapticsEnabled = true),
) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun setFederalState(state: FederalState) = settings.update { it.copy(federalState = state) }

    override suspend fun setTranslationLanguage(language: Language) = settings.update { it.copy(translationLanguage = language) }

    override suspend fun setHapticsEnabled(enabled: Boolean) = settings.update { it.copy(hapticsEnabled = enabled) }
}
