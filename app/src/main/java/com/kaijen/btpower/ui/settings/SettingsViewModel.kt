package com.kaijen.btpower.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaijen.btpower.data.bluetooth.BluetoothDeviceProvider
import com.kaijen.btpower.data.bluetooth.PairedDevice
import com.kaijen.btpower.data.repositories.DeviceRepository
import com.kaijen.btpower.data.repositories.SelectedDevice
import com.kaijen.btpower.data.repositories.SelectedDeviceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deviceProvider: BluetoothDeviceProvider,
    private val selectedDeviceStore: SelectedDeviceStore,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    private val pairedDevices = MutableStateFlow<List<PairedDevice>>(emptyList())

    val state: StateFlow<SettingsUiState> = combine(
        pairedDevices.asStateFlow(),
        selectedDeviceStore.selectedDevice,
    ) { paired, selected ->
        SettingsUiState(
            pairedDevices = paired,
            selectedDevice = selected,
            hasBluetoothPermission = deviceProvider.hasBluetoothConnectPermission(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), SettingsUiState())

    fun refreshPairedDevices() {
        pairedDevices.value = deviceProvider.pairedHeadsets()
    }

    fun selectDevice(device: PairedDevice) {
        viewModelScope.launch {
            deviceRepository.rememberDevice(device.macAddress, device.name)
            selectedDeviceStore.select(device.macAddress, device.name)
        }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

data class SettingsUiState(
    val pairedDevices: List<PairedDevice> = emptyList(),
    val selectedDevice: SelectedDevice? = null,
    val hasBluetoothPermission: Boolean = false,
)
