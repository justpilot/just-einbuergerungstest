package io.justpilot.justeinbuergerung

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.justpilot.justeinbuergerung.data.catalog.CatalogImporter
import io.justpilot.justeinbuergerung.data.catalog.CatalogParser
import io.justpilot.justeinbuergerung.data.db.AppDatabase
import io.justpilot.justeinbuergerung.data.repository.RoomProgressRepository
import io.justpilot.justeinbuergerung.data.repository.RoomQuestionRepository
import io.justpilot.justeinbuergerung.data.settings.DataStoreSettingsRepository
import io.justpilot.justeinbuergerung.domain.repository.ProgressRepository
import io.justpilot.justeinbuergerung.domain.repository.QuestionRepository
import io.justpilot.justeinbuergerung.domain.repository.SettingsRepository
import java.time.Clock

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Manual dependency injection: one instance per process, owned by [JustApplication]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { AppDatabase.create(appContext) }
    private val clock: Clock = Clock.systemDefaultZone()

    val catalogImporter by lazy {
        CatalogImporter(database) { appContext.assets.open(CatalogParser.ASSET_PATH) }
    }
    val questionRepository: QuestionRepository by lazy { RoomQuestionRepository(database) }
    val progressRepository: ProgressRepository by lazy { RoomProgressRepository(database, clock) }
    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(appContext.settingsDataStore) }
}
