package com.awakedrive.drowsedetect.detection

import kotlin.math.abs

sealed interface CalibrationResult {
    data class Success(val baseline: Baseline) : CalibrationResult
    data class Failure(val reason: Reason) : CalibrationResult
    enum class Reason { NOT_ENOUGH_FACE, EYES_NOT_OPEN }
}

/** Thu thập earAvg / mar / pitch trong [DetectionConfig.calibrationDurationMs], baseline = median. */
class Calibrator(private val cfg: DetectionConfig = DetectionConfig()) {
    private var startTs = -1L
    private var total = 0
    private val ear = ArrayList<Float>()
    private val mar = ArrayList<Float>()
    private val pitch = ArrayList<Float>()

    fun add(f: FaceFeatures) {
        if (startTs < 0) startTs = f.timestampMs
        total++
        if (f.faceDetected && abs(f.yawDeg) <= cfg.maxYawDeg) {
            ear += f.earAvg; mar += f.mar; pitch += f.pitchDeg
        }
    }

    fun progress(nowMs: Long): Float =
        if (startTs < 0) 0f else ((nowMs - startTs).toFloat() / cfg.calibrationDurationMs).coerceIn(0f, 1f)

    fun isDone(nowMs: Long) = progress(nowMs) >= 1f

    fun result(): CalibrationResult {
        if (total == 0 || ear.size.toFloat() / total < cfg.calibrationMinFaceRatio) {
            return CalibrationResult.Failure(CalibrationResult.Reason.NOT_ENOUGH_FACE)
        }
        val baseEar = median(ear)
        if (baseEar < MIN_VALID_EAR) return CalibrationResult.Failure(CalibrationResult.Reason.EYES_NOT_OPEN)
        return CalibrationResult.Success(Baseline(baseEar, median(mar), median(pitch)))
    }

    private fun median(v: List<Float>): Float {
        val s = v.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2f
    }

    companion object {
        const val MIN_VALID_EAR = 0.15f
    }
}
