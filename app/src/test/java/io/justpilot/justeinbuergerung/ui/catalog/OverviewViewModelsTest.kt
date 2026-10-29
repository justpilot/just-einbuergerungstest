package io.justpilot.justeinbuergerung.ui.catalog

import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.testing.FakeProgressRepository
import io.justpilot.justeinbuergerung.testing.FakeQuestionRepository
import io.justpilot.justeinbuergerung.testing.FakeSettingsRepository
import io.justpilot.justeinbuergerung.testing.MainDispatcherRule
import io.justpilot.justeinbuergerung.testing.answered
import io.justpilot.justeinbuergerung.testing.bookmarkOnly
import io.justpilot.justeinbuergerung.testing.question
import io.justpilot.justeinbuergerung.ui.home.HomeUiState
import io.justpilot.justeinbuergerung.ui.home.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Home and catalog both summarise the active pool. */
@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelsTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 29)
    private val clock = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC)
    private val questions = FakeQuestionRepository()
    private val progress = FakeProgressRepository(
        listOf(answered("1", box = 1, due = today), answered("2", box = 3, due = today.plusDays(7)), bookmarkOnly("BY-1"))
            .associateBy { it.questionId },
    )
    private val settings = FakeSettingsRepository()

    @Test
    fun `home is loading until the catalog is imported, then summarises the pool`() = runTest {
        val viewModel = HomeViewModel(questions, progress, settings, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)

        questions.questions.value = listOf(question("1"), question("2"), question("BY-1", Category.STATE, FederalState.BAVARIA))

        val overview = (viewModel.uiState.value as HomeUiState.Ready).overview
        assertEquals(3, overview.total)
        assertEquals(1, overview.mastered)
        assertEquals(1, overview.due)
    }

    @Test
    fun `catalog counts per topic and follows the selected state`() = runTest {
        questions.questions.value = listOf(
            question("1"),
            question("2", Category.HISTORY),
            question("BY-1", Category.STATE, FederalState.BAVARIA),
            question("BE-1", Category.STATE, FederalState.BERLIN),
            question("BE-2", Category.STATE, FederalState.BERLIN),
        )
        val viewModel = CatalogViewModel(questions, progress, settings, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        var state = viewModel.uiState.value as CatalogUiState.Ready
        assertEquals(Category.entries, state.topics.keys.toList())
        assertEquals(1, state.topics.getValue(Category.STATE).total)
        assertEquals(1, state.mistakes)
        assertEquals(1, state.bookmarks)

        settings.setFederalState(FederalState.BERLIN)
        state = viewModel.uiState.value as CatalogUiState.Ready
        assertEquals(FederalState.BERLIN, state.state)
        assertEquals(2, state.topics.getValue(Category.STATE).total)
        assertEquals("the bookmark belongs to a Bavarian question", 0, state.bookmarks)
    }
}
