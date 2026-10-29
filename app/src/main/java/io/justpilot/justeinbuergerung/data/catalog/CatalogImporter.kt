package io.justpilot.justeinbuergerung.data.catalog

import androidx.room.withTransaction
import io.justpilot.justeinbuergerung.data.db.AppDatabase
import io.justpilot.justeinbuergerung.data.db.CatalogInfoEntity
import io.justpilot.justeinbuergerung.data.db.toEntities
import java.io.InputStream

/**
 * Copies the bundled catalog into the database whenever its version differs from the imported one.
 * Questions are upserted so learning progress (keyed by question id) survives catalog updates;
 * questions that disappeared from the catalog are deleted together with their progress.
 */
class CatalogImporter(
    private val database: AppDatabase,
    private val openCatalog: () -> InputStream,
) {
    /** @return true if the database was updated */
    suspend fun importIfNeeded(): Boolean {
        val catalog = CatalogParser.parse(openCatalog())
        val questionDao = database.questionDao()
        if (questionDao.catalogVersion() == catalog.version) return false

        database.withTransaction {
            val newIds = catalog.questions.mapTo(HashSet()) { it.id }
            val removedIds = questionDao.allIds().filterNot { it in newIds }
            questionDao.deleteQuestions(removedIds)
            database.progressDao().delete(removedIds)

            val entities = catalog.questions.map { it.toEntities() }
            questionDao.upsertQuestions(entities.map { it.question })
            questionDao.upsertOptions(entities.flatMap { it.options })
            questionDao.setCatalogInfo(CatalogInfoEntity(version = catalog.version))
        }
        return true
    }
}
