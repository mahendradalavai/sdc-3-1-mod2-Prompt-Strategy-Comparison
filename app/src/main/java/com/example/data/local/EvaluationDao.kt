package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluationDao {
    @Query("SELECT * FROM evaluation_runs ORDER BY timestamp DESC")
    fun getAllRuns(): Flow<List<EvaluationEntity>>

    @Query("SELECT * FROM evaluation_runs WHERE id = :runId LIMIT 1")
    suspend fun getRunById(runId: String): EvaluationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: EvaluationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuns(runs: List<EvaluationEntity>)

    @Query("DELETE FROM evaluation_runs WHERE id = :runId")
    suspend fun deleteRunById(runId: String)

    @Query("DELETE FROM evaluation_runs")
    suspend fun clearAllRuns()

    @Query("SELECT COUNT(*) FROM evaluation_runs")
    suspend fun getRunCount(): Int
}
