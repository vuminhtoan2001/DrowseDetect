package com.awakedrive.drowsedetect.detection

import kotlin.math.max

/** Quy các tín hiệu kỹ thuật về 1 con số dễ hiểu 0..1 (0 = tỉnh táo, 1 = rất mệt/nguy hiểm). */
fun fatigueLevel(r: DrowsinessResult, cfg: DetectionConfig): Float {
    val byPerclos = r.perclos / cfg.perclosDanger
    val byClosed = r.eyesClosedMs.toFloat() / cfg.microsleepMs
    val byYawn = r.yawnCount.toFloat() / cfg.yawnCountWarning * 0.6f
    val byNod = r.nodCount.toFloat() / cfg.nodCountWarning * 0.6f
    var v = max(max(byPerclos, byClosed), max(byYawn, byNod)).coerceIn(0f, 1f)
    when (r.state) {
        DrowsinessState.DANGER -> v = 1f
        DrowsinessState.WARNING -> v = max(v, 0.6f)
        else -> Unit
    }
    return v
}
