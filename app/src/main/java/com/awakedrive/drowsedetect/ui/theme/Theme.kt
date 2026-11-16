package com.awakedrive.drowsedetect.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// App dùng khi lái xe (thường tối) nên chỉ có giao diện tối, không theo chế độ hệ thống.
private val AppColors = darkColorScheme(
    primary = BrandBlue,
    secondary = BrandCyan,
    tertiary = StateAmber,
    error = StateRed,
    background = Navy900,
    surface = Navy800,
    surfaceVariant = Navy700,
    onPrimary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
)

@Composable
fun AwakeDriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, typography = AppTypography, content = content)
}
