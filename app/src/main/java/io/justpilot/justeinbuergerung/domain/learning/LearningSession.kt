package io.justpilot.justeinbuergerung.domain.learning

import io.justpilot.justeinbuergerung.domain.model.Question

/**
 * Progress through one learning session. Questions answered wrongly are asked again in retry
 * rounds after the main round until every one of them was answered correctly.
 *
 * Only the first answer to a question counts towards the Leitner boxes ([isFirstAttempt]);
 * a retry right after seeing the solution says little about whether it was learnt.
 */
data class LearningSession(
    /** Questions of the current round; the first round holds all questions of the session. */
    val round: List<Question>,
    val position: Int = 0,
    /** 0 for the main round, then 1, 2, … for the retry rounds. */
    val roundNumber: Int = 0,
    /** Option chosen for the current question, or null while it is unanswered. */
    val selectedOptionId: String? = null,
    /** Questions of this round that were answered wrongly; they make up the next round. */
    val wrongInRound: List<Question> = emptyList(),
    /** Ids of questions answered correctly on the first attempt. */
    val correctFirstTime: Set<String> = emptySet(),
    /** Number of questions in the main round. */
    val size: Int = round.size,
) {
    val isFinished: Boolean get() = position >= round.size

    val current: Question? get() = round.getOrNull(position)

    val isAnswered: Boolean get() = selectedOptionId != null

    val isRetryRound: Boolean get() = roundNumber > 0

    val isFirstAttempt: Boolean get() = !isRetryRound

    fun answer(optionId: String): LearningSession {
        val question = checkNotNull(current) { "Session is finished" }
        check(!isAnswered) { "Question ${question.id} was already answered" }
        require(question.options.any { it.id == optionId }) { "Unknown option $optionId for ${question.id}" }
        val correct = question.isCorrect(optionId)
        return copy(
            selectedOptionId = optionId,
            wrongInRound = if (correct) wrongInRound else wrongInRound + question,
            correctFirstTime = if (correct && isFirstAttempt) correctFirstTime + question.id else correctFirstTime,
        )
    }

    /** Moves to the next question, starting a retry round when the current round is done. */
    fun next(): LearningSession {
        check(isAnswered) { "Answer the current question first" }
        return when {
            position + 1 < round.size -> copy(position = position + 1, selectedOptionId = null)
            wrongInRound.isNotEmpty() -> copy(
                round = wrongInRound,
                position = 0,
                roundNumber = roundNumber + 1,
                selectedOptionId = null,
                wrongInRound = emptyList(),
            )
            else -> copy(position = round.size, selectedOptionId = null)
        }
    }
}
