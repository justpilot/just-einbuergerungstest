package io.justpilot.justeinbuergerung.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate1To2KeepsProgressAndAddsSessionResults() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL("INSERT INTO progress (questionId, box, dueEpochDay, lastAnsweredAt, bookmarked) VALUES ('1', 2, 20000, 0, 1)")
            db.execSQL("INSERT INTO answer_log (questionId, answeredAt, answeredEpochDay, correct, source) VALUES ('1', 0, 20000, 1, 'DAILY')")
        }

        helper.runMigrationsAndValidate(DB, 2, true).use { db ->
            db.query("SELECT box, bookmarked FROM progress WHERE questionId = '1'").use {
                it.moveToFirst()
                assertEquals(2, it.getInt(0))
                assertEquals(1, it.getInt(1))
            }
            db.query("SELECT COUNT(*) FROM answer_log").use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
            db.execSQL("INSERT INTO session_results (kind, finishedAt, finishedEpochDay, correct, total) VALUES ('EXAM', 0, 20000, 20, 33)")
        }
    }

    @Test
    fun migrate2To3KeepsQuestionsAndProgressAndAddsTurkishColumns() {
        helper.createDatabase(DB, 2).use { db ->
            db.execSQL("INSERT INTO questions (id, number, state, category, imagePath, textDe, textEn, textUk, correctOption) VALUES ('1', 1, NULL, 'politics', NULL, 'Frage?', 'Question?', NULL, 'A')")
            db.execSQL("INSERT INTO options (questionId, optionId, position, textDe, textEn, textUk) VALUES ('1', 'A', 0, 'Antwort', 'Answer', NULL)")
            db.execSQL("INSERT INTO progress (questionId, box, dueEpochDay, lastAnsweredAt, bookmarked) VALUES ('1', 3, 20000, 0, 0)")
        }

        helper.runMigrationsAndValidate(DB, 3, true).use { db ->
            db.query("SELECT textDe, textTr FROM questions WHERE id = '1'").use {
                it.moveToFirst()
                assertEquals("Frage?", it.getString(0))
                assertTrue(it.isNull(1))
            }
            db.query("SELECT box FROM progress WHERE questionId = '1'").use {
                it.moveToFirst()
                assertEquals(3, it.getInt(0))
            }
            db.execSQL("UPDATE questions SET textTr = 'Soru?' WHERE id = '1'")
            db.execSQL("UPDATE options SET textTr = 'Cevap' WHERE questionId = '1'")
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
