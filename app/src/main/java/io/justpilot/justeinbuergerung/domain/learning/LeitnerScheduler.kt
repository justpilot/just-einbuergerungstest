package io.justpilot.justeinbuergerung.domain.learning

import java.time.LocalDate

/**
 * Three-box Leitner system: a correct answer moves a question one box up, a wrong answer
 * sends it back to box 1. Each box has a fixed review interval.
 */
object LeitnerScheduler {
    const val FIRST_BOX = 1
    const val MASTERED_BOX = 3

    /** Days until a question in the given box is due again, indexed by box - 1. */
    private val intervalDays = longArrayOf(0, 2, 7)

    data class Schedule(val box: Int, val dueDate: LocalDate)

    /**
     * @param currentBox the question's box, or null if it was never answered
     */
    fun schedule(currentBox: Int?, correct: Boolean, today: LocalDate): Schedule {
        val box = nextBox(currentBox, correct)
        return Schedule(box, today.plusDays(intervalDays[box - 1]))
    }

    /**
     * @param currentBox the question's box, or null if it was never answered
     */
    fun nextBox(currentBox: Int?, correct: Boolean): Int {
        require(currentBox == null || currentBox in FIRST_BOX..MASTERED_BOX) { "Invalid box: $currentBox" }
        return when {
            !correct -> FIRST_BOX
            currentBox == null -> FIRST_BOX + 1
            else -> minOf(currentBox + 1, MASTERED_BOX)
        }
    }
}
