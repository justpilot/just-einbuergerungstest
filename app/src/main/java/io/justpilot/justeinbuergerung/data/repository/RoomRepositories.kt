package io.justpilot.justeinbuergerung.data.repository

import androidx.room.withTransaction
import io.justpilot.justeinbuergerung.data.db.AnswerLogEntity
import io.justpilot.justeinbuergerung.data.db.AppDatabase
import io.justpilot.justeinbuergerung.data.db.ProgressEntity
import io.justpilot.justeinbuergerung.data.db.SessionResultEntity
import io.justpilot.justeinbuergerung.data.db.toDomain
import io.justpilot.justeinbuergerung.domain.exam.ExamResult
import io.justpilot.justeinbuergerung.domain.learning.LeitnerScheduler
import io.justpilot.justeinbuergerung.domain.model.AnswerSource
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Progress
import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.domain.model.SessionKind
import io.justpilot.justeinbuergerung.domain.model.SessionResult
import io.justpilot.justeinbuergerung.domain.repository.ProgressRepository
import io.justpilot.justeinbuergerung.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

class RoomQuestionRepository(private val database: AppDatabase) : QuestionRepository {
    override fun questionPool(state: FederalState): Flow<List<Question>> =
        database.questionDao().observePool(state.code).map { rows -> rows.map { it.toDomain() } }

    override suspend fun questions(ids: Collection<String>): List<Question> =
        database.questionDao().byIds(ids).map { it.toDomain() }
}

class RoomProgressRepository(
    private val database: AppDatabase,
    private val clock: Clock,
) : ProgressRepository {
    private val progressDao = database.progressDao()
    private val answerLogDao = database.answerLogDao()
    private val sessionResultDao = database.sessionResultDao()

    override fun progress(): Flow<Map<String, Progress>> =
        progressDao.observeAll().map { rows -> rows.associate { it.questionId to it.toDomain() } }

    override suspend fun recordAnswer(questionId: String, correct: Boolean, source: AnswerSource) {
        val now = clock.instant()
        val today = LocalDate.now(clock)
        database.withTransaction {
            answerLogDao.insert(
                AnswerLogEntity(
                    questionId = questionId,
                    answeredAt = now.toEpochMilli(),
                    answeredEpochDay = today.toEpochDay(),
                    correct = correct,
                    source = source.name,
                ),
            )
            // An exam is a test of knowledge, not a learning step; it must not reorder the boxes.
            if (source == AnswerSource.EXAM) return@withTransaction

            val current = progressDao.get(questionId)
            val answeredBefore = current?.lastAnsweredAt != null
            val schedule = LeitnerScheduler.schedule(current?.box?.takeIf { answeredBefore }, correct, today)
            progressDao.upsert(
                ProgressEntity(
                    questionId = questionId,
                    box = schedule.box,
                    dueEpochDay = schedule.dueDate.toEpochDay(),
                    lastAnsweredAt = now.toEpochMilli(),
                    bookmarked = current?.bookmarked ?: false,
                ),
            )
        }
    }

    override suspend fun recordExam(result: ExamResult) {
        val now = clock.instant()
        val today = LocalDate.now(clock)
        database.withTransaction {
            result.answers.filter { it.chosenOptionId != null }.forEach { answer ->
                answerLogDao.insert(
                    AnswerLogEntity(
                        questionId = answer.question.id,
                        answeredAt = now.toEpochMilli(),
                        answeredEpochDay = today.toEpochDay(),
                        correct = answer.correct,
                        source = AnswerSource.EXAM.name,
                    ),
                )
            }
            sessionResultDao.insert(sessionResult(SessionKind.EXAM, result.correct, result.total))
        }
    }

    override suspend fun recordDailySession(correct: Int, total: Int) {
        sessionResultDao.insert(sessionResult(SessionKind.DAILY, correct, total))
    }

    private fun sessionResult(kind: SessionKind, correct: Int, total: Int) = SessionResultEntity(
        kind = kind.name,
        finishedAt = clock.instant().toEpochMilli(),
        finishedEpochDay = LocalDate.now(clock).toEpochDay(),
        correct = correct,
        total = total,
    )

    override fun sessionResults(kind: SessionKind): Flow<List<SessionResult>> =
        sessionResultDao.observe(kind.name).map { rows -> rows.map { it.toDomain() } }

    override suspend fun setBookmarked(questionId: String, bookmarked: Boolean) {
        database.withTransaction {
            val current = progressDao.get(questionId)
                ?: ProgressEntity(questionId, LeitnerScheduler.FIRST_BOX, null, null, bookmarked = false)
            progressDao.upsert(current.copy(bookmarked = bookmarked))
        }
    }

    override fun answersPerDay(from: LocalDate): Flow<Map<LocalDate, Int>> =
        answerLogDao.observeCountsPerDay(from.toEpochDay())
            .map { rows -> rows.associate { LocalDate.ofEpochDay(it.day) to it.count } }

    override suspend fun reset() {
        database.withTransaction {
            progressDao.deleteAll()
            answerLogDao.deleteAll()
            sessionResultDao.deleteAll()
        }
    }
}
