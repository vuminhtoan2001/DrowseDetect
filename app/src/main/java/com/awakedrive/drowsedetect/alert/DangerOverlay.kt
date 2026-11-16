package com.awakedrive.drowsedetect.alert

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.awakedrive.drowsedetect.detection.AlertReason
import com.awakedrive.drowsedetect.ui.alert.DangerAlertScreen
import com.awakedrive.drowsedetect.ui.theme.AwakeDriveTheme

/**
 * Cửa sổ cảnh báo NGUY HIỂM vẽ trên mọi app (quyền "Hiển thị trên ứng dụng khác"). Chạy từ Service nên
 * tự làm chủ Lifecycle/SavedState cho Compose. Mọi hàm gọi trên main thread.
 */
class DangerOverlay(private val context: Context) : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var view: ComposeView? = null

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    init {
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    val isShowing: Boolean get() = view != null

    fun show(reason: AlertReason?, onAwake: () -> Unit, onFindRest: () -> Unit) {
        if (view != null) return
        val v = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@DangerOverlay)
            setViewTreeSavedStateRegistryOwner(this@DangerOverlay)
            setContent { AwakeDriveTheme { DangerAlertScreen(reason, onAwake, onFindRest) } }
        }
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        try {
            windowManager.addView(v, params)
            view = v
            registry.currentState = Lifecycle.State.RESUMED
        } catch (e: Exception) {
            // Thiếu quyền overlay hoặc bị hệ thống chặn: bỏ qua, âm thanh/rung vẫn chạy
            v.disposeComposition()
        }
    }

    fun hide() {
        val v = view ?: return
        view = null
        registry.currentState = Lifecycle.State.CREATED
        try { windowManager.removeView(v) } catch (_: Exception) { }
        v.disposeComposition()
    }
}
