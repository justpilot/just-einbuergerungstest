package io.justpilot.justeinbuergerung.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = TestScope(UnconfinedTestDispatcher())
    private val dataStore = PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
        folder.newFile("settings.preferences_pb")
    }
    private val repository = DataStoreSettingsRepository(dataStore)

    @Test
    fun `defaults before onboarding`() = scope.runTest {
        assertEquals(Settings(federalState = null, translationLanguage = Language.ENGLISH, hapticsEnabled = true), repository.settings.first())
    }

    @Test
    fun `stores every setting`() = scope.runTest {
        repository.setFederalState(FederalState.RHINELAND_PALATINATE)
        repository.setTranslationLanguage(Language.UKRAINIAN)
        repository.setHapticsEnabled(false)

        assertEquals(Settings(FederalState.RHINELAND_PALATINATE, Language.UKRAINIAN, hapticsEnabled = false), repository.settings.first())
    }

    @Test
    fun `German is not a translation language`() = scope.runTest {
        val error = runCatching { repository.setTranslationLanguage(Language.GERMAN) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun `unknown stored codes fall back to defaults`() = scope.runTest {
        dataStore.edit {
            it[stringPreferencesKey("federal_state")] = "XX"
            it[stringPreferencesKey("translation_language")] = "fr"
        }

        val settings = repository.settings.first()
        assertEquals(null, settings.federalState)
        assertEquals(Language.ENGLISH, settings.translationLanguage)
    }
}
