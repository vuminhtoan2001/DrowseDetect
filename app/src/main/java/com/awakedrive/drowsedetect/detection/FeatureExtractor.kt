package com.awakedrive.drowsedetect.detection

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.hypot

/** Pure Kotlin: tính EAR/MAR/head pose từ landmark chuẩn hoá (0..1) của MediaPipe. */
object FeatureExtractor {
    val LEFT_EYE = intArrayOf(33, 160, 158, 133, 153, 144)
    val RIGHT_EYE = intArrayOf(362, 385, 387, 263, 373, 380)
    private const val LIP_UPPER = 13
    private const val LIP_LOWER = 14
    private const val MOUTH_LEFT = 78
    private const val MOUTH_RIGHT = 308
    private const val NOSE = 1
    private const val FOREHEAD = 10
    private const val CHIN = 152

    /** [xs]/[ys]: tọa độ chuẩn hoá của ≥ 468 landmark. [matrix]: 16 float (4x4) nếu có. */
    fun extract(
        timestampMs: Long,
        xs: FloatArray,
        ys: FloatArray,
        width: Int,
        height: Int,
        blendshapes: Map<String, Float>,
        matrix: FloatArray?,
    ): FaceFeatures {
        val w = width.toFloat()
        val h = height.toFloat()
        fun d(a: Int, b: Int) = hypot((xs[a] - xs[b]) * w, (ys[a] - ys[b]) * h)

        fun ear(p: IntArray) = (d(p[1], p[5]) + d(p[2], p[4])) / (2f * d(p[0], p[3]).coerceAtLeast(1e-3f))

        val earL = ear(LEFT_EYE)
        val earR = ear(RIGHT_EYE)
        val mar = d(LIP_UPPER, LIP_LOWER) / d(MOUTH_LEFT, MOUTH_RIGHT).coerceAtLeast(1e-3f)

        val pose = if (matrix != null && matrix.size >= 16) {
            eulerFromMatrix(matrix)
        } else {
            // Xấp xỉ pitch từ vị trí mũi giữa trán và cằm (không có yaw/roll)
            val span = (ys[CHIN] - ys[FOREHEAD]).coerceAtLeast(1e-3f)
            val r = (ys[NOSE] - ys[FOREHEAD]) / span
            floatArrayOf(((r - 0.55f) * 120f), 0f, 0f)
        }
        return FaceFeatures(
            timestampMs = timestampMs,
            faceDetected = true,
            earLeft = earL,
            earRight = earR,
            earAvg = (earL + earR) / 2f,
            blinkLeft = blendshapes["eyeBlinkLeft"] ?: 0f,
            blinkRight = blendshapes["eyeBlinkRight"] ?: 0f,
            mar = mar,
            jawOpen = blendshapes["jawOpen"] ?: 0f,
            pitchDeg = pose[0],
            yawDeg = pose[1],
            rollDeg = pose[2],
        )
    }

    /**
     * Ma trận 4x4 (column-major) -> [pitch, yaw, roll] độ. Dấu có thể đảo nếu layout/hệ trục khác,
     * nhưng analyzer chỉ dùng |yaw| và |pitch - baseline| nên không ảnh hưởng. Góc gần ±180° (hệ trục
     * khuôn mặt quay ngược) được gập về [-90, 90].
     */
    fun eulerFromMatrix(m: FloatArray): FloatArray {
        fun r(row: Int, col: Int) = m[col * 4 + row]
        val toDeg = (180.0 / PI).toFloat()
        fun fold(a: Float) = if (a > 90f) a - 180f else if (a < -90f) a + 180f else a
        val yaw = asin((-r(2, 0)).coerceIn(-1f, 1f)) * toDeg
        val pitch = fold(atan2(r(2, 1), r(2, 2)) * toDeg)
        val roll = fold(atan2(r(1, 0), r(0, 0)) * toDeg)
        return floatArrayOf(pitch, yaw, roll)
    }
}
