package com.camera.studio.photoeditor.core

import android.content.Intent
import android.os.Build
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

    private var appliedLanguageCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appliedLanguageCode = AppStorage.language(this)
        appliedLanguageCode?.let { LocaleHelper.updateLocale(this, it) }

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

    /**
     * MainActivity khai `configChanges="locale|layoutDirection"` (Manifest) để lib đổi ngôn ngữ
     * KHÔNG làm app restart từ Splash. Nhưng vì vậy hệ thống sẽ KHÔNG tự recreate Activity khi ngôn
     * ngữ đổi — trên Android 13+ (`LocaleManager.applicationLocales`, xem LocaleHelper) hệ thống chỉ
     * gọi onConfigurationChanged() (không override) nên Compose vẫn hiển thị chuỗi theo locale CŨ.
     * Tự phát hiện khi quay lại từ màn Language của lib và ép recreate() thủ công (recreate() luôn
     * destroy+tạo lại Activity, không bị chặn bởi configChanges) để đọc lại string resource đúng
     * locale mới.
     */
    override fun onResume() {
        super.onResume()
        val currentLanguageCode = AppStorage.language(this)
        if (currentLanguageCode != appliedLanguageCode) {
            appliedLanguageCode = currentLanguageCode
            recreate()
        }
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
     * Edge-to-edge KHÔNG ĐỐI XỨNG giữa 2 bar — đây là điểm mấu chốt để vừa hết đen ở header vừa
     * ẩn được bar dưới (2 yêu cầu tưởng như xung khắc):
     *
     * - Status bar (top): SHOW thường trực + trong suốt, KHÔNG BAO GIỜ hide. Lớp scrim đen cưỡng
     *   bức (xem giải thích cũ bên dưới) chỉ xảy ra khi bar đang ở trạng thái "ẩn rồi hiện tạm"
     *   (transient reveal) — do camera privacy-indicator (Android 12+)/dialog hệ thống tự kích hoạt.
     *   Show thường trực thì KHÔNG BAO GIỜ rơi vào trạng thái đó → luôn trong suốt, app vẽ xuyên lên
     *   được đúng như mong muốn.
     * - Navigation bar (bottom): HIDE hẳn (vuốt để hiện tạm) — bar này không có nguồn nào tự ép hiện
     *   tạm như status bar, nên hide bình thường không bị lỗi scrim. Ẩn hẳn để nút "Install"/"Cài
     *   đặt" của native ad ở đáy màn không bị system nav bar đè lên (bug đã gặp khi show cả 2 bar).
     *
     * Gọi lại ở [onWindowFocusChanged] vì 1 số OEM/dialog tự reset lại sau khi mất-rồi-lấy-lại focus.
     */
    private fun enableImmersiveFullScreen() {
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
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveFullScreen()
    }
}
