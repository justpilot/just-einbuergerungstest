package io.justpilot.justeinbuergerung.testing

import io.justpilot.justeinbuergerung.domain.model.AnswerOption
import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.LocalizedText
import io.justpilot.justeinbuergerung.domain.model.Progress
import io.justpilot.justeinbuergerung.domain.model.Question
import java.time.Instant
import java.time.LocalDate

/** A question whose correct answer is always option "A". */
fun question(
    id: String,
    category: Category = Category.POLITICS,
    state: FederalState? = null,
    number: Int = id.substringAfter('-').toIntOrNull() ?: 1,
): Question = Question(
    id = id,
    number = number,
    state = state,
    category = category,
    imagePath = null,
    text = LocalizedText("Frage $id?", "Question $id?", "Питання $id?"),
    options = listOf("A", "B", "C", "D").map { AnswerOption(it, LocalizedText("Antwort $it", "Answer $it", "Відповідь $it")) },
    correctOptionId = "A",
)

/** Progress of a question that was answered at least once. */
fun answered(id: String, box: Int, due: LocalDate, bookmarked: Boolean = false): Progress =
    Progress(id, box, due, Instant.EPOCH, bookmarked)

/** A bookmark on a question that was never answered. */
fun bookmarkOnly(id: String): Progress = Progress(id, 1, null, null, bookmarked = true)
