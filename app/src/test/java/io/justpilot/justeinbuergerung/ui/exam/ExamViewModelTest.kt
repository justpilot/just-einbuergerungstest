package io.justpilot.justeinbuergerung.ui.exam

import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.SessionKind
import io.justpilot.justeinbuergerung.domain.model.SessionResult
import io.justpilot.justeinbuergerung.testing.FakeProgressRepository
import io.justpilot.justeinbuergerung.testing.FakeQuestionRepository
import io.justpilot.justeinbuergerung.testing.FakeSettingsRepository
import io.justpilot.justeinbuergerung.testing.MainDispatcherRule
import io.justpilot.justeinbuergerung.testing.question
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ExamViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val start = Instant.parse("2026-10-29T10:00:00Z")
    private val questions = FakeQuestionRepository(
        (1..40).map { question("$it") } +
            (1..10).map { question("BY-$it", Category.STATE, FederalState.BAVARIA) } +
            (1..10).map { question("BE-$it", Category.STATE, FederalState.BERLIN) },
    )
    private val progress = FakeProgressRepository()
    private val settings = FakeSettingsRepository()

    /** Follows the virtual time of the test, so the exam timer and the clock agree. */
    private fun TestScope.clock() = object : Clock() {
        override fun instant(): Instant = start.plusMillis(testScheduler.currentTime)

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this
    }

    private fun TestScope.viewModel(): ExamViewModel {
        val viewModel = ExamViewModel(questions, progress, settings, clock(), Random(1))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private val ExamViewModel.running get() = uiState.value as ExamUiState.Running

    @Test
    fun `the exam asks 33 questions of the selected state and starts with 60 minutes`() = runTest {
        val viewModel = viewModel()

        with(viewModel.running) {
            assertEquals(1, position)
            assertEquals(33, total)
            assertEquals(33, unanswered)
            assertEquals(Duration.ofMinutes(60), remaining)
        }
        viewModel.goTo(30)
        assertEquals(FederalState.BAVARIA, viewModel.running.question.state)
    }

    @Test
    fun `answers stay open until submission and nothing is recorded before`() = runTest {
        val viewModel = viewModel()

        viewModel.answer("B")
        viewModel.toggleFlag()
        viewModel.next()
        viewModel.answer("A")
        viewModel.previous()

        with(viewModel.running) {
            assertEquals(1, position)
            assertEquals("B", selectedOptionId)
            assertTrue(flagged)
            assertEquals(ExamUiState.QuestionStatus(answered = true, flagged = true), overview[0])
            assertEquals(ExamUiState.QuestionStatus(answered = true, flagged = false), overview[1])
            assertEquals(31, unanswered)
        }
        assertTrue(progress.exams.isEmpty())
        assertTrue("exam answers never go through the learning path", progress.answers.isEmpty())
    }

    @Test
    fun `submitting scores the exam once and records it`() = runTest {
        val viewModel = viewModel()
        viewModel.answer("A")
        viewModel.next()
        viewModel.answer("C")

        viewModel.submit()
        viewModel.submit()
        viewModel.answer("A")

        val result = (viewModel.uiState.value as ExamUiState.Finished).result
        assertEquals(1, result.correct)
        assertEquals(33, result.total)
        assertEquals(listOf(result), progress.exams)
    }

    @Test
    fun `the timer counts down and submits the exam when the time is up`() = runTest {
        val viewModel = viewModel()
        viewModel.answer("A")

        advanceTimeBy(Duration.ofMinutes(10).plusMillis(1).toMillis())
        assertEquals(Duration.ofMinutes(50), viewModel.running.remaining)
        advanceTimeBy(Duration.ofSeconds(1).toMillis())
        assertEquals(Duration.ofMinutes(49).plusSeconds(59), viewModel.running.remaining)
        assertTrue(progress.exams.isEmpty())

        advanceTimeBy(Duration.ofMinutes(50).toMillis())
        runCurrent()

        val result = (viewModel.uiState.value as ExamUiState.Finished).result
        assertEquals(1, result.correct)
        assertEquals(listOf(result), progress.exams)
    }

    @Test
    fun `the history lists finished exams`() = runTest {
        val viewModel = ExamHistoryViewModel(progress)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val exam = SessionResult(SessionKind.EXAM, start, LocalDate.of(2026, 10, 29), correct = 20, total = 33)

        progress.sessionResults.value = listOf(exam)

        assertEquals(ExamHistoryUiState.Ready(listOf(exam)), viewModel.uiState.value)
    }

    @Test
    fun `nothing is selected on a fresh question`() = runTest {
        val viewModel = viewModel()
        viewModel.answer("A")
        viewModel.next()

        assertNull(viewModel.running.selectedOptionId)
        assertFalse(viewModel.running.flagged)
    }
}
