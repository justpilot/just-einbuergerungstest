package io.justpilot.justeinbuergerung.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.justpilot.justeinbuergerung.domain.learning.LearningOverview
import io.justpilot.justeinbuergerung.ui.home.HomeContent
import io.justpilot.justeinbuergerung.ui.home.HomeUiState
import io.justpilot.justeinbuergerung.ui.theme.JustEinbürgerungstestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(overview: LearningOverview) {
        compose.setContent {
            JustEinbürgerungstestTheme(dynamicColor = false) { HomeContent(HomeUiState.Ready(overview), onStartDaily = {}) }
        }
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
