package io.justpilot.justeinbuergerung.domain.learning

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionBalanceTest {
    @Test
    fun `new questions answered correctly advance, wrong ones do not count as a setback`() {
        assertEquals(SessionBalance(advanced = 1), SessionBalance.of(mapOf("1" to null, "2" to null), correctFirstTime = setOf("1")))
    }

    @Test
    fun `reaching the last box counts as mastered, staying there does not`() {
        val balance = SessionBalance.of(mapOf("1" to 2, "2" to 3, "3" to 1), correctFirstTime = setOf("1", "2", "3"))
        assertEquals(SessionBalance(advanced = 1, mastered = 1), balance)
    }

    @Test
    fun `wrong answers above box 1 fall back, box 1 stays without counting`() {
        val balance = SessionBalance.of(mapOf("1" to 2, "2" to 3, "3" to 1), correctFirstTime = emptySet())
        assertEquals(SessionBalance(backToFirstBox = 2), balance)
    }
}
