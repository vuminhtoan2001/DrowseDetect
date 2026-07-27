package com.camera.studio.photoeditor.ui.shared.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.camera.studio.photoeditor.ui.shared.asset.AssetCatalog

/**
 * Sticker picker dùng chung (Edit Photo/Collage Maker/Wallpaper Creator) — 5 category
 * (Cute/Kawaii/Party/Vintage/Minimal) + nút mở rộng (mũi tên lên, sát mép phải, thẳng hàng category
 * tabs) để phóng to lưới, khớp `edit_image_sticker.jpg`.
 */
@Composable
fun StickerPicker(onPick: (assetPath: String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val categories = AssetCatalog.STICKER_CATEGORIES.split(",")
    var categoryIndex by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    val category = categories[categoryIndex]
    val items = remember(category) { AssetCatalog.stickersInCategory(context, category) }

    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                categories.forEachIndexed { i, cat ->
                    Text(
                        cat.replaceFirstChar { it.uppercase() },
                        color = if (i == categoryIndex) Color(0xFFF5C518) else Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { categoryIndex = i },
                    )
                }
            }
            Box(
                Modifier.size(18.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable { expanded = !expanded },
            ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp)) }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(if (expanded) 260.dp else 100.dp),
        ) {
            items(items) { path ->
                Box(
                    Modifier.aspectRatio(1f).background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                        .clickable { onPick(path) },
                ) {
                    AsyncImage(model = AssetCatalog.assetUri(path), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
