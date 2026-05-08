package com.kaijen.btpower.data.repositories

import com.kaijen.btpower.data.bluetooth.BatteryReading
import com.kaijen.btpower.data.db.SampleDao
import com.kaijen.btpower.data.db.SessionDao
import com.kaijen.btpower.data.db.entities.SampleEntity
import com.kaijen.btpower.data.db.entities.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val sampleDao: SampleDao,
    private val clock: Clock,
) {
    fun observeSessions(): Flow<List<SessionEntity>> = sessionDao.observeAll()

    fun observeActiveSession(): Flow<SessionEntity?> = sessionDao.observeActive()

    fun observeSession(id: Long): Flow<SessionEntity?> = sessionDao.observeById(id)

    fun observeSamples(sessionId: Long): Flow<List<SampleEntity>> =
        sampleDao.observeForSession(sessionId)

    suspend fun startSession(deviceMac: String): Long =
        sessionDao.insert(
            SessionEntity(
                deviceMac = deviceMac,
                startedAt = Instant.now(clock),
                endedAt = null,
            ),
        )

    suspend fun endSession(sessionId: Long) {
        sessionDao.markEnded(sessionId, Instant.now(clock))
    }

    suspend fun appendSample(sessionId: Long, reading: BatteryReading) {
        sampleDao.insert(
            SampleEntity(
                sessionId = sessionId,
                timestamp = reading.timestamp,
                mainLevel = reading.mainLevel,
                leftLevel = reading.leftLevel,
                rightLevel = reading.rightLevel,
                caseLevel = reading.caseLevel,
            ),
        )
    }

    suspend fun loadSessionsWithSamples(sessionIds: List<Long>): List<SessionWithSamples> {
        val sessions = sessionDao.findByIds(sessionIds)
        val samples = sampleDao.findForSessions(sessionIds).groupBy { it.sessionId }
        return sessions.map { SessionWithSamples(it, samples[it.id].orEmpty()) }
    }
}

data class SessionWithSamples(
    val session: SessionEntity,
    val samples: List<SampleEntity>,
)
