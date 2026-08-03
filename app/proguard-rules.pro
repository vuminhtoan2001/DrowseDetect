# ============================================================================
# proguard-rules.pro — Rule RIÊNG của vga52a-kotlin (Camera Studio: chụp ảnh/video, chỉnh sửa ảnh,
# collage, wallpaper, thư viện).
#
# Rule dùng chung cho mọi app (base-application AAR, Billing, AdMob mediation, Gson, Crashlytics)
# đã tách sang `proguard-rules-base.pro` (xem `proguardFiles(...)` trong app/build.gradle.kts) —
# KHÔNG lặp lại ở đây. File này chỉ giữ rule cho SDK/model ĐẶC THÙ của app này.
#
# Khi clone sang app mới: xoá hết nội dung dưới đây, chỉ giữ 3 dòng comment trên + thêm rule cho SDK
# riêng của app mới (xem "công thức" cho SDK phổ biến ở docs/integration/16_ENCODE_R8_STANDARD.md).
# ============================================================================

# --- Model / data của app (Room entities + DTO Gson) ---
-keep class com.camera.studio.photoeditor.data.** { *; }

# --- CameraX / Media3 / Room (chụp ảnh-video + burn overlay + lưu lịch sử) ---
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**
