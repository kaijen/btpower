package com.kaijen.btpower.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val macAddress: String,
    val name: String,
    val firstSeen: Instant,
)
