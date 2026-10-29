package io.justpilot.justeinbuergerung.data.catalog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.justpilot.justeinbuergerung.data.db.AppDatabase
import io.justpilot.justeinbuergerung.data.db.ProgressEntity
import io.justpilot.justeinbuergerung.data.repository.RoomQuestionRepository
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogImporterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun importer(json: String) = CatalogImporter(database) { json.byteInputStream() }

    private fun question(id: String, text: String = "Frage $id?") =
        """{"id":"$id","number":${id.substringAfter('-').toInt()},"state":${if ('-' in id) "\"${id.substringBefore('-')}\"" else "null"},
        |"category":"${if ('-' in id) "state" else "politics"}","image":null,"question":{"de":"$text"},
        |"options":[{"id":"A","text":{"de":"a"}},{"id":"B","text":{"de":"b"}},{"id":"C","text":{"de":"c"}},{"id":"D","text":{"de":"d"}}],
        |"correct_option":"A"}""".trimMargin()

    private fun catalog(version: String, vararg questions: String) =
        """{"catalogVersion":"$version","questions":[${questions.joinToString(",")}]}"""

    @Test
    fun importsOnlyWhenTheVersionChanges() = runTest {
        assertTrue(importer(catalog("v1", question("1"))).importIfNeeded())
        assertFalse(importer(catalog("v1", question("1"))).importIfNeeded())
        assertTrue(importer(catalog("v2", question("1"))).importIfNeeded())
        assertEquals("v2", database.questionDao().catalogVersion())
    }

    @Test
    fun updateKeepsProgressAndRemovesDeletedQuestions() = runTest {
        importer(catalog("v1", question("1"), question("2"))).importIfNeeded()
        val progressDao = database.progressDao()
        progressDao.upsert(ProgressEntity("1", box = 3, dueEpochDay = 100, lastAnsweredAt = 1, bookmarked = true))
        progressDao.upsert(ProgressEntity("2", box = 2, dueEpochDay = 100, lastAnsweredAt = 1, bookmarked = false))

        importer(catalog("v2", question("1", text = "Neue Frage?"), question("3"))).importIfNeeded()

        assertEquals(listOf("1", "3"), database.questionDao().allIds().sorted())
        assertEquals(3, progressDao.get("1")?.box)
        assertNull(progressDao.get("2"))
        val updated = database.questionDao().byIds(listOf("1")).single()
        assertEquals("Neue Frage?", updated.question.textDe)
        assertEquals(4, updated.options.size)
    }

    @Test
    fun bundledCatalogYieldsTheStatePools() = runTest {
        CatalogImporter(database) { context.assets.open(CatalogParser.ASSET_PATH) }.importIfNeeded()
        val repository = RoomQuestionRepository(database)

        for (state in FederalState.entries) {
            val pool = repository.questionPool(state).first()
            assertEquals("pool of $state", 310, pool.size)
            assertEquals((1..300).map(Int::toString) + (1..10).map { "${state.code}-$it" }, pool.map { it.id })
        }
        val first = repository.questions(listOf("1")).single()
        assertEquals(listOf("A", "B", "C", "D"), first.options.map { it.id })
        assertNotNull(first.text[Language.UKRAINIAN])
    }
}
