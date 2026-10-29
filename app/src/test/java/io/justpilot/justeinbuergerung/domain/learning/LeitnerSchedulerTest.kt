package io.justpilot.justeinbuergerung.domain.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class LeitnerSchedulerTest {
    private val today = LocalDate.of(2026, 10, 29)

    private fun schedule(box: Int?, correct: Boolean) = LeitnerScheduler.schedule(box, correct, today)

    @Test
    fun `new question answered correctly moves to box 2 and is due in two days`() {
        assertEquals(LeitnerScheduler.Schedule(2, today.plusDays(2)), schedule(null, correct = true))
    }

    @Test
    fun `new question answered wrongly stays in box 1 and is due today`() {
        assertEquals(LeitnerScheduler.Schedule(1, today), schedule(null, correct = false))
    }

    @Test
    fun `correct answers move one box up with growing intervals`() {
        assertEquals(LeitnerScheduler.Schedule(2, today.plusDays(2)), schedule(1, correct = true))
        assertEquals(LeitnerScheduler.Schedule(3, today.plusDays(7)), schedule(2, correct = true))
    }

    @Test
    fun `mastered questions stay in the last box`() {
        assertEquals(LeitnerScheduler.Schedule(3, today.plusDays(7)), schedule(3, correct = true))
    }

    @Test
    fun `a wrong answer sends any box back to box 1`() {
        for (box in 1..3) {
            assertEquals(LeitnerScheduler.Schedule(1, today), schedule(box, correct = false))
        }
    }

    @Test
    fun `rejects boxes outside 1 to 3`() {
        assertThrows(IllegalArgumentException::class.java) { schedule(0, correct = true) }
        assertThrows(IllegalArgumentException::class.java) { schedule(4, correct = true) }
    }
}
