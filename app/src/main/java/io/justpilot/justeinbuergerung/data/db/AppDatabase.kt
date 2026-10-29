package io.justpilot.justeinbuergerung.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        QuestionEntity::class,
        OptionEntity::class,
        ProgressEntity::class,
        AnswerLogEntity::class,
        CatalogInfoEntity::class,
        SessionResultEntity::class,
    ],
    version = 3,
    autoMigrations = [
        // 2: session_results for the exam history.
        AutoMigration(from = 1, to = 2),
        // 3: Turkish translation columns.
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao

    abstract fun progressDao(): ProgressDao

    abstract fun answerLogDao(): AnswerLogDao

    abstract fun sessionResultDao(): SessionResultDao

    companion object {
        const val NAME = "just-einbuergerungstest.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
