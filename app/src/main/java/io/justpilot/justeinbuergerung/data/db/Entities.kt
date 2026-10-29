package io.justpilot.justeinbuergerung.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "questions", indices = [Index("state")])
data class QuestionEntity(
    @PrimaryKey val id: String,
    val number: Int,
    /** Federal state code, null for general questions. */
    val state: String?,
    val category: String,
    val imagePath: String?,
    val textDe: String,
    val textEn: String?,
    val textUk: String?,
    val correctOption: String,
)

@Entity(
    tableName = "options",
    primaryKeys = ["questionId", "optionId"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class OptionEntity(
    val questionId: String,
    val optionId: String,
    val position: Int,
    val textDe: String,
    val textEn: String?,
    val textUk: String?,
)

data class QuestionWithOptions(
    @Embedded val question: QuestionEntity,
    @Relation(parentColumn = "id", entityColumn = "questionId")
    val options: List<OptionEntity>,
)

/**
 * Learning progress. Deliberately not a foreign key to [QuestionEntity]: catalog imports upsert
 * questions and must never touch progress of questions that still exist.
 */
@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val questionId: String,
    val box: Int,
    /** Local epoch day on which the question is due again, null if never answered. */
    val dueEpochDay: Long?,
    /** Epoch milliseconds of the last answer, null if never answered. */
    val lastAnsweredAt: Long?,
    val bookmarked: Boolean,
)

@Entity(tableName = "answer_log", indices = [Index("answeredEpochDay")])
data class AnswerLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    /** Epoch milliseconds. */
    val answeredAt: Long,
    /** Local epoch day of [answeredAt] in the time zone the answer was given in. */
    val answeredEpochDay: Long,
    val correct: Boolean,
    val source: String,
)

/** Result of a finished session, e.g. an exam simulation. */
@Entity(tableName = "session_results", indices = [Index("kind")])
data class SessionResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    /** Epoch milliseconds. */
    val finishedAt: Long,
    /** Local epoch day of [finishedAt] in the time zone the session was finished in. */
    val finishedEpochDay: Long,
    val correct: Int,
    val total: Int,
)

/** Single-row table holding the version of the imported question catalog. */
@Entity(tableName = "catalog_info")
data class CatalogInfoEntity(
    @PrimaryKey val id: Int = 0,
    val version: String,
)

data class DayCount(val day: Long, val count: Int)
