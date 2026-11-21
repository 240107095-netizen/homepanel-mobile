package com.example.homepanel.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.homepanel.ui.formatPercent
import com.example.homepanel.ui.theme.tui

// Полоска из символов блоков + процент: «████████▌      54%».
// Восьмые доли блока дают плавность даже на узкой полоске.
private val partials = listOf("", "▏", "▎", "▍", "▌", "▋", "▊", "▉")

fun progressBarText(fraction: Float, width: Int): Pair<String, String> {
    val f = fraction.coerceIn(0f, 1f)
    val eighths = (f * width * 8).toInt()
    val full = eighths / 8
    val filled = "█".repeat(full) + if (full < width) partials[eighths % 8] else ""
    return filled to "░".repeat((width - filled.length).coerceAtLeast(0))
}

@Composable
fun TuiProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    width: Int = 16,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val (filled, empty) = progressBarText(fraction, width)
    val track = MaterialTheme.tui.line
    Row(modifier) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = color)) { append(filled) }
                withStyle(SpanStyle(color = track)) { append(empty) }
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.width(8.dp))
        // Процент фиксированной ширины, чтобы строка не прыгала при обновлении
        Text(formatPercent(fraction).padStart(4), style = MaterialTheme.typography.bodyMedium)
    }
}
