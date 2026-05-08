package com.kaijen.btpower.ui.home

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaijen.btpower.R
import com.kaijen.btpower.data.db.entities.SessionEntity
import com.kaijen.btpower.service.TrackingService
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenSession: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        floatingActionButton = {
            val active = state.activeSessionId != null
            val canStart = state.selectedDevice != null && state.isDeviceConnected
            ExtendedFloatingActionButton(
                onClick = {
                    val mac = state.selectedDevice?.macAddress
                    val intent = Intent(context, TrackingService::class.java).apply {
                        action = if (active) TrackingService.ACTION_STOP else TrackingService.ACTION_START
                        if (!active && mac != null) {
                            putExtra(TrackingService.EXTRA_DEVICE_MAC, mac)
                        }
                    }
                    if (active) {
                        context.startService(intent)
                    } else if (canStart) {
                        context.startForegroundService(intent)
                    }
                },
                text = {
                    Text(
                        if (active) stringResource(R.string.home_stop_tracking)
                        else stringResource(R.string.home_start_tracking),
                    )
                },
                icon = {},
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            DeviceStatusBar(state = state, onOpenSettings = onOpenSettings)
            Spacer(Modifier.height(8.dp))
            if (state.sessions.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn {
                    items(state.sessions, key = { it.id }) { session ->
                        SessionRow(session = session, onClick = { onOpenSession(session.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceStatusBar(state: HomeUiState, onOpenSettings: () -> Unit) {
    val device = state.selectedDevice
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (device == null) {
                    Text(stringResource(R.string.home_no_device_selected))
                } else {
                    Text(device.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (state.isDeviceConnected) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.home_action_select_device))
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.home_no_sessions))
    }
}

@Composable
private fun SessionRow(session: SessionEntity, onClick: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    ListItem(
        modifier = Modifier.padding(horizontal = 8.dp),
        headlineContent = {
            Text(session.startedAt.atZone(zone).format(formatter))
        },
        supportingContent = {
            val ended = session.endedAt
            Text(
                text = if (ended == null) "Active" else "Ended ${ended.atZone(zone).format(formatter)}",
            )
        },
        leadingContent = { Text("#${session.id}") },
        trailingContent = {
            TextButton(onClick = onClick) { Text("Open") }
        },
    )
}
