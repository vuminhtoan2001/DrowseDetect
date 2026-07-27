package com.camera.studio.photoeditor.ui.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.camera.studio.photoeditor.ui.components.AppHeader
import com.camera.studio.photoeditor.ui.shared.asset.AssetCatalog

/** Templates gallery Wallpaper Creator — khớp `wallpaper_template.jpg`. Template 1-2 free, còn lại khoá crown. */
@Composable
fun WallpaperTemplatesScreen(onBack: () -> Unit, onOpenTemplate: (Int) -> Unit) {
    val context = LocalContext.current
    val ids = remember { AssetCatalog.templateGalleryIds(context) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppHeader(title = "Wallpaper Creator", onBack = onBack)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(ids) { id ->
                val isPremium = id > 2
                Box(
                    Modifier.padding(6.dp).aspectRatio(9f / 16f).clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenTemplate(id) },
                ) {
                    AsyncImage(
                        model = AssetCatalog.assetUri(AssetCatalog.templatePreviewPath(id)),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (isPremium) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFF5C518), modifier = Modifier.size(12.dp)) }
                    }
                }
            }
        }
    }
}
