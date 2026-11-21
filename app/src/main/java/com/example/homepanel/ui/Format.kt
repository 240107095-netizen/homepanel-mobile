package com.example.homepanel.ui

import java.util.Locale

// Человеческие числа: 14.7 GB, 3.1 MB/s, 54%. Единицы латиницей во всех языках.
private val units = listOf("B", "KB", "MB", "GB", "TB")

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return String.format(Locale.US, if (value >= 100) "%.0f %s" else "%.1f %s", value, units[unit])
}

fun formatSpeed(bytesPerSecond: Long): String = formatBytes(bytesPerSecond) + "/s"

fun formatPercent(fraction: Float): String = "${(fraction * 100).toInt()}%"

// ETA: «2h 05m», «12m», «45s»
fun formatEta(seconds: Long): String = when {
    seconds <= 0 -> "—"
    seconds >= 3600 -> String.format(Locale.US, "%dh %02dm", seconds / 3600, seconds % 3600 / 60)
    seconds >= 60 -> "${seconds / 60}m"
    else -> "${seconds}s"
}
