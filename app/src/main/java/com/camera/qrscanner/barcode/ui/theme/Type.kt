package com.camera.qrscanner.barcode.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Chuẩn cỡ chữ dùng chung toàn app — SỬA Ở ĐÂY khi cần đổi cỡ/độ đậm chữ, KHÔNG set `fontSize`/
 * `fontWeight` tay tại từng màn hình. Mọi Composable chỉ gọi `MaterialTheme.typography.<role>`.
 * Tên vai trò khớp Material3 — map theo docs/03_STYLE_APP.md §3 khi build app mới (đổi giá trị ở
 * đây theo Style Guide app mới, không đổi tại nơi dùng).
 */
val AppTypography = Typography(
    // Header/Hero — tiêu đề card lớn, ít dùng
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    // Tiêu đề màn (AppBar title, tiêu đề slide Intro, tiêu đề dialog)
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    // Subtitle / tiêu đề nhóm (section header)
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    // Body chính (label hàng Setting/Permission)
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    // Body phụ / mô tả
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    // Caption (version text, phụ chú nhỏ)
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    // Chữ nút hành động chính
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
)
