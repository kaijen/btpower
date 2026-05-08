package com.kaijen.btpower.data.bluetooth

import java.time.Instant

data class BatteryReading(
    val timestamp: Instant,
    val mainLevel: Int?,
    val leftLevel: Int? = null,
    val rightLevel: Int? = null,
    val caseLevel: Int? = null,
) {
    val hasAnyLevel: Boolean
        get() = mainLevel != null || leftLevel != null || rightLevel != null || caseLevel != null
}
