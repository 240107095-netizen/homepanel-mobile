package com.example.homepanel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Цвета состояний, которых нет в Material ColorScheme
@Immutable
data class TuiColors(
    val muted: Color = TextMuted,
    val success: Color = Success,
    val warning: Color = Warning,
    val line: Color = Line,
    val boost: Color = Boost,
)

val LocalTuiColors = staticCompositionLocalOf { TuiColors() }

private val TrueBlackScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TrueBlack,
    secondary = Accent,
    onSecondary = TrueBlack,
    background = TrueBlack,
    onBackground = Text,
    surface = TrueBlack,
    onSurface = Text,
    surfaceVariant = Boost,
    onSurfaceVariant = TextMuted,
    surfaceContainer = TrueBlack,
    surfaceContainerHigh = Boost,
    surfaceContainerHighest = Boost,
    secondaryContainer = Boost,
    onSecondaryContainer = Text,
    inverseSurface = Text,
    inverseOnSurface = TrueBlack,
    outline = Line,
    outlineVariant = Line,
    error = Error,
    onError = TrueBlack,
)

// Тема всегда тёмная: AMOLED-чёрный и в светлом режиме системы
@Composable
fun HomepanelTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTuiColors provides TuiColors()) {
        MaterialTheme(colorScheme = TrueBlackScheme, typography = Typography, content = content)
    }
}

// Короткий доступ: MaterialTheme.tui.success
val MaterialTheme.tui: TuiColors
    @Composable get() = LocalTuiColors.current
