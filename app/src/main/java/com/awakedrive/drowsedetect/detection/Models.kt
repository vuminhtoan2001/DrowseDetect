package com.awakedrive.drowsedetect.detection

/** Mọi ngưỡng nằm ở đây (pure Kotlin, test được). Giá trị mặc định theo docs/DROWSINESS_APP_SPEC.md §7.1. */
data class DetectionConfig(
    val calibrationDurationMs: Long = 15_000L,
    val calibrationMinFaceRatio: Float = 0.6f,
    val eyeClosedRatio: Float = 0.7f,
    val blinkScoreThreshold: Float = 0.5f,
    val microsleepMs: Long = 1_200L,
    val perclosWindowMs: Long = 60_000L,
    val perclosMinWindowMs: Long = 30_000L,
    val perclosWarning: Float = 0.15f,
    val perclosDanger: Float = 0.25f,
    val yawnJawOpen: Float = 0.6f,
    val yawnMarRatio: Float = 2.5f,
    val yawnMinMs: Long = 2_000L,
    val yawnWindowMs: Long = 300_000L,
    val yawnCountWarning: Int = 3,
    val nodPitchDeg: Float = 15f,
    val nodMaxMs: Long = 1_500L,
    val nodWindowMs: Long = 60_000L,
    val nodCountWarning: Int = 2,
    /** Đầu cúi kéo dài + mắt nhắm ≥ ngần này ms -> DANGER. */
    val headDownClosedMs: Long = 500L,
    val noFaceTimeoutMs: Long = 3_000L,
    val maxYawDeg: Float = 35f,
    val downgradeHoldMs: Long = 5_000L,
    /** Khoảng hở giữa 2 frame lớn hơn mức này bị cắt (tránh 1 frame trễ làm lệch PERCLOS). */
    val maxFrameGapMs: Long = 500L,
) {
    companion object
}

/** Đặc trưng của 1 frame (spec §6). */
data class FaceFeatures(
    val timestampMs: Long,
    val faceDetected: Boolean,
    val earLeft: Float = 0f,
    val earRight: Float = 0f,
    val earAvg: Float = 0f,
    val blinkLeft: Float = 0f,
    val blinkRight: Float = 0f,
    val mar: Float = 0f,
    val jawOpen: Float = 0f,
    val pitchDeg: Float = 0f,
    val yawDeg: Float = 0f,
    val rollDeg: Float = 0f,
) {
    companion object {
        fun noFace(timestampMs: Long) = FaceFeatures(timestampMs, faceDetected = false)
    }
}

/** Baseline theo người dùng. mar = 0 nghĩa là chưa có -> chỉ dùng jawOpen để phát hiện ngáp. */
data class Baseline(val ear: Float, val mar: Float, val pitchDeg: Float) {
    companion object {
        val DEFAULT = Baseline(ear = 0.28f, mar = 0f, pitchDeg = 0f)
    }
}

enum class DrowsinessState(val level: Int) {
    NO_FACE(-1), NORMAL(0), WARNING(1), DANGER(2)
}

enum class AlertReason { MICROSLEEP, PERCLOS, YAWN, HEAD_NOD }

data class DrowsinessResult(
    val state: DrowsinessState,
    val reason: AlertReason?,
    val perclos: Float,
    val eyesClosedMs: Long,
    val yawnCount: Int,
    val nodCount: Int,
    /** Frame hiện tại bị coi là mắt nhắm (đã loại frame quay đầu). */
    val eyesClosed: Boolean = false,
)

/** Độ nhạy người dùng chọn trong Cài đặt; ánh xạ sang bộ ngưỡng [DetectionConfig]. */
enum class Sensitivity { LOW, MEDIUM, HIGH }

fun DetectionConfig.Companion.forSensitivity(s: Sensitivity): DetectionConfig = when (s) {
    Sensitivity.LOW -> DetectionConfig(
        eyeClosedRatio = 0.65f, microsleepMs = 1_600L, perclosWarning = 0.18f, perclosDanger = 0.30f,
    )
    Sensitivity.MEDIUM -> DetectionConfig()
    Sensitivity.HIGH -> DetectionConfig(
        eyeClosedRatio = 0.75f, microsleepMs = 1_000L, perclosWarning = 0.12f, perclosDanger = 0.20f,
    )
}
