package io.justpilot.justeinbuergerung.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.Palette
import io.justpilot.justeinbuergerung.domain.model.Settings
import io.justpilot.justeinbuergerung.domain.model.ThemeMode
import io.justpilot.justeinbuergerung.ui.settings.SettingsContent
import io.justpilot.justeinbuergerung.ui.theme.JustEinbuergerungstestTheme
import org.junit.Assert.assertEquals
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
            JustEinbuergerungstestTheme(dynamicColor = false) {
                SettingsContent(
                    settings = Settings(FederalState.BAVARIA, Language.ENGLISH, hapticsEnabled = true),
                    snackbar = SnackbarHostState(),
                    onFederalState = {},
                    onTranslationLanguage = {},
                    onHaptics = {},
                    onThemeMode = {},
                    onPalette = {},
                    onReset = {},
                )
            }
        }

        compose.onNodeWithText("github.com/justpilot/just-einbuergerungstest", substring = true).performScrollTo()
        compose.onNodeWithText("Anlage 1 der Einbürgerungstestverordnung", substring = true).performScrollTo()
        compose.onNodeWithText("kein Angebot des BAMF", substring = true).performScrollTo()
    }

    @Test
    fun theLicensesOfTheBundledLibrariesCanBeShown() {
        compose.setContent {
            JustEinbuergerungstestTheme(dynamicColor = false) {
                SettingsContent(
                    settings = Settings(FederalState.BAVARIA, Language.ENGLISH, hapticsEnabled = true),
                    snackbar = SnackbarHostState(),
                    onFederalState = {},
                    onTranslationLanguage = {},
                    onHaptics = {},
                    onThemeMode = {},
                    onPalette = {},
                    onReset = {},
                )
            }
        }

        compose.onNodeWithText("Open-Source-Lizenzen").performScrollTo().performClick()
        compose.onNodeWithText("Apache License 2.0", substring = true).assertExists()
        compose.onNodeWithText("Schließen").performClick()
        compose.onNodeWithText("Apache License 2.0", substring = true).assertDoesNotExist()
    }

    @Test
    fun lightOrDarkAndThePaletteCanBeChosen() {
        var mode: ThemeMode? = null
        var palette: Palette? = null
        compose.setContent {
            JustEinbuergerungstestTheme(dynamicColor = false) {
                SettingsContent(
                    settings = Settings(FederalState.BAVARIA, Language.ENGLISH, hapticsEnabled = true),
                    snackbar = SnackbarHostState(),
                    onFederalState = {},
                    onTranslationLanguage = {},
                    onHaptics = {},
                    onThemeMode = { mode = it },
                    onPalette = { palette = it },
                    onReset = {},
                )
            }
        }

        compose.onNodeWithText("System").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Dunkel").performClick()
        assertEquals(ThemeMode.DARK, mode)
        compose.onNodeWithText("Blau").performScrollTo().performClick()
        assertEquals(Palette.BLUE, palette)
    }
}
