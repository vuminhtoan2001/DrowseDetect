package com.camera.studio.photoeditor.core

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.camera.studio.photoeditor.advertisement.NativeInterHost
import com.camera.studio.photoeditor.firebase.Remote
import com.camera.studio.photoeditor.ui.nav.AppNavHost
import com.camera.studio.photoeditor.ui.nav.Screen
import com.camera.studio.photoeditor.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Home Activity — lib base-application mở qua `getHomeActivity()` sau luồng Splash→Inter→
 * Language→IAP. KHÔNG khai MAIN/LAUNCHER (xem AndroidManifest).
 *
 * Quyết định start destination (port logic gate Intro theo số lần mở + nguồn cài, giống
 * vga31b/vga34b-kotlin):
 *  - Gate Intro theo số lần mở (goToHomeNumber) + nguồn cài (isAdsCampaign):
 *      • Ad campaign:  Intro khi goToHomeNumber < count_app_open.
 *      • Organic:      Intro khi count_app_open ≤ goToHomeNumber < count_app_open + organic_number_not_guide.
 *    (Language do lib quản; đây chỉ chi phối màn Intro của app.)
 *  - Dù goToHomeStatus bỏ qua Intro (user cũ/organic), vẫn phải xin đủ quyền Camera/Micro trước khi
 *    vào Home, nếu không Camera sẽ đứng hình (preview đen) không có lối yêu cầu lại.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppStorage.language(this)?.let { LocaleHelper.updateLocale(this, it) }

        enableImmersiveFullScreen()

        val startRoute = resolveStartRoute()

        setContent {
            AppTheme {
                androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    AppNavHost(startRoute = startRoute)
                }
                // Overlay hiển thị native-interstitial (fallback sau inter) khi AdManager yêu cầu.
                NativeInterHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun resolveStartRoute(): String {
        val remote = Remote.instance
        val n = AppStorage.goToHomeNumber(this)              // lần mở hiện tại (≥1)
        AppStorage.setGoToHomeNumber(this, n + 1)            // +1 cho lần sau

        val countAppOpen = remote.getInt("count_app_open").let { if (it <= 0) 3 else it }
        val organic = remote.getInt("organic_number_not_guide").let { if (it < 0) 0 else it }
        val isAdsCampaign = InstallReferrerHelper.isAdsCampaign(this)  // cache in-memory, không block

        val goToHomeStatus = if (isAdsCampaign) {
            n >= countAppOpen
        } else {
            n < organic || n >= countAppOpen + organic
        }

        if (goToHomeStatus) {
            return if (hasAllRuntimePermissions()) Screen.Home.route else Screen.Permission.route
        }
        return Screen.Intro.route
    }

    private fun hasAllRuntimePermissions(): Boolean {
        fun granted(p: String) = androidx.core.content.ContextCompat.checkSelfPermission(this, p) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        return granted(android.Manifest.permission.CAMERA) && granted(android.Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Full-screen: ẩn status bar + navigation bar hệ thống (vuốt để hiện tạm), áp dụng cho MỌI
     * route trong app (single-Activity — Camera/Take Photo/Gallery/Editor... đều dùng chung window
     * này). Gọi lại ở [onWindowFocusChanged] vì hệ thống có thể tự hiện lại bar sau khi mất focus
     * (mở dialog, bàn phím, chuyển app rồi quay lại...).
     */
    private fun enableImmersiveFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveFullScreen()
    }
}
