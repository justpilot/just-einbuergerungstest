package io.justpilot.justeinbuergerung.domain.learning

import io.justpilot.justeinbuergerung.testing.question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningSessionTest {
    private val questions = listOf(question("1"), question("2"), question("3"))

    private fun LearningSession.answerAndNext(correct: Boolean) = answer(if (correct) "A" else "B").next()

    @Test
    fun `a session without mistakes ends after the main round`() {
        var session = LearningSession(questions)
        repeat(3) {
            assertTrue(session.isFirstAttempt)
            session = session.answerAndNext(correct = true)
        }

        assertTrue(session.isFinished)
        assertNull(session.current)
        assertEquals(setOf("1", "2", "3"), session.correctFirstTime)
    }

    @Test
    fun `wrong answers are asked again until they are answered correctly`() {
        var session = LearningSession(questions)
            .answerAndNext(correct = false)
            .answerAndNext(correct = true)
            .answerAndNext(correct = false)

        assertEquals(1, session.roundNumber)
        assertTrue(session.isRetryRound)
        assertEquals(listOf("1", "3"), session.round.map { it.id })

        session = session.answerAndNext(correct = true).answerAndNext(correct = false)
        assertEquals(2, session.roundNumber)
        assertEquals(listOf("3"), session.round.map { it.id })

        session = session.answerAndNext(correct = true)
        assertTrue(session.isFinished)
        assertEquals("retries do not count as first-time correct", setOf("2"), session.correctFirstTime)
        assertEquals(3, session.size)
    }

    @Test
    fun `answering keeps the selection until next is called`() {
        val answered = LearningSession(questions).answer("C")

        assertEquals("C", answered.selectedOptionId)
        assertTrue(answered.isAnswered)
        assertEquals("1", answered.current?.id)
        assertFalse(answered.next().isAnswered)
    }

    @Test
    fun `rejects invalid transitions`() {
        val session = LearningSession(questions)
        assertThrows(IllegalStateException::class.java) { session.next() }
        assertThrows(IllegalStateException::class.java) { session.answer("A").answer("B") }
        assertThrows(IllegalArgumentException::class.java) { session.answer("E") }
    }

    @Test
    fun `an empty session is finished right away`() {
        assertTrue(LearningSession(emptyList()).isFinished)
    }
}
