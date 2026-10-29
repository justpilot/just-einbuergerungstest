package io.justpilot.justeinbuergerung.domain.learning

import io.justpilot.justeinbuergerung.testing.answered
import io.justpilot.justeinbuergerung.testing.bookmarkOnly
import io.justpilot.justeinbuergerung.testing.question
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LearningOverviewTest {
    private val today = LocalDate.of(2026, 10, 29)

    @Test
    fun `counts new, per-box, due and bookmarked questions`() {
        val questions = (1..6).map { question("$it") }
        val progress = listOf(
            answered("1", box = 1, due = today),
            answered("2", box = 5, due = today.plusDays(14), bookmarked = true),
            answered("3", box = 3, due = today.minusDays(2)),
            bookmarkOnly("4"),
            answered("other", box = 2, due = today),
        ).associateBy { it.questionId }

        val overview = LearningOverview.of(questions, progress, today)

        assertEquals(6, overview.total)
        assertEquals(3, overview.new)
        assertEquals(listOf(1, 0, 1, 0, 1), overview.perBox)
        assertEquals(2, overview.due)
        assertEquals(2, overview.bookmarked)
        assertEquals(1, overview.mastered)
        assertEquals(1, overview.mistakes)
    }
}
