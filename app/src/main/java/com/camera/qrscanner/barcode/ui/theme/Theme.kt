package com.camera.qrscanner.barcode.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// background/surface = trắng — khớp thực tế mọi màn hình đang dùng nền trắng (không dùng `Bg`
// #F5F5F5 cho nền toàn màn; `Bg`/`Bg2` để dành cho surface phụ nếu app mới cần).
private val LightColors = lightColorScheme(
    primary = Theme,
    secondary = ThemeSecond,
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Gray,
    tertiary = Orange,
)

private val DarkColors = darkColorScheme(
    primary = ThemeSecond,
    secondary = Theme,
    tertiary = Orange,
)

@Composable
fun QrScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
