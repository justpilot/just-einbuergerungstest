package io.justpilot.justeinbuergerung.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.Settings
import io.justpilot.justeinbuergerung.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {
    override val settings: Flow<Settings> = dataStore.data.map { preferences ->
        Settings(
            // Unknown codes (e.g. from a future version after a downgrade) fall back to the defaults.
            federalState = preferences[FEDERAL_STATE]?.let { code -> FederalState.entries.firstOrNull { it.code == code } },
            translationLanguage = preferences[TRANSLATION_LANGUAGE]
                ?.let { code -> Language.translations.firstOrNull { it.code == code } }
                ?: DEFAULT_TRANSLATION_LANGUAGE,
            hapticsEnabled = preferences[HAPTICS_ENABLED] ?: true,
        )
    }

    override suspend fun setFederalState(state: FederalState) {
        dataStore.edit { it[FEDERAL_STATE] = state.code }
    }

    override suspend fun setTranslationLanguage(language: Language) {
        require(language in Language.translations) { "Not a translation language: $language" }
        dataStore.edit { it[TRANSLATION_LANGUAGE] = language.code }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTICS_ENABLED] = enabled }
    }

    private companion object {
        val FEDERAL_STATE = stringPreferencesKey("federal_state")
        val TRANSLATION_LANGUAGE = stringPreferencesKey("translation_language")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val DEFAULT_TRANSLATION_LANGUAGE = Language.ENGLISH
    }
}
