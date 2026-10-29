package io.justpilot.justeinbuergerung.domain.learning

import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.Progress
import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.testing.answered
import io.justpilot.justeinbuergerung.testing.bookmarkOnly
import io.justpilot.justeinbuergerung.testing.question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class QuestionSelectorTest {
    private val today = LocalDate.of(2026, 10, 29)
    private val pool = (1..30).map { question("$it") }

    private fun daily(progress: Map<String, Progress>, pool: List<Question> = this.pool) =
        QuestionSelector.select(SessionSelection.Daily, pool, progress, today, Random(42)).map { it.id }

    private fun progressOf(vararg entries: Progress) = entries.associateBy { it.questionId }

    @Test
    fun `daily session mixes six due reviews with four new questions`() {
        val due = (1..8).map { answered("$it", box = 2, due = today.minusDays(it.toLong())) }
        val ids = daily(progressOf(*due.toTypedArray()))

        assertEquals(10, ids.size)
        assertEquals(10, ids.toSet().size)
        assertEquals(6, ids.count { it.toInt() <= 8 })
        assertEquals(4, ids.count { it.toInt() > 8 })
    }

    @Test
    fun `due reviews in lower boxes come first`() {
        val progress = progressOf(
            *(1..6).map { answered("$it", box = 3, due = today.minusDays(5)) }.toTypedArray(),
            answered("7", box = 1, due = today),
            answered("8", box = 2, due = today),
        )
        val ids = daily(progress)

        assertTrue("7" in ids && "8" in ids)
        assertEquals(4, ids.count { it.toInt() in 1..6 })
    }

    @Test
    fun `new questions fill the gap when few reviews are due`() {
        val ids = daily(progressOf(answered("1", box = 2, due = today), answered("2", box = 2, due = today.plusDays(1))))

        assertEquals(10, ids.size)
        assertTrue("1" in ids)
        assertTrue("not yet due", "2" !in ids)
    }

    @Test
    fun `due reviews fill the gap when no new questions are left`() {
        val progress = progressOf(*pool.map { answered(it.id, box = 2, due = today) }.toTypedArray())
        assertEquals(10, daily(progress).size)
    }

    @Test
    fun `questions are reviewed early when nothing is due and nothing is new`() {
        val small = pool.take(12)
        val progress = progressOf(*small.mapIndexed { i, q -> answered(q.id, box = 3, due = today.plusDays(i + 1L)) }.toTypedArray())

        assertEquals((1..10).map { "$it" }.toSet(), daily(progress, small).toSet())
    }

    @Test
    fun `bookmarked but unanswered questions count as new`() {
        val small = pool.take(3)
        assertEquals(setOf("1", "2", "3"), daily(progressOf(bookmarkOnly("1")), small).toSet())
    }

    @Test
    fun `topic sessions contain the topic's questions, weakest first`() {
        val mixed = listOf(question("1"), question("2", Category.HISTORY), question("3"), question("4"))
        val progress = progressOf(answered("1", box = 3, due = today), answered("3", box = 2, due = today))

        val ids = QuestionSelector.select(SessionSelection.Topic(Category.POLITICS), mixed, progress, today).map { it.id }
        assertEquals(listOf("4", "3", "1"), ids)
    }

    @Test
    fun `mistakes are answered questions in box 1, bookmarks include unanswered ones`() {
        val progress = progressOf(
            answered("1", box = 1, due = today, bookmarked = true),
            answered("2", box = 2, due = today),
            bookmarkOnly("3"),
        )

        assertEquals(listOf("1"), QuestionSelector.select(SessionSelection.Mistakes, pool, progress, today).map { it.id })
        assertEquals(listOf("1", "3"), QuestionSelector.select(SessionSelection.Bookmarks, pool, progress, today).map { it.id })
    }

    @Test
    fun `box sessions contain the answered questions of that box, soonest due first`() {
        val progress = progressOf(
            answered("1", box = 2, due = today.plusDays(2)),
            answered("2", box = 2, due = today),
            answered("3", box = 3, due = today),
            bookmarkOnly("4"),
        )

        assertEquals(listOf("2", "1"), QuestionSelector.select(SessionSelection.Box(2), pool, progress, today).map { it.id })
        assertEquals(listOf("3"), QuestionSelector.select(SessionSelection.Box(3), pool, progress, today).map { it.id })
        assertEquals(emptyList<String>(), QuestionSelector.select(SessionSelection.Box(1), pool, progress, today).map { it.id })
        assertThrows(IllegalArgumentException::class.java) { SessionSelection.Box(4) }
    }

    @Test
    fun `selection keys round-trip`() {
        val selections = listOf(SessionSelection.Daily, SessionSelection.Mistakes, SessionSelection.Bookmarks) +
            Category.entries.map { SessionSelection.Topic(it) } + (1..3).map { SessionSelection.Box(it) }
        for (selection in selections) {
            assertEquals(selection, SessionSelection.fromKey(selection.key))
        }
        assertThrows(IllegalArgumentException::class.java) { SessionSelection.fromKey("exam") }
    }
}
