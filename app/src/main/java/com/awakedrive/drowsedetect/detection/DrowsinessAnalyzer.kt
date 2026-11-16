package com.awakedrive.drowsedetect.detection

import kotlin.math.abs
import kotlin.math.min

/**
 * Pure Kotlin: nhận [FaceFeatures] (timestamp do bên ngoài đưa vào), trả [DrowsinessResult].
 * Không thread-safe: gọi từ 1 thread duy nhất.
 */
class DrowsinessAnalyzer(
    private val cfg: DetectionConfig = DetectionConfig(),
    private val baseline: Baseline = Baseline.DEFAULT,
) {
    private class Sample(val ts: Long, val durMs: Long, val closed: Boolean)

    private var state = DrowsinessState.NORMAL
    private var reason: AlertReason? = null
    private var lowerSince = -1L

    private var prevTs = -1L
    private var lastFaceTs = -1L

    private var closedStart = -1L
    private val samples = ArrayDeque<Sample>()
    private var windowTotalMs = 0L
    private var windowClosedMs = 0L

    private var yawnStart = -1L
    private var yawnCounted = false
    private val yawns = ArrayDeque<Long>()

    private var devStart = -1L
    private val nods = ArrayDeque<Long>()

    fun process(f: FaceFeatures): DrowsinessResult {
        val ts = f.timestampMs
        val dt = if (prevTs < 0) 0L else min(maxOf(ts - prevTs, 0L), cfg.maxFrameGapMs)
        if (lastFaceTs < 0) lastFaceTs = ts
        prevTs = ts
        evict(ts)

        if (!f.faceDetected) {
            closedStart = -1L
            yawnStart = -1L; yawnCounted = false
            devStart = -1L
            if (ts - lastFaceTs >= cfg.noFaceTimeoutMs) {
                state = DrowsinessState.NO_FACE
                reason = null
                lowerSince = -1L
            }
            return result(0L, false)
        }
        lastFaceTs = ts

        // --- Mắt nhắm (frame không đáng tin khi quay đầu quá lớn: không tính vào PERCLOS) ---
        val reliable = abs(f.yawDeg) <= cfg.maxYawDeg
        val blinkAvg = (f.blinkLeft + f.blinkRight) / 2f
        val closed = reliable && (f.earAvg < baseline.ear * cfg.eyeClosedRatio || blinkAvg > cfg.blinkScoreThreshold)
        if (reliable) {
            if (dt > 0) {
                samples.addLast(Sample(ts, dt, closed))
                windowTotalMs += dt
                if (closed) windowClosedMs += dt
            }
            if (closed) {
                if (closedStart < 0) closedStart = ts
            } else closedStart = -1L
        } else closedStart = -1L
        val closedMs = if (closedStart >= 0) ts - closedStart else 0L

        // --- Ngáp ---
        val mouthOpen = f.jawOpen > cfg.yawnJawOpen ||
            (baseline.mar > 0.001f && f.mar > baseline.mar * cfg.yawnMarRatio)
        if (mouthOpen) {
            if (yawnStart < 0) yawnStart = ts
            if (!yawnCounted && ts - yawnStart >= cfg.yawnMinMs) {
                yawns.addLast(ts); yawnCounted = true
            }
        } else {
            yawnStart = -1L; yawnCounted = false
        }

        // --- Gật đầu (độ lệch pitch so với baseline, không phụ thuộc chiều dấu của ma trận) ---
        val deviated = abs(f.pitchDeg - baseline.pitchDeg) > cfg.nodPitchDeg
        if (deviated) {
            if (devStart < 0) devStart = ts
        } else if (devStart >= 0) {
            if (ts - devStart <= cfg.nodMaxMs) nods.addLast(ts)
            devStart = -1L
        }
        val headDownLong = devStart >= 0 && ts - devStart > cfg.nodMaxMs

        // --- Trạng thái tức thời ---
        val perclos = perclos()
        var target = DrowsinessState.NORMAL
        var targetReason: AlertReason? = null
        fun raise(s: DrowsinessState, r: AlertReason) {
            if (s.level > target.level) { target = s; targetReason = r }
        }
        if (perclos >= cfg.perclosWarning) raise(DrowsinessState.WARNING, AlertReason.PERCLOS)
        if (yawns.size >= cfg.yawnCountWarning) raise(DrowsinessState.WARNING, AlertReason.YAWN)
        if (nods.size >= cfg.nodCountWarning) raise(DrowsinessState.WARNING, AlertReason.HEAD_NOD)
        if (perclos >= cfg.perclosDanger) raise(DrowsinessState.DANGER, AlertReason.PERCLOS)
        if (headDownLong && closedMs >= cfg.headDownClosedMs) raise(DrowsinessState.DANGER, AlertReason.HEAD_NOD)
        if (closedMs >= cfg.microsleepMs) raise(DrowsinessState.DANGER, AlertReason.MICROSLEEP)

        // --- Hysteresis: nâng cấp ngay, hạ cấp sau downgradeHoldMs ---
        if (target.level >= state.level) {
            state = target; reason = targetReason; lowerSince = -1L
        } else {
            if (lowerSince < 0) lowerSince = ts
            if (ts - lowerSince >= cfg.downgradeHoldMs) {
                state = target; reason = targetReason; lowerSince = -1L
            }
        }
        return result(closedMs, closed)
    }

    private fun result(closedMs: Long, closed: Boolean) = DrowsinessResult(
        state = state,
        reason = if (state == DrowsinessState.NORMAL || state == DrowsinessState.NO_FACE) null else reason,
        perclos = perclos(),
        eyesClosedMs = closedMs,
        yawnCount = yawns.size,
        nodCount = nods.size,
        eyesClosed = closed,
    )

    /** PERCLOS theo thời lượng; chỉ đánh giá khi cửa sổ có ≥ perclosMinWindowMs dữ liệu. */
    private fun perclos(): Float =
        if (windowTotalMs >= cfg.perclosMinWindowMs) windowClosedMs.toFloat() / windowTotalMs else 0f

    private fun evict(now: Long) {
        while (samples.isNotEmpty() && samples.first().ts < now - cfg.perclosWindowMs) {
            val s = samples.removeFirst()
            windowTotalMs -= s.durMs
            if (s.closed) windowClosedMs -= s.durMs
        }
        while (yawns.isNotEmpty() && yawns.first() < now - cfg.yawnWindowMs) yawns.removeFirst()
        while (nods.isNotEmpty() && nods.first() < now - cfg.nodWindowMs) nods.removeFirst()
    }
}
