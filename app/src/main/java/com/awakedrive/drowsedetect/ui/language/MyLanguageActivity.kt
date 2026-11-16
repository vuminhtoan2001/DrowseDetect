package com.awakedrive.drowsedetect.ui.language

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.brian.base_application.language.LanguageRouter
import com.awakedrive.drowsedetect.MainActivity
import com.awakedrive.drowsedetect.platform.AppEvents
import com.awakedrive.drowsedetect.ui.common.enableImmersiveFullScreen
import com.awakedrive.drowsedetect.ui.theme.AwakeDriveTheme

/**
 * Màn Language custom của app (docs/update-sdk-language/LANGUAGE_SCREEN_CUSTOM.md) — cầu nối giữa
 * [LanguageRouter] và [LanguageScreen]. Vào theo 2 luồng, phân biệt bằng [EXTRA_FROM_SETTINGS]:
 * - first-run (`false`): SDK tự `startActivity` từ Splash (đăng ký ở HostApplication qua
 *   `LanguageRouter.customActivityClass`). Confirm xong gọi `confirmLanguageSelection(this, code)`
 *   (navigate mặc định true) — SDK tự mở màn kế (IAP, đã gắn sẵn vào intent) + tự `finish()` màn
 *   này, app KHÔNG được gọi `finish()` thêm (§4.1).
 * - mở từ Settings (`true`): app tự mở tay. Confirm xong gọi
 *   `confirmLanguageSelection(this, code, navigate = false)` rồi app tự `finish()` (§4.2).
 */
class MyLanguageActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FROM_SETTINGS = "extra_from_settings"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveFullScreen(lightBars = true)

        val isFromSettings = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)

        // First-run: disable back hẳn (tránh thoát mà chưa confirm → lặp lại màn Language, §8.1).
        // Từ Settings: callback đã chặn default → phải tự finish(), nếu không user bị kẹt.
        onBackPressedDispatcher.addCallback(this) {
            if (isFromSettings) finish()
        }

        setContent {
            AwakeDriveTheme {
                LanguageScreen(
                    fromSettings = isFromSettings,
                    onDone = { code, method ->
                        AppEvents.log(
                            this, "language_confirm",
                            "code" to code, "method" to method, "from_settings" to isFromSettings,
                        )
                        if (isFromSettings) {
                            LanguageRouter.confirmLanguageSelection(this, code, navigate = false)
                            finish()
                        } else {
                            LanguageRouter.confirmLanguageSelection(this, code, MainActivity::class.java)
                        }
                    },
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveFullScreen(lightBars = true)
    }
}
