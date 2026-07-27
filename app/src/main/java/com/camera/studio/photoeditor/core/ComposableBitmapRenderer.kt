package com.camera.studio.photoeditor.core

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Render bất kỳ Composable tĩnh nào ra 1 [Bitmap] ĐỘC LẬP với màn hình, ở đúng độ phân giải mong
 * muốn — dùng để bake layer chỉnh sửa (frame/text/sticker/draw/collage-grid/wallpaper-canvas) vào
 * ảnh cuối khi Save/Export ở Edit Photo, Collage Maker, Wallpaper Creator.
 *
 * Cách làm: gắn tạm 1 [ComposeView] (bọc trong FrameLayout alpha=0) vào root window của [activity]
 * — nhờ vậy Composable có đủ ViewTree(Lifecycle/SavedState/ViewModelStore) owner để compose đúng —
 * đợi 2 frame để chắc chắn đã layout+draw xong, `draw()` ra Canvas, rồi gỡ view khỏi cây.
 *
 * @param density áp dụng cho Composable: nếu layer viết bằng dp, đổi density (khác density thật của
 *   máy) sẽ scale toàn bộ layout tỉ lệ đúng theo [widthPx]/[heightPx] mong muốn — đây là cách bảo đảm
 *   bake ra ảnh full-res vẫn giữ ĐÚNG tỉ lệ như lúc hiện ở preview.
 */
suspend fun renderComposableToBitmap(
    activity: Activity,
    widthPx: Int,
    heightPx: Int,
    density: Density,
    content: @Composable () -> Unit,
): Bitmap = suspendCancellableCoroutine { cont ->
    val root = activity.window.decorView as ViewGroup
    val composeView = ComposeView(activity).apply {
        setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                content()
            }
        }
    }
    val container = FrameLayout(activity).apply {
        alpha = 0f
        layoutParams = FrameLayout.LayoutParams(widthPx, heightPx)
    }
    container.addView(composeView, FrameLayout.LayoutParams(widthPx, heightPx))
    root.addView(container)

    fun capture() {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        composeView.draw(canvas)
        root.removeView(container)
        if (cont.isActive) cont.resume(bitmap)
    }

    // 2 frame để bảo đảm Compose đã compose+layout+draw xong (nội dung tĩnh, không ảnh network).
    container.post { container.post { capture() } }

    cont.invokeOnCancellation {
        root.post { runCatching { root.removeView(container) } }
    }
}

/**
 * Tính [Density] "ảo" để 1 layer viết bằng dp render ra đúng tỉ lệ khi kích thước đích (ảnh/video,
 * [targetWidthPx]) khác kích thước Box hiển thị preview ([previewWidthDp]).
 */
fun scaledDensityFor(targetWidthPx: Int, previewWidthDp: Float): Density =
    Density(density = targetWidthPx / previewWidthDp, fontScale = 1f)
