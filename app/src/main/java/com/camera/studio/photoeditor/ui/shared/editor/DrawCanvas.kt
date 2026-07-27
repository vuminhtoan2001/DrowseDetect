package com.camera.studio.photoeditor.ui.shared.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Canvas vẽ tay tự do dùng chung Edit Photo/Collage Maker/Wallpaper Creator (docs/07 §Draw).
 * [enabled]=false khi tool Draw không mở → chỉ hiển thị nét đã vẽ, không bắt gesture (để không
 * chặn gesture kéo-thả Text/Sticker layer bên dưới).
 */
@Composable
fun DrawCanvas(
    strokes: List<DrawStroke>,
    currentColor: androidx.compose.ui.graphics.Color,
    currentWidthPx: Float,
    currentAlpha: Float,
    enabled: Boolean,
    onStrokeFinished: (DrawStroke) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activePoints by remember { mutableStateOf(listOf<Offset>()) }

    Canvas(
        modifier.fillMaxSize().let {
            if (!enabled) it else it.pointerInput(currentColor, currentWidthPx, currentAlpha) {
                detectDragPath(
                    onStart = { activePoints = listOf(it) },
                    onDrag = { activePoints = activePoints + it },
                    onEnd = {
                        if (activePoints.size > 1) {
                            onStrokeFinished(DrawStroke(activePoints, currentColor, currentWidthPx, currentAlpha))
                        }
                        activePoints = emptyList()
                    },
                )
            }
        },
    ) {
        (strokes + listOfNotNull(
            if (activePoints.size > 1) DrawStroke(activePoints, currentColor, currentWidthPx, currentAlpha) else null,
        )).forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            val path = Path().apply {
                moveTo(stroke.points.first().x, stroke.points.first().y)
                for (p in stroke.points.drop(1)) lineTo(p.x, p.y)
            }
            drawPath(
                path,
                color = stroke.color.copy(alpha = stroke.alpha),
                style = Stroke(width = stroke.widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectDragPath(
    onStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEnd: () -> Unit,
) {
    detectDragGestures(
        onDragStart = { onStart(it) },
        onDragEnd = { onEnd() },
        onDragCancel = { onEnd() },
        onDrag = { change, _ -> onDrag(change.position) },
    )
}
