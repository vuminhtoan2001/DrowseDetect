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
     * Edge-to-edge: status bar + navigation bar hệ thống LUÔN hiển thị nhưng trong suốt, để nội
     * dung app (Column `.statusBarsPadding()`/`.navigationBarsPadding()` ở từng Screen, xem
     * HomeScreen/CameraScreen/Common.kt) tự chừa khoảng rồi vẽ nền xuyên lên vùng đó — áp dụng cho
     * MỌI route (single-Activity). Gọi lại ở [onWindowFocusChanged] vì 1 số OEM/dialog tự reset màu
     * bar khi mất-rồi-lấy-lại focus.
     *
     * KHÔNG dùng `controller.hide(systemBars())`: trước đây app ẩn hẳn bar rồi cho vuốt hiện tạm
     * (transient), nhưng Android LUÔN vẽ 1 lớp scrim đen mờ CƯỠNG BỨC phía sau bar khi nó đang ở
     * trạng thái "transient reveal" — cờ `isStatusBarContrastEnforced=false`/màu TRANSPARENT hoàn
     * toàn KHÔNG có tác dụng trong trường hợp này (chỉ có tác dụng khi bar hiển thị THƯỜNG TRỰC,
     * không phải bị ẩn rồi hiện tạm). Camera privacy-indicator (chấm xanh khi dùng Camera, Android
     * 12+) và các dialog hệ thống (vd AdMob native-ad validator) đều tự ép bar hiện tạm — nên trước
     * đây phần header ở màn Take Photo hay bị 1 khối đen tách biệt dù đã set trong suốt. Để bar hiển
     * thị thường trực (không hide) thì cờ trong suốt mới thực sự ăn, nền app mới lộ ra đúng như mong
     * muốn.
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
        controller.show(WindowInsetsCompat.Type.systemBars())
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveFullScreen()
    }
}
