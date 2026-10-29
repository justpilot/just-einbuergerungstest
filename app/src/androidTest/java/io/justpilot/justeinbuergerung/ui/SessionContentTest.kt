package io.justpilot.justeinbuergerung.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.justpilot.justeinbuergerung.domain.learning.SessionBalance
import io.justpilot.justeinbuergerung.domain.model.AnswerOption
import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.LocalizedText
import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.ui.onboarding.OnboardingScreen
import io.justpilot.justeinbuergerung.ui.session.SessionContent
import io.justpilot.justeinbuergerung.ui.session.SessionUiState
import io.justpilot.justeinbuergerung.ui.theme.JustEinbuergerungstestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val question = Question(
        id = "1",
        number = 1,
        state = null,
        category = Category.POLITICS,
        imagePath = null,
        text = LocalizedText("Was gilt hier?", "What applies here?", "Що тут діє?", "Burada ne geçerlidir?"),
        options = listOf("A", "B", "C", "D").map { AnswerOption(it, LocalizedText("Antwort $it", "Answer $it", "Відповідь $it", "Cevap $it")) },
        correctOptionId = "A",
    )

    private fun asking(selected: String? = null, translation: Boolean = false, daily: Boolean = false) = SessionUiState.Asking(
        question = question,
        position = 3,
        roundSize = 10,
        isRetryRound = false,
        selectedOptionId = selected,
        isLastQuestion = false,
        showTranslation = translation,
        translationLanguage = Language.ENGLISH,
        bookmarked = false,
        hapticsEnabled = false,
        isDaily = daily,
    )

    private fun show(initial: SessionUiState.Asking, onNext: () -> Unit = {}, onClose: () -> Unit = {}) {
        compose.setContent {
            var state by remember { mutableStateOf(initial) }
            JustEinbuergerungstestTheme(dynamicColor = false) {
                SessionContent(
                    state = state,
                    onAnswer = { state = state.copy(selectedOptionId = it) },
                    onNext = onNext,
                    onToggleTranslation = { state = state.copy(showTranslation = !state.showTranslation) },
                    onToggleBookmark = {},
                    onClose = onClose,
                    onRestart = {},
                )
            }
        }
    }

    private fun showFinished(finished: SessionUiState.Finished, onClose: () -> Unit = {}, onRestart: () -> Unit = {}) {
        compose.setContent {
            JustEinbuergerungstestTheme(dynamicColor = false) {
                SessionContent(
                    state = finished,
                    onAnswer = {},
                    onNext = {},
                    onToggleTranslation = {},
                    onToggleBookmark = {},
                    onClose = onClose,
                    onRestart = onRestart,
                )
            }
        }
    }

    @Test
    fun closingAStartedDailySessionAsksFirst() {
        var closed = 0
        show(asking(daily = true), onClose = { closed++ })

        compose.onNodeWithContentDescription("Sitzung beenden").performClick()
        compose.onNodeWithText("Tages-Session beenden?").assertExists()
        compose.onNodeWithText("Weitermachen").performClick()
        assertEquals(0, closed)

        compose.onNodeWithContentDescription("Sitzung beenden").performClick()
        compose.onNodeWithText("Beenden").performClick()
        assertEquals(1, closed)
    }

    @Test
    fun otherSessionsCloseWithoutAsking() {
        var closed = 0
        show(asking(daily = false), onClose = { closed++ })

        compose.onNodeWithContentDescription("Sitzung beenden").performClick()
        assertEquals(1, closed)
    }

    @Test
    fun picturesOpenFullScreen() {
        show(asking().copy(question = question.copy(imagePath = "images/130.png")))

        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Abbildung zur Frage").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Abbildung zur Frage").performClick()
        compose.onNodeWithContentDescription("Schließen").performClick()
        compose.onNodeWithContentDescription("Schließen").assertDoesNotExist()
    }

    @Test
    fun theCatalogNumberIsShownAboveTheQuestion() {
        show(asking())
        compose.onNodeWithText("Frage ${question.number}").assertExists()
    }

    @Test
    fun aWeakRoundIsNotCelebratedAndCanBeRepeated() {
        var restarts = 0
        showFinished(SessionUiState.Finished(correctFirstTime = 3, total = 10), onRestart = { restarts++ })

        compose.onNodeWithText("Runde beendet").assertExists()
        compose.onNodeWithText("Geschafft!").assertDoesNotExist()
        compose.onNodeWithText("Noch eine Runde").performClick()
        assertEquals(1, restarts)
    }

    @Test
    fun answeringShowsTheSolutionAndLocksTheCards() {
        var nextClicks = 0
        show(asking(), onNext = { nextClicks++ })
        compose.onNodeWithText("Frage 3 von 10").assertExists()
        compose.onNodeWithText("Weiter").assertDoesNotExist()

        compose.onNodeWithText("Antwort B").performClick()

        compose.onNodeWithText("Leider falsch", substring = true).assertExists()
        compose.onNodeWithText("Antwort A").assertIsNotEnabled()
        compose.onNodeWithText("Weiter").performClick()
        assertEquals(1, nextClicks)
    }

    @Test
    fun translationFoldsOutForQuestionAndAnswers() {
        show(asking())
        compose.onNodeWithText("What applies here?").assertDoesNotExist()

        compose.onNodeWithContentDescription("Übersetzung anzeigen").performClick()

        compose.onNodeWithText("What applies here?").assertExists()
        compose.onNodeWithText("Answer D").assertExists()
    }

    @Test
    fun summaryShowsOnlyTheBoxMovesThatHappened() {
        compose.setContent {
            JustEinbuergerungstestTheme(dynamicColor = false) {
                SessionContent(
                    state = SessionUiState.Finished(correctFirstTime = 8, total = 10, SessionBalance(advanced = 5, mastered = 3)),
                    onAnswer = {},
                    onNext = {},
                    onToggleTranslation = {},
                    onToggleBookmark = {},
                    onClose = {},
                    onRestart = {},
                )
            }
        }

        compose.onNodeWithText("3 Fragen neu gemeistert").assertExists()
        compose.onNodeWithText("5 Fragen einen Kasten aufgestiegen").assertExists()
        compose.onNodeWithText("zurück in Kasten 1", substring = true).assertDoesNotExist()
    }

    @Test
    fun onboardingReportsThePickedState() {
        var picked: FederalState? = null
        compose.setContent { JustEinbuergerungstestTheme(dynamicColor = false) { OnboardingScreen(onSelect = { picked = it }) } }

        compose.onNodeWithText("Bayern").performClick()
        assertEquals(FederalState.BAVARIA, picked)
    }
}
