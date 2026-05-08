package com.kaijen.btpower.service

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.kaijen.btpower.data.bluetooth.BatteryLevelReceiver
import com.kaijen.btpower.data.bluetooth.BatteryReading
import com.kaijen.btpower.data.bluetooth.DisconnectReceiver
import com.kaijen.btpower.data.repositories.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * Foreground service. Runs while a tracking session is active; stopped on disconnect or by
 * the user via the notification action.
 *
 * NOTE: Sampling source defaults to the [BatteryLevelReceiver] system broadcast (path #2 from
 * the M1 spike). Once the spike confirms the actual API path for the Valco NL25, swap the
 * source plug here.
 */
@AndroidEntryPoint
class TrackingService : Service() {

    @Inject lateinit var sessionRepository: SessionRepository

    @Inject lateinit var clock: Clock

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val latestReading = MutableStateFlow<BatteryReading?>(null)

    private var sessionId: Long = -1L
    private var targetMac: String? = null
    private var batteryReceiver: BatteryLevelReceiver? = null
    private var disconnectReceiver: DisconnectReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        TrackingNotification.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Timber.i("Stop action received")
                stopAndClose()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val mac = intent.getStringExtra(EXTRA_DEVICE_MAC)
                if (mac.isNullOrBlank()) {
                    Timber.w("Start without device mac, ignoring")
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (targetMac == null) startTracking(mac)
            }
        }
        return START_STICKY
    }

    private fun startTracking(mac: String) {
        targetMac = mac

        startInForeground()

        serviceScope.launch {
            sessionId = sessionRepository.startSession(mac)
            Timber.i("Tracking session %d started for %s", sessionId, mac)
            registerReceivers(mac)
            launchSamplingLoop()
        }
    }

    private fun startInForeground() {
        val notification = TrackingNotification.build(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                TrackingNotification.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(TrackingNotification.NOTIFICATION_ID, notification)
        }
    }

    private fun registerReceivers(mac: String) {
        batteryReceiver = BatteryLevelReceiver(mac, clock) { reading ->
            latestReading.value = reading
        }.also {
            ContextCompat.registerReceiver(
                this,
                it,
                IntentFilter(BatteryLevelReceiver.ACTION_BATTERY_LEVEL_CHANGED),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
        disconnectReceiver = DisconnectReceiver(mac) {
            Timber.i("Target device disconnected, ending session")
            stopAndClose()
        }.also {
            ContextCompat.registerReceiver(
                this,
                it,
                IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
    }

    private fun launchSamplingLoop() {
        serviceScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(SAMPLE_INTERVAL.inWholeMilliseconds)
                val cached = latestReading.value
                val toWrite = cached?.copy(timestamp = Instant.now(clock))
                if (toWrite != null) {
                    sessionRepository.appendSample(sessionId, toWrite)
                    Timber.d("Sample written for session %d", sessionId)
                } else {
                    Timber.d("No reading yet, skipping sample")
                }
            }
        }
    }

    private fun stopAndClose() {
        serviceScope.launch {
            try {
                latestReading.value?.let { reading ->
                    if (sessionId > 0) {
                        sessionRepository.appendSample(sessionId, reading.copy(timestamp = Instant.now(clock)))
                    }
                }
                if (sessionId > 0) sessionRepository.endSession(sessionId)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        batteryReceiver?.let { runCatching { unregisterReceiver(it) } }
        disconnectReceiver?.let { runCatching { unregisterReceiver(it) } }
        batteryReceiver = null
        disconnectReceiver = null
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.kaijen.btpower.action.START_TRACKING"
        const val ACTION_STOP = "com.kaijen.btpower.action.STOP_TRACKING"
        const val EXTRA_DEVICE_MAC = "device_mac"

        private val SAMPLE_INTERVAL = 5.minutes
    }
}
