package com.awakedrive.drowsedetect.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * ImageProxy (RGBA_8888) -> Bitmap đã xoay đúng chiều -> [FaceLandmarkerHelper]. Giới hạn fps theo
 * [minIntervalMs]. Không lật gương: không có preview nên không cần, và EAR/MAR/blendshape đối xứng.
 */
class FrameAnalyzer(private val helper: FaceLandmarkerHelper) : ImageAnalysis.Analyzer {
    @Volatile var minIntervalMs: Long = 66L
    private var lastTs = 0L

    override fun analyze(image: ImageProxy) {
        try {
            val now = SystemClock.uptimeMillis()
            if (now - lastTs < minIntervalMs) return
            lastTs = now
            val raw = image.toBitmap()
            val rot = image.imageInfo.rotationDegrees
            val bitmap = if (rot == 0) raw else {
                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
                    .also { raw.recycle() }
            }
            helper.detectAsync(bitmap)
        } finally {
            image.close()
        }
    }
}
