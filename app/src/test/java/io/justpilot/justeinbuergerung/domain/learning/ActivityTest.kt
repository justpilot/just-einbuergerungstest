package io.justpilot.justeinbuergerung.domain.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ActivityTest {
    private val today = LocalDate.of(2026, 10, 29)

    private fun daysAgo(vararg days: Long) = days.map { today.minusDays(it) }.toSet()

    @Test
    fun `the last seven days end today and days without answers count 0`() {
        val activity = Activity.of(mapOf(today to 12, today.minusDays(2) to 5, today.minusDays(7) to 99), emptySet(), today)

        assertEquals((6 downTo 0).map { today.minusDays(it.toLong()) }, activity.days.map { it.date })
        assertEquals(listOf(0, 0, 0, 0, 5, 0, 12), activity.days.map { it.answers })
    }

    @Test
    fun `the streak counts consecutive days with a finished Daily 10 up to today`() {
        val activity = Activity.of(emptyMap(), daysAgo(0, 1, 2, 4), today)

        assertEquals(3, activity.streak)
        assertTrue(activity.dailyDoneToday)
    }

    @Test
    fun `an open today keeps yesterday's streak`() {
        val activity = Activity.of(emptyMap(), daysAgo(1, 2), today)

        assertEquals(2, activity.streak)
        assertFalse(activity.dailyDoneToday)
    }

    @Test
    fun `a missed day ends the streak`() {
        assertEquals(0, Activity.of(emptyMap(), daysAgo(2, 3), today).streak)
        assertEquals(0, Activity.of(emptyMap(), emptySet(), today).streak)
    }
}
