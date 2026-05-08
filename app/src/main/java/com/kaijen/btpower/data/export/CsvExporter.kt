package com.kaijen.btpower.data.export

import com.kaijen.btpower.data.repositories.SessionWithSamples
import java.io.Writer

object CsvExporter {

    private const val HEADER =
        "session_id,device_mac,session_started_at,session_ended_at,sample_timestamp," +
            "main_level,left_level,right_level,case_level"

    fun write(sessions: List<SessionWithSamples>, writer: Writer) {
        writer.appendLine(HEADER)
        for (entry in sessions) {
            val s = entry.session
            for (sample in entry.samples) {
                writer.append(s.id.toString()).append(',')
                writer.append(s.deviceMac).append(',')
                writer.append(s.startedAt.toString()).append(',')
                writer.append(s.endedAt?.toString().orEmpty()).append(',')
                writer.append(sample.timestamp.toString()).append(',')
                writer.append(sample.mainLevel?.toString().orEmpty()).append(',')
                writer.append(sample.leftLevel?.toString().orEmpty()).append(',')
                writer.append(sample.rightLevel?.toString().orEmpty()).append(',')
                writer.append(sample.caseLevel?.toString().orEmpty())
                writer.append('\n')
            }
        }
        writer.flush()
    }
}
