package io.justpilot.justeinbuergerung.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    /** General questions first (by number), then those of [state]. */
    @Transaction
    @Query("SELECT * FROM questions WHERE state IS NULL OR state = :state ORDER BY state IS NOT NULL, number")
    fun observePool(state: String): Flow<List<QuestionWithOptions>>

    @Transaction
    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun byIds(ids: Collection<String>): List<QuestionWithOptions>

    @Query("SELECT id FROM questions")
    suspend fun allIds(): List<String>

    @Upsert
    suspend fun upsertQuestions(questions: List<QuestionEntity>)

    @Upsert
    suspend fun upsertOptions(options: List<OptionEntity>)

    /** Options cascade. */
    @Query("DELETE FROM questions WHERE id IN (:ids)")
    suspend fun deleteQuestions(ids: Collection<String>)

    @Query("SELECT version FROM catalog_info WHERE id = 0")
    suspend fun catalogVersion(): String?

    @Upsert
    suspend fun setCatalogInfo(info: CatalogInfoEntity)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress")
    fun observeAll(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE questionId = :questionId")
    suspend fun get(questionId: String): ProgressEntity?

    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Query("DELETE FROM progress WHERE questionId IN (:questionIds)")
    suspend fun delete(questionIds: Collection<String>)

    @Query("DELETE FROM progress")
    suspend fun deleteAll()
}

@Dao
interface AnswerLogDao {
    @Insert
    suspend fun insert(entry: AnswerLogEntity)

    @Query(
        "SELECT answeredEpochDay AS day, COUNT(*) AS count FROM answer_log " +
            "WHERE answeredEpochDay >= :fromEpochDay GROUP BY answeredEpochDay",
    )
    fun observeCountsPerDay(fromEpochDay: Long): Flow<List<DayCount>>

    @Query("DELETE FROM answer_log")
    suspend fun deleteAll()
}

@Dao
interface SessionResultDao {
    @Insert
    suspend fun insert(result: SessionResultEntity)

    /** Newest first. */
    @Query("SELECT * FROM session_results WHERE kind = :kind ORDER BY finishedAt DESC")
    fun observe(kind: String): Flow<List<SessionResultEntity>>

    @Query("DELETE FROM session_results")
    suspend fun deleteAll()
}
