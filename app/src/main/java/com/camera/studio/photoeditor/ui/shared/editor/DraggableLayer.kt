package com.camera.studio.photoeditor.ui.shared.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import com.camera.studio.photoeditor.ui.shared.asset.AssetCatalog
import com.camera.studio.photoeditor.ui.theme.AppFontOptions
import kotlin.math.roundToInt

/**
 * Wrapper kéo-thả/resize/xoay (pinch) dùng chung cho mọi [EditorLayer] — tay cầm X (xoá) + copy
 * (nhân bản) khi đang chọn, khớp bounding-box quan sát ở `collage_edit_text.jpg`.
 *
 * Gộp chọn (tap) + kéo/pinch vào CHUNG 1 `pointerInput` (thay vì 2 `pointerInput` riêng biệt như
 * trước) — 2 gesture-detector độc lập cùng đọc 1 pointer event stream dễ giành `consume()` lẫn
 * nhau khiến kéo không hoạt động (tap chọn được nhưng pan bị nuốt). Đồng thời dùng
 * `rememberUpdatedState` cho [transform]/[onTransformChange]/[onTap] vì `pointerInput(Unit)` chỉ
 * khởi chạy coroutine 1 LẦN (không restart khi recompose) — nếu đọc thẳng tham số `transform` bị
 * "đóng băng" giá trị lúc mount đầu tiên, mọi delta kéo sau đó cộng dồn lên 1 gốc cũ sai lệch.
 */
@Composable
fun DraggableLayerHost(
    transform: LayerTransform,
    selected: Boolean,
    onTransformChange: (LayerTransform) -> Unit,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val latestTransform by rememberUpdatedState(transform)
    val latestOnTransformChange by rememberUpdatedState(onTransformChange)
    val latestOnTap by rememberUpdatedState(onTap)

    Box(
        modifier
            .offset { IntOffset(transform.offsetX.roundToInt(), transform.offsetY.roundToInt()) }
            .graphicsLayer(
                scaleX = transform.scale,
                scaleY = transform.scale,
                rotationZ = transform.rotationDeg,
                transformOrigin = TransformOrigin.Center,
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    latestOnTap()
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val rotationChange = event.calculateRotation()
                        val panChange = event.calculatePan()
                        if (zoomChange != 1f || rotationChange != 0f || panChange != Offset.Zero) {
                            val t = latestTransform
                            latestOnTransformChange(
                                t.copy(
                                    offsetX = t.offsetX + panChange.x,
                                    offsetY = t.offsetY + panChange.y,
                                    scale = (t.scale * zoomChange).coerceIn(0.3f, 4f),
                                    rotationDeg = t.rotationDeg + rotationChange,
                                ),
                            )
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            }
            .then(if (selected) Modifier.border(1.dp, Color(0xFFF5C518)) else Modifier),
    ) {
        content()
        if (selected) {
            Box(
                Modifier.align(Alignment.TopStart).offset((-10).dp, (-10).dp).size(20.dp)
                    .background(Color.White, CircleShape).border(1.dp, Color.Gray, CircleShape)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onDelete() }) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp)) }
            Box(
                Modifier.align(Alignment.BottomEnd).offset(10.dp, 10.dp).size(20.dp)
                    .background(Color.White, CircleShape).border(1.dp, Color.Gray, CircleShape)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onDuplicate() }) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(11.dp)) }
        }
    }
}

@Composable
fun TextLayerContent(layer: EditorLayer.TextLayer) {
    val font = AppFontOptions.getOrNull(layer.fontIndex)?.family
    androidx.compose.material3.Text(
        text = layer.text.ifBlank { " " },
        color = layer.color,
        fontSize = layer.fontSizeSp.sp,
        fontWeight = if (layer.bold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (layer.italic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (layer.underline) TextDecoration.Underline else TextDecoration.None,
        textAlign = layer.align,
        style = TextStyle(fontFamily = font),
    )
}

@Composable
fun StickerLayerContent(layer: EditorLayer.StickerLayer) {
    AsyncImage(
        model = AssetCatalog.assetUri(layer.assetPath),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(72.dp),
    )
}
