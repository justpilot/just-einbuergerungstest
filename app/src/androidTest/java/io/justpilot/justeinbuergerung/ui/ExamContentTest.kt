package io.justpilot.justeinbuergerung.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.justpilot.justeinbuergerung.domain.exam.ExamResult
import io.justpilot.justeinbuergerung.domain.model.AnswerOption
import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.LocalizedText
import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.ui.exam.ExamContent
import io.justpilot.justeinbuergerung.ui.exam.ExamUiState
import io.justpilot.justeinbuergerung.ui.theme.JustEinbürgerungstestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration

@RunWith(AndroidJUnit4::class)
class ExamContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun question(id: String, state: FederalState? = null) = Question(
        id = id,
        number = id.substringAfter('-').toInt(),
        state = state,
        category = if (state == null) Category.POLITICS else Category.STATE,
        imagePath = null,
        text = LocalizedText("Was gilt bei $id?", "What applies to $id?", null, null),
        options = listOf("A", "B", "C", "D").map { AnswerOption(it, LocalizedText("Antwort $it", "Answer $it", null, null)) },
        correctOptionId = "A",
    )

    private fun running(position: Int = 1, selected: String? = null) = ExamUiState.Running(
        question = question("7"),
        position = position,
        selectedOptionId = selected,
        flagged = false,
        overview = List(33) { ExamUiState.QuestionStatus(answered = it < 3, flagged = false) },
        remaining = Duration.ofMinutes(42).plusSeconds(5),
    )

    private fun show(initial: ExamUiState, onSubmit: () -> Unit = {}, onClose: () -> Unit = {}) {
        compose.setContent {
            var state by remember { mutableStateOf(initial) }
            JustEinbürgerungstestTheme(dynamicColor = false) {
                ExamContent(
                    state = state,
                    onAnswer = { option -> state = (state as ExamUiState.Running).copy(selectedOptionId = option) },
                    onToggleFlag = {},
                    onPrevious = {},
                    onNext = {},
                    onGoTo = {},
                    onSubmit = onSubmit,
                    onClose = onClose,
                )
            }
        }
    }

    @Test
    fun answersAreSelectedWithoutRevealingTheSolution() {
        show(running())
        compose.onNodeWithText("Frage 1 von 33").assertExists()
        compose.onNodeWithContentDescription("Verbleibende Zeit 42:05").assertExists()

        compose.onNodeWithText("Antwort B").performClick()

        compose.onNodeWithText("Leider falsch", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Übersetzung anzeigen").assertDoesNotExist()
        // The answer can still be changed.
        compose.onNodeWithText("Antwort A").assertIsEnabled().performClick()
    }

    @Test
    fun submittingAsksFirstAndNamesTheUnansweredQuestions() {
        var submitted = 0
        show(running(position = 33), onSubmit = { submitted++ })

        compose.onNodeWithText("Abgeben").performClick()

        compose.onNodeWithText("30 Fragen sind noch unbeantwortet und zählen als falsch.").assertExists()
        assertEquals(0, submitted)
        compose.onNodeWithText("Weitermachen").performClick()
        compose.onNodeWithText("Prüfung abgeben?").assertDoesNotExist()
    }

    @Test
    fun closingARunningExamAsksFirst() {
        var closed = 0
        show(running(), onClose = { closed++ })

        compose.onNodeWithContentDescription("Sitzung beenden").performClick()
        assertEquals(0, closed)

        compose.onNodeWithText("Prüfung abbrechen").performClick()
        assertEquals(1, closed)
    }

    @Test
    fun resultShowsPassMarksAndTheWrongAnswers() {
        val answers = List(17) { ExamResult.Answer(question("${it + 1}"), "A") } +
            ExamResult.Answer(question("BY-4", FederalState.BAVARIA), "C") +
            List(15) { ExamResult.Answer(question("${it + 100}"), null) }
        show(ExamUiState.Finished(ExamResult(total = 33, answers = answers)))

        compose.onNodeWithText("17 von 33 richtig").assertExists()
        compose.onNodeWithText("Einbürgerungstest (ab 17)", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Bayern, Frage 4").performScrollTo().assertExists()
        compose.onNodeWithText("Deine Antwort: C) Antwort C").performScrollTo().assertExists()
        compose.onNodeWithText("Frage 100").performScrollTo().assertExists()
    }
}
