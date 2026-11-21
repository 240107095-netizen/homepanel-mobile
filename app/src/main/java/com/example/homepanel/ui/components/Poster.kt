package com.example.homepanel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import com.example.homepanel.ui.theme.tui

// Постер с пропорцией 2:3. Пока картинки нет — заглушка того же размера
// с первой буквой названия, чтобы раскладка не прыгала.
@Composable
fun Poster(
    name: String,
    modifier: Modifier = Modifier,
    width: Dp = 96.dp,
) {
    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .background(MaterialTheme.tui.boost),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.tui.muted,
        )
    }
}
