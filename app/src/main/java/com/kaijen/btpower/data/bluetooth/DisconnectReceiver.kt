package com.kaijen.btpower.data.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Listens for ACL_DISCONNECTED of the target device. Registered at runtime from
 * [com.kaijen.btpower.service.TrackingService]; on a hit, the service is responsible for
 * flushing the final sample and calling `stopSelf()`.
 */
class DisconnectReceiver(
    private val targetMac: String,
    private val onDisconnected: () -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_DISCONNECTED) return
        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        if (device?.address != targetMac) return
        Timber.i("ACL_DISCONNECTED for tracked device %s", targetMac)
        onDisconnected()
    }
}
