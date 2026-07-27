package com.camera.studio.photoeditor.ui.shared.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
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

/** Frame picker dùng chung Edit Photo/Collage Maker — 5 category (Party/Travel/Floral/Cute/Vintage). */
@Composable
fun FramePicker(selectedAssetPath: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val categories = listOf("party", "travel", "floral", "cute", "vintage")
    var categoryIndex by remember { mutableStateOf(0) }
    val category = categories[categoryIndex]
    val items = remember(category) { AssetCatalog.framesInCategory(context, category) }

    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            item {
                Box(
                    Modifier.aspectRatio(1f).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .clickable { onSelect(null) },
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) { Icon(Icons.Default.Block, contentDescription = null, tint = Color.White) }
            }
            items(items) { path ->
                Box(
                    Modifier.aspectRatio(1f).background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                        .clickable { onSelect(path) },
                ) {
                    AsyncImage(model = AssetCatalog.assetUri(path), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
