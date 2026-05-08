package com.kaijen.btpower.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaijen.btpower.data.bluetooth.BluetoothDeviceProvider
import com.kaijen.btpower.data.db.entities.SessionEntity
import com.kaijen.btpower.data.repositories.SelectedDevice
import com.kaijen.btpower.data.repositories.SelectedDeviceStore
import com.kaijen.btpower.data.repositories.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    private val selectedDeviceStore: SelectedDeviceStore,
    private val deviceProvider: BluetoothDeviceProvider,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        sessionRepository.observeSessions(),
        sessionRepository.observeActiveSession(),
        selectedDeviceStore.selectedDevice,
    ) { sessions, active, selected ->
        HomeUiState(
            sessions = sessions,
            activeSessionId = active?.id,
            selectedDevice = selected,
            isDeviceConnected = selected?.let { deviceProvider.isHeadsetConnected(it.macAddress) } == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), HomeUiState())

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

data class HomeUiState(
    val sessions: List<SessionEntity> = emptyList(),
    val activeSessionId: Long? = null,
    val selectedDevice: SelectedDevice? = null,
    val isDeviceConnected: Boolean = false,
)
