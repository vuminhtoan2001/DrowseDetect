package com.camera.studio.photoeditor.ui.shared.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize

/** Vị trí/kích thước/xoay của 1 layer trên canvas — dùng chung Text & Sticker. */
data class LayerTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
)

/** Text/Sticker object kéo-thả trên canvas (Edit Photo/Collage Maker/Wallpaper Creator). */
sealed class EditorLayer {
    abstract val id: Long
    abstract val transform: LayerTransform

    data class TextLayer(
        override val id: Long,
        override val transform: LayerTransform = LayerTransform(),
        val text: String = "",
        val fontIndex: Int = 0,
        val fontSizeSp: Float = 28f,
        val color: Color = Color.White,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val align: TextAlign = TextAlign.Center,
        /** -100..100, bẻ cong baseline chữ — chỉ Collage Maker dùng (docs/07 §Text). */
        val arcPercent: Int = 0,
    ) : EditorLayer()

    data class StickerLayer(
        override val id: Long,
        override val transform: LayerTransform = LayerTransform(),
        val assetPath: String,
    ) : EditorLayer()
}

fun EditorLayer.withTransform(t: LayerTransform): EditorLayer = when (this) {
    is EditorLayer.TextLayer -> copy(transform = t)
    is EditorLayer.StickerLayer -> copy(transform = t)
}

/**
 * Vị trí mặc định khi thêm layer mới: giữa canvas (không phải góc trên-trái) — [DraggableLayerHost]
 * định vị theo TOP-LEFT của content nên cần trừ đi khoảng nửa kích thước ước lượng của content.
 */
fun centerTransform(canvasSizePx: IntSize, halfContentPx: Float = 50f): LayerTransform =
    LayerTransform(
        offsetX = canvasSizePx.width / 2f - halfContentPx,
        offsetY = canvasSizePx.height / 2f - halfContentPx,
    )

/** 1 nét vẽ tay (Draw tool) — danh sách điểm theo toạ độ canvas gốc (chưa scale theo màn hình). */
data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float,
    val alpha: Float = 1f,
)
