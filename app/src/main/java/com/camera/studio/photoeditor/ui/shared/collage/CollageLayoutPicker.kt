package com.camera.studio.photoeditor.ui.shared.collage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val COLUMNS = 5

/**
 * 7 icon layout — dòng 1 đủ 5, dòng 2 còn 2 CĂN GIỮA (dùng weight-spacer 2 bên cùng đơn vị cột với
 * dòng trên, không phải LazyVerticalGrid Fixed(5) căn trái mặc định) — khớp
 * `Screenshot_20260724_094708_*.jpg`. Dùng chung ở Camera More-menu (M2) và Collage Maker (M5).
 */
@Composable
fun CollageLayoutPicker(
    selected: CollageLayout,
    onSelect: (CollageLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CollageLayout.entries.chunked(COLUMNS).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val missing = COLUMNS - rowItems.size
                if (missing > 0) Spacer(Modifier.weight(missing / 2f))
                rowItems.forEach { layout ->
                    val isSelected = layout == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0x47E05A3C) else Color.White.copy(alpha = 0.1f))
                            .then(if (isSelected) Modifier.border(1.5.dp, Color(0xFFF5C518), RoundedCornerShape(6.dp)) else Modifier)
                            .clickable { onSelect(layout) }
                            .padding(5.dp),
                    ) {
                        LayoutGlyph(layout, isSelected)
                    }
                }
                if (missing > 0) Spacer(Modifier.weight(missing / 2f))
            }
        }
    }
}

@Composable
private fun LayoutGlyph(layout: CollageLayout, selected: Boolean) {
    val paneColor = if (selected) Color(0xFFF5C518) else Color.White.copy(alpha = 0.45f)
    when (layout) {
        CollageLayout.SINGLE -> Box(Modifier.fillMaxSize().background(paneColor, RoundedCornerShape(1.dp)))
        CollageLayout.TWO_VERTICAL -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.weight(1f).fillMaxSize().background(paneColor, RoundedCornerShape(1.dp)))
            Box(Modifier.weight(1f).fillMaxSize().background(paneColor, RoundedCornerShape(1.dp)))
        }
        CollageLayout.TWO_HORIZONTAL -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.weight(1f).fillMaxSize().background(paneColor, RoundedCornerShape(1.dp)))
            Box(Modifier.weight(1f).fillMaxSize().background(paneColor, RoundedCornerShape(1.dp)))
        }
        CollageLayout.THREE_VERTICAL -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(3) { Box(Modifier.weight(1f).fillMaxSize().background(paneColor, RoundedCornerShape(1.dp))) }
        }
        CollageLayout.THREE_HORIZONTAL -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(3) { Box(Modifier.weight(1f).fillMaxSize().background(paneColor, RoundedCornerShape(1.dp))) }
        }
        CollageLayout.FOUR_GRID -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(2) {
                Row(Modifier.weight(1f).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(Modifier.weight(1f).fillMaxSize().background(paneColor, RoundedCornerShape(1.dp)))
                    Box(Modifier.weight(1f).fillMaxSize().background(paneColor, RoundedCornerShape(1.dp)))
                }
            }
        }
        CollageLayout.NINE_GRID -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(3) {
                Row(Modifier.weight(1f).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) { Box(Modifier.weight(1f).fillMaxSize().background(paneColor, RoundedCornerShape(1.dp))) }
                }
            }
        }
    }
}
