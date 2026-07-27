package com.camera.studio.photoeditor.ui.shared.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.camera.studio.photoeditor.ui.shared.asset.AssetCatalog

/** Nguồn nền của [BackgroundState] — Blur (mờ ảnh hiện có), Upload (ảnh máy), Colors (đơn sắc), Template. */
sealed class BackgroundState {
    data object Blur : BackgroundState()
    data class SolidColor(val color: Color) : BackgroundState()
    data class TemplateImage(val assetPath: String) : BackgroundState()
    data class UploadedImage(val uri: String) : BackgroundState()
}

private val BG_COLORS = listOf(
    Color.White, Color.Black, Color(0xFFE05A3C), Color(0xFFF5C518),
    Color(0xFF5AC47A), Color(0xFF4A90E2), Color(0xFF9B6FD2), Color(0xFFE068A8),
)

private enum class BgSource { BLUR, UPLOAD, COLORS, TEMPLATE }

/** Background picker dùng chung Collage Maker/Wallpaper Creator — Blur/Upload/Colors/Template (docs/07 §Background). */
@Composable
fun BackgroundPicker(
    onSelect: (BackgroundState) -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var source by remember { mutableStateOf(BgSource.TEMPLATE) }
    val templateIds = remember { AssetCatalog.templateGalleryIds(context) }

    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (source) {
            BgSource.COLORS -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BG_COLORS.forEach { c ->
                    Box(
                        Modifier.size(26.dp).background(c, CircleShape)
                            .clickable { onSelect(BackgroundState.SolidColor(c)) },
                    )
                }
            }
            BgSource.TEMPLATE -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(templateIds) { id ->
                    val path = AssetCatalog.templateBackgroundPath(id)
                    Box(Modifier.size(56.dp).background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp)).clickable { onSelect(BackgroundState.TemplateImage(path)) }) {
                        AsyncImage(model = AssetCatalog.assetUri(path), contentDescription = null, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            else -> {}
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            SourceIcon(Icons.Default.BlurOn, "Blur", source == BgSource.BLUR) { source = BgSource.BLUR; onSelect(BackgroundState.Blur) }
            SourceIcon(Icons.Default.Upload, "Upload", source == BgSource.UPLOAD) { source = BgSource.UPLOAD; onPickImage() }
            SourceIcon(Icons.Default.Palette, "Colors", source == BgSource.COLORS) { source = BgSource.COLORS }
            SourceIcon(Icons.Default.Wallpaper, "Template", source == BgSource.TEMPLATE) { source = BgSource.TEMPLATE }
        }
    }
}

@Composable
private fun SourceIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Icon(icon, contentDescription = null, tint = if (active) Color(0xFFF5C518) else Color.White)
        Text(label, color = Color.White, fontSize = 9.sp)
    }
}
