package io.justpilot.justeinbuergerung.domain.learning

import io.justpilot.justeinbuergerung.domain.model.AnswerSource
import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.Progress
import io.justpilot.justeinbuergerung.domain.model.Question
import java.time.LocalDate
import kotlin.random.Random

/** Which questions a learning session asks. */
sealed interface SessionSelection {
    /** Answers count towards the Leitner boxes either way; the source only labels them in the log. */
    val source: AnswerSource

    /** Daily 10: a mix of due reviews and new questions. */
    data object Daily : SessionSelection {
        override val source = AnswerSource.DAILY
    }

    /** The [QuestionSelector.TOPIC_SIZE] weakest questions of one topic area. */
    data class Topic(val category: Category) : SessionSelection {
        override val source = AnswerSource.CATALOG
    }

    /** Questions that were answered and are currently in box 1. */
    data object Mistakes : SessionSelection {
        override val source = AnswerSource.CATALOG
    }

    data object Bookmarks : SessionSelection {
        override val source = AnswerSource.CATALOG
    }

    /** Answered questions currently in one Leitner box, the soonest due first. */
    data class Box(val box: Int) : SessionSelection {
        init {
            require(box in LeitnerScheduler.FIRST_BOX..LeitnerScheduler.MASTERED_BOX) { "Invalid box $box" }
        }

        override val source = AnswerSource.CATALOG
    }

    /** Stable string form for navigation arguments. */
    val key: String
        get() = when (this) {
            Daily -> "daily"
            is Topic -> "topic:${category.code}"
            Mistakes -> "mistakes"
            Bookmarks -> "bookmarks"
            is Box -> "box:$box"
        }

    companion object {
        fun fromKey(key: String): SessionSelection = when {
            key == Daily.key -> Daily
            key == Mistakes.key -> Mistakes
            key == Bookmarks.key -> Bookmarks
            key.startsWith("topic:") -> Topic(Category.fromCode(key.removePrefix("topic:")))
            key.startsWith("box:") -> Box(key.removePrefix("box:").toInt())
            else -> throw IllegalArgumentException("Unknown session selection: $key")
        }
    }
}

object QuestionSelector {
    const val DAILY_SIZE = 10
    const val DAILY_REVIEWS = 6
    const val DAILY_NEW = DAILY_SIZE - DAILY_REVIEWS

    /** Topics hold up to 151 questions; a block keeps the retry round within reach. */
    const val TOPIC_SIZE = 20

    fun select(
        selection: SessionSelection,
        pool: List<Question>,
        progress: Map<String, Progress>,
        today: LocalDate,
        random: Random = Random.Default,
    ): List<Question> = when (selection) {
        SessionSelection.Daily -> daily(pool, progress, today, random)
        is SessionSelection.Topic -> pool.filter { it.category == selection.category }.weakestFirst(progress).take(TOPIC_SIZE)
        SessionSelection.Mistakes -> pool.filter { progress[it.id].isMistake() }
        SessionSelection.Bookmarks -> pool.filter { progress[it.id]?.bookmarked == true }
        is SessionSelection.Box -> pool
            .filter { progress[it.id].isAnswered() && progress.getValue(it.id).box == selection.box }
            .sortedBy { progress.getValue(it.id).dueDate }
    }

    /**
     * Up to [DAILY_REVIEWS] due questions (lowest box first) and [DAILY_NEW] new ones. If one
     * kind runs short the other fills the gap; if both do, questions that are not yet due are
     * reviewed early, soonest first. The result is shuffled so reviews and new questions mix.
     */
    private fun daily(pool: List<Question>, progress: Map<String, Progress>, today: LocalDate, random: Random): List<Question> {
        val (answered, new) = pool.partition { progress[it.id].isAnswered() }
        val (due, notDue) = answered.partition { progress.getValue(it.id).dueDate!! <= today }
        val dueQueue = due.sortedWith(compareBy({ progress.getValue(it.id).box }, { progress.getValue(it.id).dueDate }))
        val newQueue = new.shuffled(random)

        val reviews = dueQueue.take(DAILY_REVIEWS)
        // With six reviews this leaves room for four new questions, with fewer reviews for more.
        val fresh = newQueue.take(DAILY_SIZE - reviews.size)
        val moreReviews = dueQueue.drop(reviews.size).take(DAILY_SIZE - reviews.size - fresh.size)
        val early = notDue.sortedBy { progress.getValue(it.id).dueDate }
            .take(DAILY_SIZE - reviews.size - fresh.size - moreReviews.size)
        return (reviews + fresh + moreReviews + early).shuffled(random)
    }

    /** Unanswered and box-1 questions first, mastered ones last; catalog order within a box. */
    private fun List<Question>.weakestFirst(progress: Map<String, Progress>): List<Question> =
        sortedBy { progress[it.id]?.box ?: LeitnerScheduler.FIRST_BOX }

    private fun Progress?.isAnswered() = this?.lastAnsweredAt != null

    private fun Progress?.isMistake() = this != null && lastAnsweredAt != null && box == LeitnerScheduler.FIRST_BOX
}
