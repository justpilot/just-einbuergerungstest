package io.justpilot.justeinbuergerung.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class GreetingTest {
    @Test
    fun `the greeting follows the time of day`() {
        assertEquals(Greeting.EVENING, Greeting.of(LocalTime.of(3, 59)))
        assertEquals(Greeting.MORNING, Greeting.of(LocalTime.of(4, 0)))
        assertEquals(Greeting.MORNING, Greeting.of(LocalTime.of(10, 59)))
        assertEquals(Greeting.DAY, Greeting.of(LocalTime.of(11, 0)))
        assertEquals(Greeting.DAY, Greeting.of(LocalTime.of(17, 59)))
        assertEquals(Greeting.EVENING, Greeting.of(LocalTime.of(18, 0)))
    }
}
