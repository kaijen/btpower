package com.kaijen.btpower.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["macAddress"],
            childColumns = ["deviceMac"],
        ),
    ],
    indices = [Index("deviceMac")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceMac: String,
    val startedAt: Instant,
    val endedAt: Instant?,
)
