package com.kaijen.btpower.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothDeviceProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adapter: BluetoothAdapter?,
) {
    fun hasBluetoothConnectPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun pairedHeadsets(): List<PairedDevice> {
        val a = adapter ?: return emptyList()
        if (!hasBluetoothConnectPermission()) return emptyList()
        if (!a.isEnabled) return emptyList()
        return a.bondedDevices.orEmpty().map {
            PairedDevice(macAddress = it.address, name = it.name ?: it.address)
        }
    }

    @SuppressLint("MissingPermission")
    fun isHeadsetConnected(macAddress: String): Boolean {
        val a = adapter ?: return false
        if (!hasBluetoothConnectPermission()) return false
        val state = a.getProfileConnectionState(BluetoothProfile.HEADSET)
        if (state != BluetoothProfile.STATE_CONNECTED) return false
        return a.bondedDevices.orEmpty().any { it.address == macAddress && it.isEffectivelyConnected() }
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.isEffectivelyConnected(): Boolean = try {
        val method = this.javaClass.getMethod("isConnected")
        method.invoke(this) as? Boolean ?: false
    } catch (_: ReflectiveOperationException) {
        false
    }
}

data class PairedDevice(
    val macAddress: String,
    val name: String,
)
