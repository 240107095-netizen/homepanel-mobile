package com.example.homepanel.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Весь текст моноширинный, как в терминале. Три уровня: жирное, обычное, тусклое (цветом).
private val Mono = FontFamily.Monospace

private fun mono(size: Int, weight: FontWeight = FontWeight.Normal, line: Int = size + 6) =
    TextStyle(fontFamily = Mono, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

val Typography = Typography(
    titleLarge = mono(18, FontWeight.Bold),
    titleMedium = mono(16, FontWeight.Bold),
    titleSmall = mono(14, FontWeight.Bold),
    bodyLarge = mono(15),
    bodyMedium = mono(14),
    bodySmall = mono(12),
    labelLarge = mono(14, FontWeight.Bold),
    labelMedium = mono(12),
    labelSmall = mono(11),
)
