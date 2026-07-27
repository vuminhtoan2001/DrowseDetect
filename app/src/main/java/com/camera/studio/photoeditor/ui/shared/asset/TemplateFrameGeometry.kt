package com.camera.studio.photoeditor.ui.shared.asset

import androidx.compose.ui.geometry.Offset
import kotlin.math.max

/**
 * Quy đổi polygon 4 điểm của [TemplateFrame] (toạ độ PIXEL GỐC theo ảnh template, ví dụ 1572×3408)
 * sang toạ độ canvas đang hiển thị — dùng ĐÚNG công thức cover-fit (scale = max theo 2 chiều, canh
 * giữa) mà `ContentScale.Crop` đang áp cho background.webp/frame.webp. Nếu công thức lệch với cách
 * Coil vẽ 2 ảnh nền/frame kia, polygon sẽ không còn khớp với khung ảnh thật trong artwork.
 */
fun TemplateFrame.toCanvasPoints(imageWidth: Float, imageHeight: Float, canvasWidth: Float, canvasHeight: Float): List<Offset> {
    if (imageWidth <= 0f || imageHeight <= 0f || canvasWidth <= 0f || canvasHeight <= 0f) return points
    val scale = max(canvasWidth / imageWidth, canvasHeight / imageHeight)
    val offsetX = (canvasWidth - imageWidth * scale) / 2f
    val offsetY = (canvasHeight - imageHeight * scale) / 2f
    return points.map { Offset(offsetX + it.x * scale, offsetY + it.y * scale) }
}

/** Ray-casting point-in-polygon — test tap có rơi đúng vào khung hay không, chính xác cho cả khung
 * nghiêng/lệch phối cảnh chứ không chỉ bounding-box chữ nhật. */
fun List<Offset>.containsPoint(point: Offset): Boolean {
    var inside = false
    var j = size - 1
    for (i in indices) {
        val pi = this[i]
        val pj = this[j]
        if ((pi.y > point.y) != (pj.y > point.y)) {
            val xIntersect = (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x
            if (point.x < xIntersect) inside = !inside
        }
        j = i
    }
    return inside
}

fun List<Offset>.centroid(): Offset {
    var sx = 0f
    var sy = 0f
    forEach { sx += it.x; sy += it.y }
    return Offset(sx / size, sy / size)
}
