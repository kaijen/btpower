package com.kaijen.btpower.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kaijen.btpower.data.db.entities.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("UPDATE sessions SET endedAt = :endedAt WHERE id = :id")
    suspend fun markEnded(id: Long, endedAt: Instant)

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<Long>): List<SessionEntity>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Long)
}
