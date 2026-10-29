package io.justpilot.justeinbuergerung.domain.learning

/** How a learning session moved its questions between the Leitner boxes. */
data class SessionBalance(
    /** Questions that moved up a box without reaching mastery, new ones included. */
    val advanced: Int = 0,
    /** Questions that reached the mastered box in this session. */
    val mastered: Int = 0,
    /** Questions that had been answered correctly before and fell back to box 1. */
    val backToFirstBox: Int = 0,
) {
    companion object {
        /**
         * @param startBoxes box of every session question before the session, null for questions never answered
         * @param correctFirstTime ids of questions answered correctly on the first attempt
         */
        fun of(startBoxes: Map<String, Int?>, correctFirstTime: Set<String>): SessionBalance {
            var advanced = 0
            var mastered = 0
            var backToFirstBox = 0
            for ((id, box) in startBoxes) {
                val newBox = LeitnerScheduler.nextBox(box, correct = id in correctFirstTime)
                when {
                    newBox == LeitnerScheduler.MASTERED_BOX && box != LeitnerScheduler.MASTERED_BOX -> mastered++
                    newBox > (box ?: LeitnerScheduler.FIRST_BOX) -> advanced++
                    box != null && box > LeitnerScheduler.FIRST_BOX && newBox == LeitnerScheduler.FIRST_BOX -> backToFirstBox++
                }
            }
            return SessionBalance(advanced, mastered, backToFirstBox)
        }
    }
}
