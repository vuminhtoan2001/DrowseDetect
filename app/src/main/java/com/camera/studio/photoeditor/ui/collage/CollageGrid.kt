package com.camera.studio.photoeditor.ui.collage

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.camera.studio.photoeditor.ui.shared.collage.CollageLayout
import com.camera.studio.photoeditor.ui.shared.filter.FilterOption

/**
 * Vẽ lưới ảnh thật theo [CollageLayout] bằng Row/Column weight (không cần tính Rect px/dp) — cùng
 * cấu trúc với `CollageLayoutPicker`'s icon glyph, chỉ đổi khối màu thành ảnh thật.
 */
@Composable
fun CollageGrid(layout: CollageLayout, bitmaps: List<Bitmap?>, filter: FilterOption?, onAddAt: (Int) -> Unit, gapDp: Int = 3) {
    val gap = gapDp.dp
    when (layout) {
        CollageLayout.SINGLE -> Pane(bitmaps.getOrNull(0), filter, Modifier.fillMaxSize()) { onAddAt(0) }
        CollageLayout.TWO_VERTICAL -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            repeat(2) { i -> Pane(bitmaps.getOrNull(i), filter, Modifier.weight(1f).fillMaxSize()) { onAddAt(i) } }
        }
        CollageLayout.TWO_HORIZONTAL -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
            repeat(2) { i -> Pane(bitmaps.getOrNull(i), filter, Modifier.weight(1f).fillMaxSize()) { onAddAt(i) } }
        }
        CollageLayout.THREE_VERTICAL -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            repeat(3) { i -> Pane(bitmaps.getOrNull(i), filter, Modifier.weight(1f).fillMaxSize()) { onAddAt(i) } }
        }
        CollageLayout.THREE_HORIZONTAL -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
            repeat(3) { i -> Pane(bitmaps.getOrNull(i), filter, Modifier.weight(1f).fillMaxSize()) { onAddAt(i) } }
        }
        CollageLayout.FOUR_GRID -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
            repeat(2) { row ->
                Row(Modifier.weight(1f).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(2) { col -> val i = row * 2 + col; Pane(bitmaps.getOrNull(i), filter, Modifier.weight(1f).fillMaxSize()) { onAddAt(i) } }
                }
            }
        }
        CollageLayout.NINE_GRID -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
            repeat(3) { row ->
                Row(Modifier.weight(1f).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(3) { col -> val i = row * 3 + col; Pane(bitmaps.getOrNull(i), filter, Modifier.weight(1f).fillMaxSize()) { onAddAt(i) } }
                }
            }
        }
    }
}

@Composable
private fun Pane(bitmap: Bitmap?, filter: FilterOption?, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.background(Color(0xFF2A2A2E)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = filter?.matrix?.let { ColorFilter.colorMatrix(ComposeColorMatrix(it)) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
        }
    }
}
