package io.justpilot.justeinbuergerung.data.db

import android.content.Context
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
    ],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao

    abstract fun progressDao(): ProgressDao

    abstract fun answerLogDao(): AnswerLogDao

    companion object {
        const val NAME = "just-einbuergerungstest.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
