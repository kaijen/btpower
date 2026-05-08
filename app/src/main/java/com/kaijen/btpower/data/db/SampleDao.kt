package com.kaijen.btpower.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kaijen.btpower.data.db.entities.SampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleDao {

    @Insert
    suspend fun insert(sample: SampleEntity): Long

    @Query("SELECT * FROM samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeForSession(sessionId: Long): Flow<List<SampleEntity>>

    @Query("SELECT * FROM samples WHERE sessionId IN (:sessionIds) ORDER BY timestamp ASC")
    suspend fun findForSessions(sessionIds: List<Long>): List<SampleEntity>

    @Query("SELECT COUNT(*) FROM samples WHERE sessionId = :sessionId")
    suspend fun countForSession(sessionId: Long): Int
}
