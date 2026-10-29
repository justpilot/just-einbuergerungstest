package io.justpilot.justeinbuergerung.domain.learning

import io.justpilot.justeinbuergerung.testing.answered
import io.justpilot.justeinbuergerung.testing.bookmarkOnly
import io.justpilot.justeinbuergerung.testing.question
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LearningOverviewTest {
    private val today = LocalDate.of(2026, 10, 29)

    @Test
    fun `counts new, per-box, due and bookmarked questions`() {
        val questions = (1..6).map { question("$it") }
        val progress = listOf(
            answered("1", box = 1, due = today),
            answered("2", box = 3, due = today.plusDays(7), bookmarked = true),
            answered("3", box = 2, due = today.minusDays(2)),
            bookmarkOnly("4"),
            answered("other", box = 2, due = today),
        ).associateBy { it.questionId }

        val overview = LearningOverview.of(questions, progress, today)

        assertEquals(6, overview.total)
        assertEquals(3, overview.new)
        assertEquals(listOf(1, 1, 1), overview.perBox)
        assertEquals(2, overview.due)
        assertEquals(2, overview.bookmarked)
        assertEquals(1, overview.mastered)
        assertEquals(1, overview.mistakes)
        assertEquals(2, overview.learned)
        assertEquals(1, overview.readyToMaster)
        assertEquals(LearningOverview.NextGoal.ReadyToMaster(1), overview.nextGoal)
    }

    @Test
    fun `without questions ready today the goal points to the next box 2 questions that become due`() {
        val questions = (1..5).map { question("$it") }
        val progress = listOf(
            answered("1", box = 2, due = today.plusDays(2)),
            answered("2", box = 2, due = today.plusDays(1)),
            answered("3", box = 2, due = today.plusDays(1)),
            answered("4", box = 3, due = today.plusDays(1)),
        ).associateBy { it.questionId }

        val overview = LearningOverview.of(questions, progress, today)

        assertEquals(LearningOverview.NextGoal.ReadySoon(count = 2, inDays = 1), overview.nextGoal)
    }

    @Test
    fun `without questions in box 2 the goal is the next milestone`() {
        val questions = (1..120).map { question("$it") }
        val mastered = (1..57).map { answered("$it", box = 3, due = today.plusDays(3)) }
        val mistake = answered("58", box = 1, due = today)

        val overview = LearningOverview.of(questions, (mastered + mistake).associateBy { it.questionId }, today)

        assertEquals(LearningOverview.NextGoal.Milestone(target = 100, missing = 43), overview.nextGoal)
    }

    @Test
    fun `the last milestone is the whole pool, then everything is mastered`() {
        val questions = (1..30).map { question("$it") }
        val almost = (1..29).map { answered("$it", box = 3, due = today.plusDays(3)) }
        assertEquals(
            LearningOverview.NextGoal.Milestone(target = 30, missing = 1),
            LearningOverview.of(questions, almost.associateBy { it.questionId }, today).nextGoal,
        )

        val all = almost + answered("30", box = 3, due = today)
        assertEquals(LearningOverview.NextGoal.AllMastered, LearningOverview.of(questions, all.associateBy { it.questionId }, today).nextGoal)
    }
}
