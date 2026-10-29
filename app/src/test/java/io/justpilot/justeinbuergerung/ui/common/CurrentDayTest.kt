package io.justpilot.justeinbuergerung.ui.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentDayTest {
    @Test
    fun `the day moves on at local midnight`() = runTest {
        val berlin = ZoneId.of("Europe/Berlin")
        val start = LocalDate.of(2026, 10, 29).atTime(23, 0).atZone(berlin).toInstant()
        val clock = object : Clock() {
            override fun instant(): Instant = start.plusMillis(testScheduler.currentTime)

            override fun getZone(): ZoneId = berlin

            override fun withZone(zone: ZoneId?): Clock = this
        }
        val days = mutableListOf<LocalDate>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { currentDay(clock).toList(days) }

        advanceTimeBy(Duration.ofMinutes(59).toMillis())
        assertEquals(listOf(LocalDate.of(2026, 10, 29)), days)

        advanceTimeBy(Duration.ofMinutes(2).toMillis())
        assertEquals(listOf(LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 30)), days)
    }
}
