package io.justpilot.justeinbuergerung.ui

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.justpilot.justeinbuergerung.domain.learning.Activity
import io.justpilot.justeinbuergerung.domain.learning.LearningOverview
import io.justpilot.justeinbuergerung.domain.learning.SessionSelection
import io.justpilot.justeinbuergerung.ui.home.HomeContent
import io.justpilot.justeinbuergerung.ui.home.HomeUiState
import io.justpilot.justeinbuergerung.ui.theme.JustEinbürgerungstestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HomeContentTest {
    @get:Rule
    val compose = createComposeRule()

    // A Thursday, so the chart runs from Friday to today.
    private val today = LocalDate.of(2026, 10, 29)
    private val noActivity = Activity.of(emptyMap(), emptySet(), today)
    private val started = mutableListOf<SessionSelection>()

    private fun show(overview: LearningOverview, activity: Activity = noActivity) {
        compose.setContent {
            JustEinbürgerungstestTheme(dynamicColor = false) {
                HomeContent(HomeUiState.Ready(overview, activity), onStartSession = { started += it })
            }
        }
    }

    @Test
    fun streakAndTheAnswersOfTheLastSevenDaysAreShown() {
        val activity = Activity.of(mapOf(today to 12, today.minusDays(6) to 4), setOf(today.minusDays(1), today.minusDays(2)), today)
        show(LearningOverview(total = 310, new = 300, perBox = listOf(5, 5, 0), due = 5, readyToMaster = 0, readySoon = 0, readySoonInDays = 0, bookmarked = 0), activity)

        compose.onNodeWithText("2 Tage in Folge").assertExists()
        compose.onNodeWithText("Schließe heute eine Tages-Session ab, um deine Serie zu halten.").assertExists()
        compose.onNodeWithText("16 Antworten").assertExists()
        compose.onNodeWithContentDescription("Heute: 12 Antworten").assertExists()
        compose.onNodeWithContentDescription("Freitag: 4 Antworten").performScrollTo().performClick()
        compose.onNodeWithText("4").assertExists()
    }

    @Test
    fun boxesWithQuestionsStartASessionOfThatBox() {
        show(LearningOverview(total = 310, new = 295, perBox = listOf(0, 15, 0), due = 0, readyToMaster = 0, readySoon = 0, readySoonInDays = 0, bookmarked = 0))

        compose.onNodeWithText("Kasten 2").performScrollTo().performClick()
        compose.onNodeWithText("Kasten 1").assertHasNoClickAction()
        compose.onNodeWithText("10 Fragen starten").performScrollTo().performClick()

        assertEquals(listOf(SessionSelection.Box(2), SessionSelection.Daily), started)
    }

    @Test
    fun progressCountsBox2AsLearnedAndPointsToQuestionsReadyForMastery() {
        show(LearningOverview(total = 310, new = 200, perBox = listOf(10, 100, 0), due = 30, readyToMaster = 25, readySoon = 40, readySoonInDays = 1, bookmarked = 0))

        compose.onNodeWithText("100 von 310 Fragen gelernt").assertExists()
        compose.onNodeWithText("davon 0 gemeistert").assertExists()
        compose.onNodeWithText("25 Fragen aus Kasten 2 sind heute bereit für Kasten 3.").assertExists()
    }

    @Test
    fun withoutBox2QuestionsTheNextMilestoneIsShown() {
        show(LearningOverview(total = 310, new = 263, perBox = listOf(5, 0, 42), due = 0, readyToMaster = 0, readySoon = 0, readySoonInDays = 0, bookmarked = 0))

        compose.onNodeWithText("Noch 8 Fragen bis 50 gemeistert.").assertExists()
    }

    @Test
    fun box2QuestionsThatBecomeDueLaterAreAnnouncedWithTheirDay() {
        show(LearningOverview(total = 310, new = 300, perBox = listOf(5, 5, 0), due = 5, readyToMaster = 0, readySoon = 5, readySoonInDays = 2, bookmarked = 0))

        compose.onNodeWithText("5 Fragen aus Kasten 2 sind in 2 Tagen bereit für Kasten 3.").assertExists()
    }
}
