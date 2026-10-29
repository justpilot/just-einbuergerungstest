package io.justpilot.justeinbuergerung.domain.exam

import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.testing.question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExamTest {
    private val pool = (1..300).map { question("$it") } + (1..10).map { question("BY-$it", Category.STATE, FederalState.BAVARIA) }

    @Test
    fun `an exam has 30 general questions followed by 3 state questions`() {
        val exam = Exam.generate(pool, Random(1))

        assertEquals(33, exam.questions.size)
        assertEquals(33, exam.questions.map { it.id }.toSet().size)
        assertTrue(exam.questions.take(30).all { it.state == null })
        assertTrue(exam.questions.drop(30).all { it.state == FederalState.BAVARIA })
    }

    @Test
    fun `exams differ from one another`() {
        assertFalse(Exam.generate(pool, Random(1)).questions == Exam.generate(pool, Random(2)).questions)
    }

    @Test
    fun `answers can be changed and questions flagged in any order`() {
        var exam = Exam.generate(pool, Random(1))
        val first = exam.current.id

        exam = exam.answer("B").answer("A").toggleFlag()
        assertEquals(mapOf(first to "A"), exam.answers)
        assertEquals(setOf(first), exam.flagged)

        exam = exam.goTo(32).answer("C").goTo(0).toggleFlag()
        assertEquals(setOf<String>(), exam.flagged)
        assertEquals(31, exam.unanswered)
    }

    @Test
    fun `navigation stays within the exam`() {
        val exam = Exam.generate(pool, Random(1))

        assertEquals(0, exam.goTo(-1).position)
        assertEquals(32, exam.goTo(33).position)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown options are rejected`() {
        Exam.generate(pool, Random(1)).answer("E")
    }

    @Test
    fun `unanswered questions count as wrong`() {
        // The test questions are answered correctly with "A".
        val exam = Exam.generate(pool, Random(1)).answer("A").goTo(1).answer("B")

        val result = exam.result()

        assertEquals(33, result.total)
        assertEquals(1, result.correct)
        assertEquals(32, result.wrong.size)
        assertEquals("B", result.wrong.first().chosenOptionId)
        assertNull(result.wrong.last().chosenOptionId)
    }

    @Test
    fun `pass marks are 15 for Leben in Deutschland and 17 for citizenship`() {
        fun resultWith(correct: Int): ExamResult {
            var exam = Exam.generate(pool, Random(1))
            repeat(correct) { exam = exam.goTo(it).answer("A") }
            return exam.result()
        }

        with(resultWith(14)) { assertFalse(passedLifeInGermany); assertFalse(passedCitizenship) }
        with(resultWith(15)) { assertTrue(passedLifeInGermany); assertFalse(passedCitizenship) }
        with(resultWith(16)) { assertTrue(passedLifeInGermany); assertFalse(passedCitizenship) }
        with(resultWith(17)) { assertTrue(passedLifeInGermany); assertTrue(passedCitizenship) }
    }
}
