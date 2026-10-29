package io.justpilot.justeinbuergerung.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.Settings
import io.justpilot.justeinbuergerung.ui.settings.SettingsContent
import io.justpilot.justeinbuergerung.ui.theme.JustEinbürgerungstestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aboutNamesTheSourceCodeAndTheOfficialCatalogWithoutClaimingToBeOfficial() {
        compose.setContent {
            JustEinbürgerungstestTheme(dynamicColor = false) {
                SettingsContent(
                    settings = Settings(FederalState.BAVARIA, Language.ENGLISH, hapticsEnabled = true),
                    snackbar = SnackbarHostState(),
                    onFederalState = {},
                    onTranslationLanguage = {},
                    onHaptics = {},
                    onDynamicColor = {},
                    onReset = {},
                )
            }
        }

        compose.onNodeWithText("github.com/justpilot/just-einbuergerungstest", substring = true).performScrollTo()
        compose.onNodeWithText("Anlage 1 der Einbürgerungstestverordnung", substring = true).performScrollTo()
        compose.onNodeWithText("kein Angebot des BAMF", substring = true).performScrollTo()
    }
}
