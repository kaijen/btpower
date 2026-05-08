package com.kaijen.btpower.data.export

import com.kaijen.btpower.data.db.entities.SampleEntity
import com.kaijen.btpower.data.db.entities.SessionEntity
import com.kaijen.btpower.data.repositories.SessionWithSamples
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.time.Instant

class JsonExporterTest {

    @Test
    fun `writes array with session and samples`() {
        val session = SessionEntity(
            id = 7L,
            deviceMac = "AA:BB",
            startedAt = Instant.parse("2026-05-08T10:00:00Z"),
            endedAt = null,
        )
        val sample = SampleEntity(
            id = 1L,
            sessionId = 7L,
            timestamp = Instant.parse("2026-05-08T10:05:00Z"),
            mainLevel = 80,
            leftLevel = null,
            rightLevel = null,
            caseLevel = null,
        )
        val writer = StringWriter()
        JsonExporter.write(listOf(SessionWithSamples(session, listOf(sample))), writer)

        val expected =
            """[{"session_id":7,"device_mac":"AA:BB","started_at":"2026-05-08T10:00:00Z","ended_at":null,""" +
                """"samples":[{"timestamp":"2026-05-08T10:05:00Z","main_level":80,"left_level":null,""" +
                """"right_level":null,"case_level":null}]}]"""

        assertEquals(expected, writer.toString())
    }

    @Test
    fun `escapes special characters in strings`() {
        val session = SessionEntity(
            id = 1L,
            deviceMac = "Mac\"with\\quote",
            startedAt = Instant.parse("2026-05-08T10:00:00Z"),
            endedAt = null,
        )
        val writer = StringWriter()
        JsonExporter.write(listOf(SessionWithSamples(session, emptyList())), writer)

        // Verify quote and backslash were escaped.
        val s = writer.toString()
        assertEquals(true, s.contains("""Mac\"with\\quote"""))
    }
}
