package io.justpilot.justeinbuergerung.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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

    private companion object {
        const val DB = "migration-test"
    }
}
