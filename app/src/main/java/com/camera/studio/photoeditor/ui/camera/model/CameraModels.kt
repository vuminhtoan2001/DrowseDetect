package com.camera.studio.photoeditor.ui.camera.model

import androidx.camera.video.Quality

/** 7 preset — khớp `take_photo_wb.jpg`, chia cố định 2 hàng (4+3) ở UI, không cuộn. */
enum class WhiteBalancePreset(val label: String, val matrix: FloatArray?) {
    AUTO("Auto", null),
    SUNNY("Sunny", floatArrayOf(1.06f, 0f, 0f, 0f, 6f, 0f, 1.0f, 0f, 0f, 2f, 0f, 0f, 0.94f, 0f, -6f, 0f, 0f, 0f, 1f, 0f)),
    CLOUDY("Cloudy", floatArrayOf(1.1f, 0f, 0f, 0f, 8f, 0f, 1.0f, 0f, 0f, 2f, 0f, 0f, 0.9f, 0f, -8f, 0f, 0f, 0f, 1f, 0f)),
    SHADE("Shade", floatArrayOf(1.14f, 0f, 0f, 0f, 10f, 0f, 1.0f, 0f, 0f, 0f, 0f, 0f, 0.86f, 0f, -10f, 0f, 0f, 0f, 1f, 0f)),
    TUNGSTEN("Tungsten", floatArrayOf(0.85f, 0f, 0f, 0f, -12f, 0f, 0.95f, 0f, 0f, -4f, 0f, 0f, 1.2f, 0f, 14f, 0f, 0f, 0f, 1f, 0f)),
    FLUORESCENT("Fluorescent", floatArrayOf(0.92f, 0f, 0f, 0f, -4f, 0f, 1.0f, 0f, 0f, 2f, 0f, 0f, 1.08f, 0f, 4f, 0f, 0f, 0f, 1f, 0f)),
    FLASH("Flash", floatArrayOf(1.02f, 0f, 0f, 0f, 2f, 0f, 1.0f, 0f, 0f, 0f, 0f, 0f, 0.98f, 0f, -2f, 0f, 0f, 0f, 1f, 0f)),
}

/** 3s/5s/10s + tắt — icon `ic_alarm_clock_3s/5s/10s.svg` xác nhận đúng bộ giá trị này. */
enum class CameraTimerOption(val label: String, val seconds: Int) {
    OFF("Off", 0), S3("3s", 3), S5("5s", 5), S10("10s", 10),
}

/** FHD/2K/4K — khớp `Screenshot_20260724_094705_*.jpg`. 4K khoá premium (asset `ic_premium_hd.png`). */
enum class ResolutionOption(val label: String, val widthPx: Int, val heightPx: Int, val quality: Quality, val isPremium: Boolean) {
    FHD("FHD", 1920, 1080, Quality.FHD, false),
    TWO_K("2K", 2560, 1440, Quality.HIGHEST, false),
    FOUR_K("4K", 3840, 2160, Quality.UHD, true),
}

/** 5 chế độ lấy nét — khớp bộ icon `ic_focus/ic_center_focus/ic_focus_detection/ic_infinity_focus/
 * ic_macro_focus.svg`. CameraX không lộ API bật/tắt macro/infinity trực tiếp — hiện chỉ đổi UI state,
 * AUTO/CENTER dùng tap-to-focus thật (MeteringPoint), INFINITY/MACRO/DETECTION là lựa chọn hiển thị. */
enum class FocusMode(val label: String) {
    AUTO("Auto"), CENTER("Center"), DETECTION("Detect"), INFINITY("Infinity"), MACRO("Macro"),
}

enum class MoreMenuItem { GRID, FOCUS, EXPOSURE, RESOLUTION, COLLAGE }
