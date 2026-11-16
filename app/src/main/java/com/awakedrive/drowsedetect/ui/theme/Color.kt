package com.awakedrive.drowsedetect.ui.theme

import androidx.compose.ui.graphics.Color

// Màu thương hiệu port từ utils/Values/colors.js
val Theme = Color(0xFF013495)         // COLOR.THEME
val ThemeSecond = Color(0xFF045ecd)   // COLOR.THEME_SECOND
val Bg = Color(0xFFF5F5F5)            // COLOR.BG
val Bg2 = Color(0xFFE2F1FF)           // COLOR.BG2
val Orange = Color(0xFFFCAC38)        // COLOR.orange
val Border = Color(0xFFF3F2F7)        // COLOR.BORDER

// Màu accent riêng — không có slot tương ứng trong Material3 ColorScheme, khai tên riêng để vẫn
// sửa được 1 chỗ (đừng hardcode Color(0xFF...) lại tại nơi dùng).
val PremiumGold = Color(0xFFF9CB16)     // Icon/nút mở màn IAP (Premium)
val SettingIconTint = Color(0xFF30295E) // Icon các dòng trong màn Settings
