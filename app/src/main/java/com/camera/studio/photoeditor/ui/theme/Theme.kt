package com.camera.studio.photoeditor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette khớp ảnh reference app gốc (vga52a/screenshot/*.jpg) — nền tối, accent vàng cam.
val Primary = Color(0xFFF5C518)          // accent vàng cam chính (nút chụp/tab active/checkmark)
val ThemeSecond = Color(0xFFED9A16)      // accent vàng cam phụ (PRO MODE label...)
val OnPrimary = Color(0xFF1A1512)
val BgDark = Color(0xFF0E0E10)           // nền tổng thể (Home/Settings/Gallery...)
val SurfaceDark = Color(0xFF1C1815)      // card/surface (grid card Home, bottom sheet...)
val SurfaceVariantDark = Color(0xFF221E1A)
val TextPrimary = Color(0xFFEDEDED)      // text chính trên nền tối
val TextSecondary = Color(0xFF9B9B9B)    // text phụ/label section
val IconGray = Color(0xFFCFCFCF)

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = ThemeSecond,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
)

@Composable
fun AppTheme(
    // App dùng 1 theme tối cố định khớp ảnh reference gốc, không theo dark/light mode hệ thống.
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content,
    )
}
