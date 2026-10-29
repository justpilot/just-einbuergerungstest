package io.justpilot.justeinbuergerung.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.justpilot.justeinbuergerung.data.db.AppDatabase
import io.justpilot.justeinbuergerung.domain.exam.ExamResult
import io.justpilot.justeinbuergerung.domain.model.AnswerOption
import io.justpilot.justeinbuergerung.domain.model.AnswerSource
import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.LocalizedText
import io.justpilot.justeinbuergerung.domain.model.Progress
import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.domain.model.SessionKind
import io.justpilot.justeinbuergerung.domain.model.SessionResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RoomProgressRepositoryTest {
    private val berlin: ZoneId = ZoneId.of("Europe/Berlin")
    private val today = LocalDate.of(2026, 10, 29)
    private var now: Instant = today.atTime(12, 0).atZone(berlin).toInstant()
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomProgressRepository

    /** A clock the test can move forward. */
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = berlin
        override fun withZone(zone: ZoneId?): Clock = throw UnsupportedOperationException()
        override fun instant(): Instant = now
    }

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = RoomProgressRepository(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun progress(id: String): Progress? = repository.progress().first()[id]

    @Test
    fun answersMoveQuestionsThroughTheBoxes() = runTest {
        repository.recordAnswer("1", correct = true, AnswerSource.DAILY)
        assertEquals(2, progress("1")?.box)
        assertEquals(today.plusDays(2), progress("1")?.dueDate)

        repository.recordAnswer("1", correct = true, AnswerSource.DAILY)
        assertEquals(3, progress("1")?.box)

        repository.recordAnswer("1", correct = false, AnswerSource.CATALOG)
        assertEquals(1, progress("1")?.box)
        assertEquals(today, progress("1")?.dueDate)
        assertEquals(now, progress("1")?.lastAnsweredAt)
    }

    @Test
    fun examAnswersAreLoggedButDoNotChangeProgress() = runTest {
        repository.recordAnswer("1", correct = true, AnswerSource.EXAM)

        assertNull(progress("1"))
        assertEquals(mapOf(today to 1), repository.answersPerDay(today).first())
    }

    @Test
    fun examsStoreTheirScoreAndLogOnlyAnsweredQuestions() = runTest {
        fun question(id: String) = Question(
            id, 1, null, Category.POLITICS, null, LocalizedText("?", null, null),
            listOf("A", "B").map { AnswerOption(it, LocalizedText(it, null, null)) }, correctOptionId = "A",
        )
        val first = ExamResult(
            total = 3,
            answers = listOf(
                ExamResult.Answer(question("1"), "A"),
                ExamResult.Answer(question("2"), "B"),
                ExamResult.Answer(question("3"), null),
            ),
        )

        repository.recordExam(first)
        now = now.plusSeconds(3600)
        repository.recordExam(first.copy(answers = first.answers.take(1)))

        assertEquals(
            listOf(
                SessionResult(SessionKind.EXAM, now, today, correct = 1, total = 3),
                SessionResult(SessionKind.EXAM, now.minusSeconds(3600), today, correct = 1, total = 3),
            ),
            repository.sessionResults(SessionKind.EXAM).first(),
        )
        assertEquals(mapOf(today to 3), repository.answersPerDay(today).first())
        assertEquals(emptyMap<String, Progress>(), repository.progress().first())
    }

    @Test
    fun bookmarkingKeepsLearningStateAndUnansweredQuestionsCountAsNew() = runTest {
        repository.setBookmarked("1", true)
        assertEquals(Progress("1", box = 1, dueDate = null, lastAnsweredAt = null, bookmarked = true), progress("1"))

        // A bookmarked but never answered question is still new: a correct answer moves it to box 2.
        repository.recordAnswer("1", correct = true, AnswerSource.DAILY)
        assertEquals(2, progress("1")?.box)
        assertTrue(progress("1")!!.bookmarked)

        repository.setBookmarked("1", false)
        assertEquals(2, progress("1")?.box)
    }

    @Test
    fun answersAreCountedPerLocalDay() = runTest {
        repository.recordAnswer("1", correct = true, AnswerSource.DAILY)
        repository.recordAnswer("2", correct = false, AnswerSource.DAILY)
        // Days follow the local time zone: 23:30 still counts for today, 00:30 for tomorrow.
        now = today.atTime(23, 30).atZone(berlin).toInstant()
        repository.recordAnswer("3", correct = true, AnswerSource.DAILY)
        now = today.plusDays(1).atTime(0, 30).atZone(berlin).toInstant()
        repository.recordAnswer("4", correct = true, AnswerSource.DAILY)

        assertEquals(mapOf(today to 3, today.plusDays(1) to 1), repository.answersPerDay(today.minusDays(6)).first())
        assertEquals(mapOf(today.plusDays(1) to 1), repository.answersPerDay(today.plusDays(1)).first())
    }

    @Test
    fun resetDeletesProgressLogAndResults() = runTest {
        repository.recordAnswer("1", correct = true, AnswerSource.DAILY)
        repository.setBookmarked("2", true)
        repository.recordExam(ExamResult(total = 33, answers = emptyList()))

        repository.reset()

        assertEquals(emptyMap<String, Progress>(), repository.progress().first())
        assertEquals(emptyMap<LocalDate, Int>(), repository.answersPerDay(LocalDate.of(2000, 1, 1)).first())
        assertEquals(emptyList<SessionResult>(), repository.sessionResults(SessionKind.EXAM).first())
    }
}
