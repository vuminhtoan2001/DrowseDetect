package com.camera.studio.photoeditor.ui.shared.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camera.studio.photoeditor.ui.theme.AppFontOptions

private enum class TextTool { EDIT, FONT, SIZE, COLORS, STYLE, ALIGN, ARC }

private val TEXT_COLORS = listOf(
    Color.White, Color.Black, Color(0xFFE05A3C), Color(0xFFF5C518),
    Color(0xFF5AC47A), Color(0xFF4A90E2), Color(0xFF9B6FD2), Color(0xFFE068A8),
)

/**
 * Toolbar Text dùng chung — icon tròn phía trên/nhãn dưới, khớp `collage_edit_text.jpg`.
 * [showArc] bật ở Collage Maker (có ARC), tắt ở Wallpaper Creator (theo docs/07 §Text).
 * 2 tầng: hàng icon (tầng 1) luôn hiện; chọn 1 icon mở sub-control (tầng 2) phía trên kèm nút X
 * (+ ✓ cho loại slider) để đóng sub-control quay lại hàng icon — không đóng cả Text tool (đóng hẳn
 * Text tool dùng nút X ở header tổng của màn hình, theo đúng góp ý user).
 */
@Composable
fun TextToolbar(
    layer: EditorLayer.TextLayer,
    onChange: (EditorLayer.TextLayer) -> Unit,
    onDelete: () -> Unit,
    showArc: Boolean,
    modifier: Modifier = Modifier,
) {
    var activeTool by remember { mutableStateOf<TextTool?>(TextTool.EDIT) }

    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val current = activeTool
        if (current != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.clickable { activeTool = null })
                if (current == TextTool.SIZE || current == TextTool.ARC) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFF5C518), modifier = Modifier.clickable { activeTool = null })
                }
            }
        }
        when (current) {
            TextTool.EDIT -> BasicTextField(
                value = layer.text,
                onValueChange = { onChange(layer.copy(text = it)) },
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.1f)).padding(10.dp),
            )
            TextTool.FONT -> LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(AppFontOptions.size) { i ->
                    val opt = AppFontOptions[i]
                    Text(
                        opt.displayName,
                        color = if (layer.fontIndex == i) Color(0xFFF5C518) else Color.White,
                        fontFamily = opt.family,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { onChange(layer.copy(fontIndex = i)) },
                    )
                }
            }
            TextTool.SIZE -> Slider(
                value = layer.fontSizeSp,
                onValueChange = { onChange(layer.copy(fontSizeSp = it)) },
                valueRange = 12f..72f,
            )
            TextTool.COLORS -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TEXT_COLORS.forEach { c ->
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(24.dp).clip(CircleShape).background(c)
                            .then(if (c == layer.color) Modifier.background(Color.Transparent) else Modifier)
                            .clickable { onChange(layer.copy(color = c)) },
                    )
                }
            }
            TextTool.STYLE -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.FormatBold, contentDescription = null, tint = if (layer.bold) Color(0xFFF5C518) else Color.White, modifier = Modifier.clickable { onChange(layer.copy(bold = !layer.bold)) })
                Icon(Icons.Default.FormatItalic, contentDescription = null, tint = if (layer.italic) Color(0xFFF5C518) else Color.White, modifier = Modifier.clickable { onChange(layer.copy(italic = !layer.italic)) })
            }
            TextTool.ALIGN -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.FormatAlignLeft, contentDescription = null, tint = if (layer.align == TextAlign.Start) Color(0xFFF5C518) else Color.White, modifier = Modifier.clickable { onChange(layer.copy(align = TextAlign.Start)) })
                Icon(Icons.Default.FormatAlignCenter, contentDescription = null, tint = if (layer.align == TextAlign.Center) Color(0xFFF5C518) else Color.White, modifier = Modifier.clickable { onChange(layer.copy(align = TextAlign.Center)) })
                Icon(Icons.Default.FormatAlignRight, contentDescription = null, tint = if (layer.align == TextAlign.End) Color(0xFFF5C518) else Color.White, modifier = Modifier.clickable { onChange(layer.copy(align = TextAlign.End)) })
            }
            TextTool.ARC -> Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = layer.arcPercent.toFloat(),
                    onValueChange = { onChange(layer.copy(arcPercent = it.toInt())) },
                    valueRange = -100f..100f,
                    modifier = Modifier.weight(1f),
                )
                Text("${layer.arcPercent}%", color = Color.White, fontSize = 11.sp)
            }
            null -> {}
        }

        fun toggle(tool: TextTool) { activeTool = if (activeTool == tool) null else tool }

        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            ToolIcon(Icons.Default.TextFields, "Edit", current == TextTool.EDIT) { toggle(TextTool.EDIT) }
            ToolIcon(Icons.Default.TextFields, "Font", current == TextTool.FONT) { toggle(TextTool.FONT) }
            ToolIcon(Icons.Default.TextFields, "Size", current == TextTool.SIZE) { toggle(TextTool.SIZE) }
            ToolIcon(Icons.Default.Colorize, "Colors", current == TextTool.COLORS) { toggle(TextTool.COLORS) }
            ToolIcon(Icons.Default.FormatBold, "Style", current == TextTool.STYLE) { toggle(TextTool.STYLE) }
            ToolIcon(Icons.Default.FormatAlignCenter, "Align", current == TextTool.ALIGN) { toggle(TextTool.ALIGN) }
            if (showArc) ToolIcon(Icons.Default.TextFields, "ARC", current == TextTool.ARC) { toggle(TextTool.ARC) }
            ToolIcon(Icons.Default.Delete, "Delete", false, onDelete)
        }
    }
}

@Composable
private fun ToolIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        androidx.compose.foundation.layout.Box(
            Modifier.size(26.dp).clip(CircleShape).background(if (active) Color(0xFFF5C518).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = if (active) Color(0xFFF5C518) else Color.White, modifier = Modifier.size(14.dp)) }
        Text(label, color = Color.White, fontSize = 8.sp)
    }
}
