package io.justpilot.justeinbuergerung.domain.repository

import io.justpilot.justeinbuergerung.domain.model.AnswerSource
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.Progress
import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface QuestionRepository {
    /** The 310 questions of the active pool: all general questions plus those of [state]. */
    fun questionPool(state: FederalState): Flow<List<Question>>

    suspend fun questions(ids: Collection<String>): List<Question>
}

interface ProgressRepository {
    /** Progress of every question that was answered or bookmarked, keyed by question id. */
    fun progress(): Flow<Map<String, Progress>>

    /** Logs the answer and, unless it was given in an exam, moves the question between Leitner boxes. */
    suspend fun recordAnswer(questionId: String, correct: Boolean, source: AnswerSource)

    suspend fun setBookmarked(questionId: String, bookmarked: Boolean)

    /** Number of answers per local day from [from] to today; days without answers are absent. */
    fun answersPerDay(from: LocalDate): Flow<Map<LocalDate, Int>>

    /** Deletes all learning progress and the answer log. */
    suspend fun reset()
}

interface SettingsRepository {
    val settings: Flow<Settings>

    suspend fun setFederalState(state: FederalState)

    suspend fun setTranslationLanguage(language: Language)

    suspend fun setHapticsEnabled(enabled: Boolean)
}
