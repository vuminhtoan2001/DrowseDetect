package com.camera.studio.photoeditor.ui.camera.model

import android.graphics.ColorMatrix as AndroidColorMatrix

/** Nhân dồn nhiều ma trận màu (WB × Filter × AMP × Exposure) thành 1 ma trận áp 1 lần duy nhất. */
fun combineColorMatrices(vararg matrices: FloatArray?): FloatArray? {
    val nonNull = matrices.filterNotNull()
    if (nonNull.isEmpty()) return null
    val result = AndroidColorMatrix()
    for (m in nonNull) {
        result.postConcat(AndroidColorMatrix(m))
    }
    return result.array.copyOf()
}

/** AMP (Auto Amplify) — slider 0-100%, tăng sáng + bão hoà nhẹ, xấp xỉ "beauty light" tự động. */
fun ampMatrix(percent: Int): FloatArray? {
    if (percent <= 0) return null
    val brightness = percent * 0.6f
    val satBoost = 1f + percent / 400f
    val invSat = 1 - satBoost
    val lr = 0.2126f * invSat; val lg = 0.7152f * invSat; val lb = 0.0722f * invSat
    return floatArrayOf(
        lr + satBoost, lg, lb, 0f, brightness,
        lr, lg + satBoost, lb, 0f, brightness,
        lr, lg, lb + satBoost, 0f, brightness,
        0f, 0f, 0f, 1f, 0f,
    )
}

/** Exposure thủ công — slider -100%..100% (0 = giữ nguyên), chỉ tăng/giảm sáng tuyến tính. */
fun exposureMatrix(percent: Int): FloatArray? {
    if (percent == 0) return null
    val add = percent * 1.2f
    return floatArrayOf(
        1f, 0f, 0f, 0f, add,
        0f, 1f, 0f, 0f, add,
        0f, 0f, 1f, 0f, add,
        0f, 0f, 0f, 1f, 0f,
    )
}
