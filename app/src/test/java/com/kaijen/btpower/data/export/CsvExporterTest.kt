package com.kaijen.btpower.data.export

import com.kaijen.btpower.data.db.entities.SampleEntity
import com.kaijen.btpower.data.db.entities.SessionEntity
import com.kaijen.btpower.data.repositories.SessionWithSamples
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.time.Instant

class CsvExporterTest {

    @Test
    fun `writes header and rows for samples`() {
        val session = SessionEntity(
            id = 1L,
            deviceMac = "AA:BB:CC:DD:EE:FF",
            startedAt = Instant.parse("2026-05-08T10:00:00Z"),
            endedAt = Instant.parse("2026-05-08T11:00:00Z"),
        )
        val samples = listOf(
            SampleEntity(
                id = 1L,
                sessionId = 1L,
                timestamp = Instant.parse("2026-05-08T10:05:00Z"),
                mainLevel = 100,
                leftLevel = null,
                rightLevel = null,
                caseLevel = null,
            ),
            SampleEntity(
                id = 2L,
                sessionId = 1L,
                timestamp = Instant.parse("2026-05-08T10:10:00Z"),
                mainLevel = 90,
                leftLevel = 90,
                rightLevel = 80,
                caseLevel = 75,
            ),
        )
        val writer = StringWriter()
        CsvExporter.write(listOf(SessionWithSamples(session, samples)), writer)

        val expected = """
            session_id,device_mac,session_started_at,session_ended_at,sample_timestamp,main_level,left_level,right_level,case_level
            1,AA:BB:CC:DD:EE:FF,2026-05-08T10:00:00Z,2026-05-08T11:00:00Z,2026-05-08T10:05:00Z,100,,,
            1,AA:BB:CC:DD:EE:FF,2026-05-08T10:00:00Z,2026-05-08T11:00:00Z,2026-05-08T10:10:00Z,90,90,80,75
        """.trimIndent() + "\n"

        assertEquals(expected, writer.toString())
    }
}
