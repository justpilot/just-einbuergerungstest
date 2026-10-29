package io.justpilot.justeinbuergerung.domain.learning

import io.justpilot.justeinbuergerung.domain.model.Progress
import io.justpilot.justeinbuergerung.domain.model.Question
import java.time.LocalDate

/** How far a set of questions has been learnt. */
data class LearningOverview(
    val total: Int,
    /** Questions that were never answered. */
    val new: Int,
    /** Answered questions per Leitner box, indexed by box - 1. */
    val perBox: List<Int>,
    /** Answered questions whose review is due today or overdue. */
    val due: Int,
    val bookmarked: Int,
) {
    val mastered: Int get() = perBox[LeitnerScheduler.MASTERED_BOX - 1]

    /** Answered questions in box 1, i.e. the mistakes that still need work. */
    val mistakes: Int get() = perBox[LeitnerScheduler.FIRST_BOX - 1]

    companion object {
        fun of(questions: List<Question>, progress: Map<String, Progress>, today: LocalDate): LearningOverview {
            val entries = questions.mapNotNull { progress[it.id] }
            val answered = entries.filter { it.lastAnsweredAt != null }
            return LearningOverview(
                total = questions.size,
                new = questions.size - answered.size,
                perBox = (LeitnerScheduler.FIRST_BOX..LeitnerScheduler.MASTERED_BOX).map { box -> answered.count { it.box == box } },
                due = answered.count { it.dueDate != null && it.dueDate <= today },
                bookmarked = entries.count { it.bookmarked },
            )
        }
    }
}
