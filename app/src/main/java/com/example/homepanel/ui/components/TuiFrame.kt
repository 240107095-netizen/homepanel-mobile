package com.example.homepanel.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.homepanel.ui.theme.tui

// Рамка как в терминале: прямые углы, тонкая линия.
// Рамку получает только то, что в фокусе (поле поиска, выбранная карточка).
@Composable
fun TuiFrame(
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.tui.line
    Box(
        modifier = modifier
            .border(1.dp, color, RectangleShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        content = content,
    )
}
