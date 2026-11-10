package com.camera.qrscanner.barcode.ui.common

import android.app.Activity
import android.os.Build
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Toàn màn hình: ẩn thanh điều hướng, thanh trạng thái trong suốt. Vuốt từ mép để gọi lại thanh
 * điều hướng (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`). Dùng chung cho MainActivity và
 * MyLanguageActivity.
 *
 * Phải gọi lại ở `onWindowFocusChanged` chứ không chỉ ở `onCreate`: hệ thống trả lại thanh điều
 * hướng mỗi khi cửa sổ mất rồi lấy lại tiêu điểm (mở hộp thoại, kéo thanh thông báo, chuyển app).
 */
fun Activity.enableImmersiveFullScreen(lightBars: Boolean = false) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = android.graphics.Color.TRANSPARENT
    window.navigationBarColor = android.graphics.Color.TRANSPARENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
    }
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.show(WindowInsetsCompat.Type.statusBars())
    controller.hide(WindowInsetsCompat.Type.navigationBars())
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    // lightBars = true: nền SÁNG → icon thanh trạng thái/điều hướng TỐI. false: icon trắng (nền tối/xanh).
    controller.isAppearanceLightStatusBars = lightBars
    controller.isAppearanceLightNavigationBars = lightBars
}
