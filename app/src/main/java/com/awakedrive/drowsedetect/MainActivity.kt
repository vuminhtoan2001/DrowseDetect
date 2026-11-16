package com.awakedrive.drowsedetect

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.awakedrive.drowsedetect.platform.HomeReached
import com.awakedrive.drowsedetect.ui.AppRoot
import com.awakedrive.drowsedetect.ui.common.enableImmersiveFullScreen
import com.awakedrive.drowsedetect.ui.theme.AwakeDriveTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Home của app, trả qua HostApplication.getHomeActivity(). KHÔNG khai MAIN/LAUNCHER: launcher là
 * SplashActivity của SDK (Splash → Inter → Language → IAP → Home).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Đánh dấu đã vào Home (cờ "first run" cho tên màn First_* của màn lib, SCREEN_TRACKING.md §3.6)
        HomeReached.mark(this)
        enableImmersiveFullScreen()
        setContent {
            AwakeDriveTheme {
                AppRoot()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveFullScreen()
    }
}
