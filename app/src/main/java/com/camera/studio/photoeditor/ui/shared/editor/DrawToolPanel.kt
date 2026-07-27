package com.camera.studio.photoeditor.ui.shared.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DRAW_COLORS = listOf(
    Color.White, Color(0xFF9B6FD2), Color(0xFFE0A13C), Color(0xFF9AA0A6), Color(0xFF5AC47A), Color(0xFF4A90E2), Color.Black,
)

/** Panel Draw dùng chung Edit Photo/Collage Maker/Wallpaper Creator — khớp Screenshot_083743. */
@Composable
fun DrawToolPanel(
    color: Color,
    onColorChange: (Color) -> Unit,
    widthPx: Float,
    onWidthChange: (Float) -> Unit,
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Default.Undo, contentDescription = null, tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.3f), modifier = Modifier.clickable(enabled = canUndo, onClick = onUndo))
                Icon(Icons.Default.Redo, contentDescription = null, tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.3f), modifier = Modifier.clickable(enabled = canRedo, onClick = onRedo))
            }
            Text("Brush", color = Color.White, fontSize = 12.sp)
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFF5C518), modifier = Modifier.clickable(onClick = onDone))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DRAW_COLORS.forEach { c ->
                androidx.compose.foundation.layout.Box(
                    Modifier.size(20.dp).background(c, CircleShape)
                        .then(if (c == color) Modifier.background(Color(0x33F5C518), CircleShape) else Modifier)
                        .clickable { onColorChange(c) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Size", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
            Slider(value = widthPx, onValueChange = onWidthChange, valueRange = 2f..40f, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Opacity", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
            Slider(value = alpha, onValueChange = onAlphaChange, valueRange = 0.1f..1f, modifier = Modifier.weight(1f))
        }
    }
}
