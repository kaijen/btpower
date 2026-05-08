package com.kaijen.btpower.data.repositories

import com.kaijen.btpower.data.bluetooth.BatteryReading
import com.kaijen.btpower.data.db.SampleDao
import com.kaijen.btpower.data.db.SessionDao
import com.kaijen.btpower.data.db.entities.SampleEntity
import com.kaijen.btpower.data.db.entities.SessionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SessionRepositoryTest {

    private val now = Instant.parse("2026-05-08T10:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val sessionDao: SessionDao = mockk(relaxed = true)
    private val sampleDao: SampleDao = mockk(relaxed = true)
    private val repo = SessionRepository(sessionDao, sampleDao, clock)

    @Test
    fun `startSession inserts session with current clock and null endedAt`() = runTest {
        coEvery { sessionDao.insert(any()) } returns 42L

        val id = repo.startSession("AA:BB:CC")

        assertEquals(42L, id)
        coVerify {
            sessionDao.insert(
                match<SessionEntity> {
                    it.deviceMac == "AA:BB:CC" && it.startedAt == now && it.endedAt == null
                },
            )
        }
    }

    @Test
    fun `endSession marks ended at current clock`() = runTest {
        repo.endSession(42L)
        coVerify { sessionDao.markEnded(42L, now) }
    }

    @Test
    fun `appendSample writes sample copying reading fields`() = runTest {
        val reading = BatteryReading(
            timestamp = Instant.parse("2026-05-08T10:05:00Z"),
            mainLevel = 80,
            leftLevel = 70,
            rightLevel = 65,
            caseLevel = 50,
        )
        repo.appendSample(42L, reading)
        coVerify {
            sampleDao.insert(
                match<SampleEntity> {
                    it.sessionId == 42L &&
                        it.timestamp == reading.timestamp &&
                        it.mainLevel == 80 &&
                        it.leftLevel == 70 &&
                        it.rightLevel == 65 &&
                        it.caseLevel == 50
                },
            )
        }
    }
}
