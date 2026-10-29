package io.justpilot.justeinbuergerung.domain.learning

import java.time.LocalDate

/**
 * Five-box Leitner system: a correct answer moves a question one box up, a wrong answer
 * sends it back to box 1. Each box has a fixed review interval.
 */
object LeitnerScheduler {
    const val FIRST_BOX = 1
    const val MASTERED_BOX = 5

    /** Days until a question in the given box is due again, indexed by box - 1. */
    private val intervalDays = longArrayOf(0, 1, 3, 7, 14)

    data class Schedule(val box: Int, val dueDate: LocalDate)

    /**
     * @param currentBox the question's box, or null if it was never answered
     */
    fun schedule(currentBox: Int?, correct: Boolean, today: LocalDate): Schedule {
        require(currentBox == null || currentBox in FIRST_BOX..MASTERED_BOX) { "Invalid box: $currentBox" }
        val box = when {
            !correct -> FIRST_BOX
            currentBox == null -> FIRST_BOX + 1
            else -> minOf(currentBox + 1, MASTERED_BOX)
        }
        return Schedule(box, today.plusDays(intervalDays[box - 1]))
    }
}
