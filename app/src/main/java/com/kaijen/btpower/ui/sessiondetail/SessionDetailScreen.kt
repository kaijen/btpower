package com.kaijen.btpower.ui.sessiondetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaijen.btpower.R
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Suppress("UnusedParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val zone = ZoneId.systemDefault()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.session_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            val session = state.session
            if (session != null) {
                Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Session #${session.id}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Started ${session.startedAt.atZone(zone).format(timeFormatter)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        session.endedAt?.let {
                            Text(
                                "Ended ${it.atZone(zone).format(timeFormatter)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            BatteryChart(samples = state.samples)
            HorizontalDivider()
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.samples, key = { it.id }) { sample ->
                    ListItem(
                        headlineContent = {
                            Text(sample.timestamp.atZone(zone).format(timeFormatter))
                        },
                        supportingContent = {
                            Text(
                                listOfNotNull(
                                    sample.mainLevel?.let { "main $it%" },
                                    sample.leftLevel?.let { "L $it%" },
                                    sample.rightLevel?.let { "R $it%" },
                                    sample.caseLevel?.let { "case $it%" },
                                ).joinToString(" · "),
                            )
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
