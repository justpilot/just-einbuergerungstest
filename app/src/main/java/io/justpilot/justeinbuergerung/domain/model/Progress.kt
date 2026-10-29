package io.justpilot.justeinbuergerung.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Learning state of one question. Questions that were never answered have no [Progress];
 * bookmarking an unanswered question creates one in box 1 without a due date.
 */
data class Progress(
    val questionId: String,
    /** Leitner box 1 (new or wrong) to 5 (mastered). */
    val box: Int,
    /** First day on which the question is due again; null if it was never answered. */
    val dueDate: LocalDate?,
    val lastAnsweredAt: Instant?,
    val bookmarked: Boolean,
)

/** Where an answer was given; exam answers are logged but only count for statistics. */
enum class AnswerSource {
    DAILY,
    EXAM,
    CATALOG,
}

data class Settings(
    /** Null until the user picked a federal state during onboarding. */
    val federalState: FederalState?,
    val translationLanguage: Language,
    val hapticsEnabled: Boolean,
)
