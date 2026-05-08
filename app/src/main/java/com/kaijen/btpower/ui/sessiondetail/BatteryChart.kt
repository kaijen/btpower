package com.kaijen.btpower.ui.sessiondetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kaijen.btpower.data.db.entities.SampleEntity

/**
 * Placeholder until M4 wires Vico in. Renders a simple text summary of samples so the
 * detail screen has a visible chart slot. Replace with a Vico LineChart when M3 has real
 * data flowing.
 */
@Composable
fun BatteryChart(samples: List<SampleEntity>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (samples.isEmpty()) {
            Text("No samples yet", style = MaterialTheme.typography.bodyMedium)
        } else {
            val first = samples.first().mainLevel
            val last = samples.last().mainLevel
            Text(
                text = "Samples: ${samples.size}, first ${first ?: "?"}%, last ${last ?: "?"}%",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
