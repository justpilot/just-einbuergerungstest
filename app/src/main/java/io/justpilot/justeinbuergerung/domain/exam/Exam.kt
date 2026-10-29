package io.justpilot.justeinbuergerung.domain.exam

import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.Question
import java.time.Duration
import kotlin.random.Random

/**
 * One exam simulation: questions can be answered in any order, changed until submission and
 * flagged for a second look. Nothing is revealed before [result].
 */
data class Exam(
    val questions: List<Question>,
    val position: Int = 0,
    /** Chosen option per question id; unanswered questions are absent. */
    val answers: Map<String, String> = emptyMap(),
    /** Ids of questions marked for a second look. */
    val flagged: Set<String> = emptySet(),
) {
    init {
        require(position in questions.indices) { "Position $position outside 0..${questions.lastIndex}" }
    }

    val current: Question get() = questions[position]

    val unanswered: Int get() = questions.count { it.id !in answers }

    fun answer(optionId: String): Exam {
        require(current.options.any { it.id == optionId }) { "Unknown option $optionId for ${current.id}" }
        return copy(answers = answers + (current.id to optionId))
    }

    fun toggleFlag(): Exam = copy(flagged = if (current.id in flagged) flagged - current.id else flagged + current.id)

    fun goTo(index: Int): Exam = copy(position = index.coerceIn(questions.indices))

    fun result(): ExamResult = ExamResult(
        total = questions.size,
        answers = questions.map { ExamResult.Answer(it, answers[it.id]) },
    )

    companion object {
        const val GENERAL_QUESTIONS = 30
        const val STATE_QUESTIONS = 3
        val DURATION: Duration = Duration.ofMinutes(60)

        /**
         * 30 random general questions followed by 3 random questions of the federal state, as in
         * the real test. [pool] is the active pool: all general questions plus those of one state.
         */
        fun generate(pool: List<Question>, random: Random = Random.Default): Exam {
            val (state, general) = pool.partition { it.category == Category.STATE }
            return Exam(general.shuffled(random).take(GENERAL_QUESTIONS) + state.shuffled(random).take(STATE_QUESTIONS))
        }
    }
}

data class ExamResult(val total: Int, val answers: List<Answer>) {
    /** A question with the option chosen for it, null if it was left unanswered. */
    data class Answer(val question: Question, val chosenOptionId: String?) {
        val correct: Boolean get() = chosenOptionId != null && question.isCorrect(chosenOptionId)
    }

    val correct: Int get() = answers.count { it.correct }

    val wrong: List<Answer> get() = answers.filterNot { it.correct }

    /** Pass mark of the "Leben in Deutschland" test. */
    val passedLifeInGermany: Boolean get() = correct >= LIFE_IN_GERMANY_PASS_MARK

    /** Pass mark of the citizenship test. */
    val passedCitizenship: Boolean get() = correct >= CITIZENSHIP_PASS_MARK

    companion object {
        const val LIFE_IN_GERMANY_PASS_MARK = 15
        const val CITIZENSHIP_PASS_MARK = 17
    }
}
