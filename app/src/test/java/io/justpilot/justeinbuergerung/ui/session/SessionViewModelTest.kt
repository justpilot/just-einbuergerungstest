package io.justpilot.justeinbuergerung.ui.session

import io.justpilot.justeinbuergerung.domain.learning.SessionBalance
import io.justpilot.justeinbuergerung.domain.learning.SessionSelection
import io.justpilot.justeinbuergerung.domain.model.AnswerSource
import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.testing.FakeProgressRepository
import io.justpilot.justeinbuergerung.testing.FakeQuestionRepository
import io.justpilot.justeinbuergerung.testing.FakeSettingsRepository
import io.justpilot.justeinbuergerung.testing.MainDispatcherRule
import io.justpilot.justeinbuergerung.testing.answered
import io.justpilot.justeinbuergerung.testing.question
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val questions = FakeQuestionRepository(
        listOf(
            question("1"),
            question("2"),
            question("3", Category.HISTORY),
            question("BY-1", Category.STATE, FederalState.BAVARIA),
            question("BE-1", Category.STATE, FederalState.BERLIN),
        ),
    )
    private val progress = FakeProgressRepository()
    private val settings = FakeSettingsRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC)

    private fun TestScope.viewModel(selection: SessionSelection): SessionViewModel {
        val viewModel = SessionViewModel(selection, questions, progress, settings, clock, Random(1))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private val SessionViewModel.asking get() = uiState.value as SessionUiState.Asking

    @Test
    fun `daily session asks the questions of the selected state only`() = runTest {
        val viewModel = viewModel(SessionSelection.Daily)
        val asked = mutableListOf<String>()
        while (viewModel.uiState.value is SessionUiState.Asking) {
            asked += viewModel.asking.question.id
            viewModel.answer("A")
            viewModel.next()
        }

        assertEquals(setOf("1", "2", "3", "BY-1"), asked.toSet())
        assertEquals(SessionUiState.Finished(correctFirstTime = 4, total = 4, SessionBalance(advanced = 4)), viewModel.uiState.value)
        assertTrue(progress.answers.all { it.correct && it.source == AnswerSource.DAILY })
    }

    @Test
    fun `wrong answers are retried but only the first answer is recorded`() = runTest {
        val viewModel = viewModel(SessionSelection.Topic(Category.HISTORY))
        assertEquals("3", viewModel.asking.question.id)

        viewModel.answer("B")
        assertEquals("B", viewModel.asking.selectedOptionId)
        assertFalse("a retry round follows", viewModel.asking.isLastQuestion)
        viewModel.next()

        assertTrue(viewModel.asking.isRetryRound)
        viewModel.answer("A")
        assertTrue(viewModel.asking.isLastQuestion)
        viewModel.next()

        assertEquals(SessionUiState.Finished(correctFirstTime = 0, total = 1), viewModel.uiState.value)
        assertEquals(listOf(FakeProgressRepository.Answer("3", false, AnswerSource.CATALOG)), progress.answers)
    }

    @Test
    fun `the summary counts box moves against the boxes at session start`() = runTest {
        progress.progress.value = mapOf("1" to answered("1", box = 2, due = LocalDate.of(2026, 10, 29)), "2" to answered("2", box = 3, due = LocalDate.of(2026, 10, 29)))
        val viewModel = viewModel(SessionSelection.Topic(Category.POLITICS))
        while (viewModel.uiState.value is SessionUiState.Asking) {
            // Question 2 is answered wrongly once, then correctly in the retry round.
            val wrongFirst = viewModel.asking.question.id == "2" && !viewModel.asking.isRetryRound
            viewModel.answer(if (wrongFirst) "B" else "A")
            viewModel.next()
        }

        assertEquals(SessionBalance(mastered = 1, backToFirstBox = 1), (viewModel.uiState.value as SessionUiState.Finished).balance)
    }

    @Test
    fun `answering twice and skipping ahead are ignored`() = runTest {
        val viewModel = viewModel(SessionSelection.Topic(Category.POLITICS))
        val first = viewModel.asking.question.id

        viewModel.next()
        assertEquals(first, viewModel.asking.question.id)

        viewModel.answer("A")
        viewModel.answer("B")
        assertEquals("A", viewModel.asking.selectedOptionId)
        assertEquals(1, progress.answers.size)
    }

    @Test
    fun `translation toggle stays on across questions and follows the language setting`() = runTest {
        settings.setTranslationLanguage(Language.UKRAINIAN)
        val viewModel = viewModel(SessionSelection.Topic(Category.POLITICS))
        assertFalse(viewModel.asking.showTranslation)

        viewModel.toggleTranslation()
        viewModel.answer("A")
        viewModel.next()

        assertTrue(viewModel.asking.showTranslation)
        assertEquals(Language.UKRAINIAN, viewModel.asking.translationLanguage)
    }

    @Test
    fun `bookmark toggles the current question`() = runTest {
        val viewModel = viewModel(SessionSelection.Topic(Category.HISTORY))

        viewModel.toggleBookmark()
        assertTrue(viewModel.asking.bookmarked)
        viewModel.toggleBookmark()
        assertFalse(viewModel.asking.bookmarked)
        assertNull("bookmarking does not answer", viewModel.asking.selectedOptionId)
    }

    @Test
    fun `an empty selection finishes right away`() = runTest {
        val viewModel = viewModel(SessionSelection.Mistakes)
        assertEquals(SessionUiState.Finished(correctFirstTime = 0, total = 0), viewModel.uiState.value)
    }
}
