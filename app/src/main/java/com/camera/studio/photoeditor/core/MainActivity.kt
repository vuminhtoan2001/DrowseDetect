package com.camera.studio.photoeditor.core

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.camera.studio.photoeditor.ui.nav.AppNavHost
import com.camera.studio.photoeditor.ui.nav.Screen
import com.camera.studio.photoeditor.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity + launcher DUY NHẤT ở giai đoạn M0-M7 (chưa gắn base-application AAR — xem M8).
 * Ở M8: bỏ MAIN/LAUNCHER khỏi Manifest, lib mở Activity này qua `getHomeActivity()` sau luồng
 * Splash(lib)→check Intro→check Permission→Home; `resolveStartRoute()` bên dưới đã tách sẵn phần
 * app tự quyết định (Intro/Permission/Home) để ghép vào luồng đó mà không cần viết lại.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var userSettingsStore: UserSettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppStorage.language(this)?.let { LocaleHelper.updateLocale(this, it) }

        // Edge-to-edge: status/nav bar hệ thống VẪN HIỆN (giờ/sóng/wifi, home/back) nhưng TRONG SUỐT
        // — nền app/camera preview vẽ xuyên qua phía sau, icon hệ thống màu sáng cho hợp nền tối
        // (đã thử ẩn hẳn ở bản trước nhưng không ổn định trên máy thật → quay lại transparent).
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            var startRoute by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                startRoute = resolveStartRoute()
            }
            startRoute?.let { route ->
                AppTheme {
                    androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                        AppNavHost(startRoute = route)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /**
     * Quyết định start destination: chưa xem Intro → Intro; đã xem nhưng thiếu quyền cần thiết →
     * Permission; ngược lại → Home thẳng. Đơn giản hoá so với logic đếm-lần-mở-theo-nguồn-cài của
     * bản GPS gốc (ads-campaign/organic) — logic đó gắn liền Remote Config/InstallReferrer của
     * base-application, sẽ khôi phục đúng playbook khi gắn AAR ở M8 nếu cần.
     */
    private suspend fun resolveStartRoute(): String {
        val hasSeenIntro = userSettingsStore.settings.first().hasSeenIntro
        if (!hasSeenIntro) {
            lifecycleScope.launch { userSettingsStore.setHasSeenIntro(true) }
            return Screen.Intro.route
        }
        return if (hasAllRuntimePermissions()) Screen.Home.route else Screen.Permission.route
    }

    private fun hasAllRuntimePermissions(): Boolean {
        fun granted(p: String) = androidx.core.content.ContextCompat.checkSelfPermission(this, p) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        return granted(android.Manifest.permission.CAMERA) && granted(android.Manifest.permission.RECORD_AUDIO)
    }
}
