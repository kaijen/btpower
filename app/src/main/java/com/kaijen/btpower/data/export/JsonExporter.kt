package com.kaijen.btpower.data.export

import com.kaijen.btpower.data.repositories.SessionWithSamples
import java.io.Writer

/**
 * Minimal handwritten JSON serialization. Avoids `org.json` so the exporter is testable on
 * a plain JVM without Robolectric.
 */
object JsonExporter {

    fun write(sessions: List<SessionWithSamples>, writer: Writer) {
        writer.append('[')
        sessions.forEachIndexed { sessionIndex, entry ->
            if (sessionIndex > 0) writer.append(',')
            writeSession(entry, writer)
        }
        writer.append(']')
        writer.flush()
    }

    private fun writeSession(entry: SessionWithSamples, writer: Writer) {
        val s = entry.session
        writer.append('{')
        writeField(writer, "session_id", s.id.toString(), quote = false, first = true)
        writeField(writer, "device_mac", s.deviceMac, quote = true)
        writeField(writer, "started_at", s.startedAt.toString(), quote = true)
        writeField(
            writer,
            "ended_at",
            s.endedAt?.toString(),
            quote = true,
            allowNull = true,
        )
        writer.append(",\"samples\":[")
        entry.samples.forEachIndexed { sampleIndex, sample ->
            if (sampleIndex > 0) writer.append(',')
            writer.append('{')
            writeField(writer, "timestamp", sample.timestamp.toString(), quote = true, first = true)
            writeNullable(writer, "main_level", sample.mainLevel)
            writeNullable(writer, "left_level", sample.leftLevel)
            writeNullable(writer, "right_level", sample.rightLevel)
            writeNullable(writer, "case_level", sample.caseLevel)
            writer.append('}')
        }
        writer.append(']')
        writer.append('}')
    }

    private fun writeField(
        writer: Writer,
        key: String,
        value: String?,
        quote: Boolean,
        first: Boolean = false,
        allowNull: Boolean = false,
    ) {
        if (!first) writer.append(',')
        writer.append('"').append(key).append("\":")
        if (value == null) {
            check(allowNull) { "null not allowed for $key" }
            writer.append("null")
        } else if (quote) {
            writer.append('"').append(escape(value)).append('"')
        } else {
            writer.append(value)
        }
    }

    private fun writeNullable(writer: Writer, key: String, value: Int?) {
        writer.append(',').append('"').append(key).append("\":")
        if (value == null) writer.append("null") else writer.append(value.toString())
    }

    private fun escape(s: String): String {
        val sb = StringBuilder(s.length + 2)
        for (c in s) {
            when (c) {
                '\\', '"' -> { sb.append('\\'); sb.append(c) }
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        return sb.toString()
    }
}
