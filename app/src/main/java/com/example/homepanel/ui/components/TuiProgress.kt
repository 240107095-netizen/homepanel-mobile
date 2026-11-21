package com.example.homepanel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.homepanel.ui.formatPercent
import com.example.homepanel.ui.theme.tui

// Полоска «████▌░░░░ 54%» фиксированной ширины. Рисуем на Canvas ячейками,
// как блоки в терминале: символы █/░ в разных шрифтах разной ширины и полоски «прыгали».
@Composable
fun TuiProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    width: Dp = 160.dp,
    cells: Int = 20,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val track = MaterialTheme.tui.line
    val f = fraction.coerceIn(0f, 1f)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(
            Modifier
                .width(width)
                .height(10.dp),
        ) {
            val gap = 2.dp.toPx()
            val cell = (size.width - gap * (cells - 1)) / cells
            for (i in 0 until cells) {
                val x = i * (cell + gap)
                // Сколько этой ячейки заполнено: 0..1 (последняя может быть частичной)
                val fill = (f * cells - i).coerceIn(0f, 1f)
                drawRect(track, Offset(x, 0f), Size(cell, size.height))
                if (fill > 0f) drawRect(color, Offset(x, 0f), Size(cell * fill, size.height))
            }
        }
        Spacer(Modifier.width(8.dp))
        // Процент фиксированной ширины, чтобы строка не прыгала при обновлении
        Text(formatPercent(f).padStart(4), style = MaterialTheme.typography.bodyMedium)
    }
}
