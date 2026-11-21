package com.example.homepanel.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.homepanel.ui.theme.tui

// Смысл состояния -> цвет. Цвет всегда идёт вместе со значком и словом.
enum class StatusKind { OK, WAIT, ERROR, ACTIVE, MUTED }

@Composable
fun StatusKind.color(): Color = when (this) {
    StatusKind.OK -> MaterialTheme.tui.success
    StatusKind.WAIT -> MaterialTheme.tui.warning
    StatusKind.ERROR -> MaterialTheme.colorScheme.error
    StatusKind.ACTIVE -> MaterialTheme.colorScheme.primary
    StatusKind.MUTED -> MaterialTheme.tui.muted
}

// «✓ ready», «⏸ paused», «✗ offline»
@Composable
fun StatusBadge(icon: String, label: String, kind: StatusKind, modifier: Modifier = Modifier) {
    Text(
        text = "$icon $label",
        color = kind.color(),
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        modifier = modifier,
    )
}
