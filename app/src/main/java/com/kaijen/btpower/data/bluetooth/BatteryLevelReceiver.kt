package com.kaijen.btpower.data.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber
import java.time.Clock
import java.time.Instant

/**
 * Listens for the (officially undocumented) system broadcast
 * `android.bluetooth.device.action.BATTERY_LEVEL_CHANGED`. Path #2 from the M1 spike.
 *
 * Registered at runtime from [com.kaijen.btpower.service.TrackingService]. Not declared in
 * the manifest because Android disallows manifest registration for this implicit broadcast
 * for 3rd-party apps on recent versions.
 */
class BatteryLevelReceiver(
    private val targetMac: String,
    private val clock: Clock,
    private val onReading: (BatteryReading) -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_BATTERY_LEVEL_CHANGED) return
        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        if (device?.address != targetMac) return

        val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, BATTERY_LEVEL_UNKNOWN)
        if (level == BATTERY_LEVEL_UNKNOWN || level !in 0..100) {
            Timber.d("BATTERY_LEVEL_CHANGED received but level invalid: %d", level)
            return
        }
        Timber.d("BATTERY_LEVEL_CHANGED for %s -> %d%%", targetMac, level)
        onReading(BatteryReading(timestamp = Instant.now(clock), mainLevel = level))
    }

    companion object {
        const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
        const val BATTERY_LEVEL_UNKNOWN = -1
    }
}
