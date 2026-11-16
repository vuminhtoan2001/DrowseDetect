package com.awakedrive.drowsedetect.platform

import android.content.Context
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Log `onboarding_exit` khi user bấm Back thoát ở màn onboarding, KHÔNG đổi hành vi thoát app
 * (docs/update-sdk-language/SCREEN_TRACKING.md §3.3).
 *
 * Không dùng BackHandler: muốn chuyển tiếp Back phải tắt callback của chính mình NGAY rồi mới gọi
 * lại dispatcher; BackHandler chỉ cập nhật isEnabled ở lần compose sau → lặp vô hạn.
 */
@Composable
fun LogOnboardingExit(screen: String) {
    val context = LocalContext.current
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentScreen = rememberUpdatedState(screen)
    DisposableEffect(dispatcher, lifecycleOwner) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AppEvents.log(context, "onboarding_exit", "screen" to currentScreen.value)
                isEnabled = false
                dispatcher.onBackPressed()
                isEnabled = true
            }
        }
        dispatcher.addCallback(lifecycleOwner, callback)
        onDispose { callback.remove() }
    }
}

/**
 * Cờ "đã từng vào Home" — phân biệt luồng first-open với các lần sau cho tên màn `VD_First_*`
 * (màn của lib: NotiPermission / IAP). App này không có cờ onboarding một-lần (gate Intro theo số
 * lần mở, xem `AppViewModel`), nên đánh dấu ngay khi MainActivity được tạo lần đầu.
 */
object HomeReached {
    private const val KEY = "home_reached"

    private fun prefs(context: Context) =
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

    fun isReached(context: Context): Boolean = prefs(context).getBoolean(KEY, false)

    fun mark(context: Context) {
        if (!isReached(context)) prefs(context).edit().putBoolean(KEY, true).apply()
    }
}
