package com.camera.studio.photoeditor.ui.shared.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.camera.studio.photoeditor.core.BitmapUtils

/**
 * Ghép N bitmap (đã chụp/chọn) thành 1 ảnh collage theo [CollageLayout] — dùng chung cho Camera
 * "chụp trực tiếp thành collage" (M2) và Collage Maker (M5, ảnh chọn từ Gallery thay vì chụp mới).
 */
object CollageCompositor {

    fun composite(
        layout: CollageLayout,
        panes: List<Bitmap>,
        canvasWidth: Int,
        canvasHeight: Int,
        gapPx: Int = 6,
        backgroundColor: Int = Color.BLACK,
    ): Bitmap {
        val result = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(backgroundColor)
        val rects = layout.panes(canvasWidth, canvasHeight, gapPx)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        rects.forEachIndexed { i, rect ->
            val src = panes.getOrNull(i) ?: return@forEachIndexed
            val cropped = BitmapUtils.centerCropToAspect(
                src,
                rect.width().toFloat(),
                rect.height().toFloat(),
            )
            val dst = android.graphics.Rect(rect.left, rect.top, rect.right, rect.bottom)
            canvas.drawBitmap(cropped, null, dst, paint)
        }
        return result
    }
}
