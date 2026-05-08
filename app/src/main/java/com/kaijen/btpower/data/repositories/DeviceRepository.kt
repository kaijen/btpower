package com.kaijen.btpower.data.repositories

import com.kaijen.btpower.data.db.DeviceDao
import com.kaijen.btpower.data.db.entities.DeviceEntity
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val dao: DeviceDao,
    private val clock: Clock,
) {
    fun observeDevices(): Flow<List<DeviceEntity>> = dao.observeAll()

    suspend fun rememberDevice(macAddress: String, name: String) {
        val existing = dao.findByMac(macAddress)
        val firstSeen = existing?.firstSeen ?: Instant.now(clock)
        dao.upsert(DeviceEntity(macAddress = macAddress, name = name, firstSeen = firstSeen))
    }
}
