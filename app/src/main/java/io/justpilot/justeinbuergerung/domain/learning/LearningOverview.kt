package io.justpilot.justeinbuergerung.domain.learning

import io.justpilot.justeinbuergerung.domain.model.Progress
import io.justpilot.justeinbuergerung.domain.model.Question
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How far a set of questions has been learnt. */
data class LearningOverview(
    val total: Int,
    /** Questions that were never answered. */
    val new: Int,
    /** Answered questions per Leitner box, indexed by box - 1. */
    val perBox: List<Int>,
    /** Answered questions whose review is due today or overdue. */
    val due: Int,
    /** Due questions one box below mastery: a correct answer today masters them. */
    val readyToMaster: Int,
    /** Questions one box below mastery that become due next, on the earliest due date after today. */
    val readySoon: Int,
    /** Days until [readySoon] questions are due; meaningless when [readySoon] is 0. */
    val readySoonInDays: Int,
    val bookmarked: Int,
) {
    val mastered: Int get() = perBox[LeitnerScheduler.MASTERED_BOX - 1]

    /** Answered questions in box 1, i.e. the mistakes that still need work. */
    val mistakes: Int get() = perBox[LeitnerScheduler.FIRST_BOX - 1]

    /** Questions whose last answer was correct, i.e. everything above box 1. */
    val learned: Int get() = perBox.drop(LeitnerScheduler.FIRST_BOX).sum()

    /**
     * The closest goal worth pointing out: questions ready for mastery today, else the ones ready next,
     * else the next mastery milestone.
     */
    val nextGoal: NextGoal
        get() = when {
            mastered >= total -> NextGoal.AllMastered
            readyToMaster > 0 -> NextGoal.ReadyToMaster(readyToMaster)
            readySoon > 0 -> NextGoal.ReadySoon(readySoon, readySoonInDays)
            else -> {
                val target = minOf((mastered / MILESTONE_STEP + 1) * MILESTONE_STEP, total)
                NextGoal.Milestone(target = target, missing = target - mastered)
            }
        }

    sealed interface NextGoal {
        data class ReadyToMaster(val count: Int) : NextGoal

        data class ReadySoon(val count: Int, val inDays: Int) : NextGoal

        /** [missing] more mastered questions reach [target]. */
        data class Milestone(val target: Int, val missing: Int) : NextGoal

        data object AllMastered : NextGoal
    }

    companion object {
        const val MILESTONE_STEP = 50

        fun of(questions: List<Question>, progress: Map<String, Progress>, today: LocalDate): LearningOverview {
            val entries = questions.mapNotNull { progress[it.id] }
            val answered = entries.filter { it.lastAnsweredAt != null }
            val belowMastery = answered.filter { it.box == LeitnerScheduler.MASTERED_BOX - 1 }
            val nextReadyDate = belowMastery.mapNotNull { it.dueDate }.filter { it > today }.minOrNull()
            return LearningOverview(
                total = questions.size,
                new = questions.size - answered.size,
                perBox = (LeitnerScheduler.FIRST_BOX..LeitnerScheduler.MASTERED_BOX).map { box -> answered.count { it.box == box } },
                due = answered.count { it.isDue(today) },
                readyToMaster = belowMastery.count { it.isDue(today) },
                readySoon = belowMastery.count { it.dueDate == nextReadyDate },
                readySoonInDays = nextReadyDate?.let { ChronoUnit.DAYS.between(today, it).toInt() } ?: 0,
                bookmarked = entries.count { it.bookmarked },
            )
        }

        private fun Progress.isDue(today: LocalDate) = dueDate != null && dueDate <= today
    }
}
