package com.awakedrive.drowsedetect.platform

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Tự log `screen_view` với tên màn cố định (docs/update-sdk-language/SCREEN_TRACKING.md §3.2).
 *
 * `screen_class` = chính tên màn (string literal): tên class thật thì mọi màn Compose đều là
 * `MainActivity`, và class của app có thể bị script encode đổi thành chuỗi ngẫu nhiên.
 *
 * Chặn log trùng liên tiếp: cùng tên với lần log trước thì bỏ qua. Quay về màn đang đứng từ bất
 * kỳ đâu (interstitial/app-open ad, IAP, recreate(), share chooser, CH Play, Settings hệ thống,
 * bấm Home rồi mở lại) đều KHÔNG log lại. Ngoại lệ: app ở background lâu hơn
 * [RELOG_AFTER_BACKGROUND_MS] (= timeout phiên GA4 mặc định) thì log lại cho phiên mới.
 */
object ScreenTracker {
    private const val TAG = "ScreenTracker"
    private const val RELOG_AFTER_BACKGROUND_MS = 30 * 60 * 1000L

    @Volatile
    private var lastScreen: String? = null
    private var backgroundAt = 0L

    /** Gọi 1 lần trong Application.onCreate (main thread). */
    fun init() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                backgroundAt = SystemClock.elapsedRealtime()
            }

            override fun onStart(owner: LifecycleOwner) {
                if (backgroundAt != 0L && SystemClock.elapsedRealtime() - backgroundAt > RELOG_AFTER_BACKGROUND_MS) {
                    lastScreen = null
                }
                backgroundAt = 0L
            }
        })
    }

    fun log(context: Context, screenName: String, params: Map<String, Any?> = emptyMap()) {
        if (screenName == lastScreen) {
            Log.d(TAG, "screen_view: $screenName (bỏ qua, trùng lần trước)")
            return
        }
        lastScreen = screenName
        Log.d(TAG, "screen_view: $screenName ${if (params.isEmpty()) "" else params}")
        val bundle = params.toBundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
}

/**
 * Event nghiệp vụ (snake_case, không prefix). Gọi thẳng trong callback, KHÔNG phụ thuộc
 * lifecycle — vẫn gửi được khi app ở background.
 */
object AppEvents {
    private const val TAG = "AppEvents"

    fun log(context: Context, name: String, vararg params: Pair<String, Any?>) {
        val map = params.toMap()
        Log.d(TAG, "$name $map")
        FirebaseAnalytics.getInstance(context).logEvent(name, map.toBundle())
    }
}

/** String / Boolean ("true"/"false") / số nguyên / số thực; null bị bỏ qua. */
private fun Map<String, Any?>.toBundle(): Bundle = bundleOf().also { b ->
    forEach { (k, v) ->
        when (v) {
            null -> Unit
            is String -> b.putString(k, v)
            is Boolean -> b.putString(k, v.toString())
            is Int -> b.putLong(k, v.toLong())
            is Long -> b.putLong(k, v)
            is Number -> b.putDouble(k, v.toDouble())
            else -> b.putString(k, v.toString())
        }
    }
}

/**
 * Log mỗi lần lifecycle owner hiện tại (NavBackStackEntry / Activity) vào RESUMED, và khi
 * [screenName] đổi. Không dùng LaunchedEffect: sẽ không log khi back về màn cũ.
 * [params] đi kèm screen_view — đổi params không tự log lại.
 */
@Composable
fun TrackScreen(screenName: String, params: Map<String, Any?> = emptyMap()) {
    val context = LocalContext.current
    LifecycleResumeEffect(screenName) {
        ScreenTracker.log(context, screenName, params)
        onPauseOrDispose { }
    }
}
