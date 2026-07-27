package com.camera.studio.photoeditor.ui.shared.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.camera.studio.photoeditor.data.repo.MediaRepository
import com.camera.studio.photoeditor.ui.components.AppHeader
import com.camera.studio.photoeditor.ui.gallery.DeviceMediaQuery
import com.camera.studio.photoeditor.ui.gallery.GalleryViewModel
import com.camera.studio.photoeditor.ui.shared.media.DeviceMediaKind
import com.camera.studio.photoeditor.ui.shared.media.rememberMediaAccessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Chọn ảnh dùng chung Edit Photo (single-select)/Collage Maker (multi-select)/Wallpaper — 2 mục:
 * "Ảnh từ app" (có nút "+" ở header dẫn thẳng tới Camera để chụp ảnh mới) và "Ảnh trong máy" (xin
 * quyền tại chỗ nếu chưa có; có nút "+" xin cấp thêm khi mới đang partial access). Không còn
 * "Sample photo" theo góp ý thực tế.
 */
@Composable
fun MediaPickerScreen(
    title: String,
    multiSelect: Boolean,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val allMedia by viewModel.allMedia.collectAsState()
    val myPhotos = allMedia.filter { it.type == MediaRepository.TYPE_IMAGE }
    val selected = remember { mutableStateOf(setOf<String>()) }

    val mediaAccess = rememberMediaAccessState(DeviceMediaKind.IMAGE)
    val devicePhotos by produceState(initialValue = emptyList<String>(), mediaAccess.hasAnyAccess, mediaAccess.refreshKey) {
        value = if (mediaAccess.hasAnyAccess) {
            withContext(Dispatchers.IO) { DeviceMediaQuery.queryImagePaths(context) }
        } else {
            emptyList()
        }
    }

    fun toggle(path: String) {
        if (multiSelect) {
            selected.value = if (path in selected.value) selected.value - path else selected.value + path
        } else {
            onConfirm(listOf(path))
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppHeader(
            title = title,
            onBack = onBack,
            actions = {
                if (multiSelect) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (selected.value.isNotEmpty()) Color(0xFFF5C518) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(enabled = selected.value.isNotEmpty()) { onConfirm(selected.value.toList()) }.padding(12.dp),
                    )
                }
            },
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            // ---------- Mục 1: Ảnh do app tạo ra — nút "+" dẫn tới Camera để chụp ảnh mới ----------
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader("ẢNH TỪ APP")
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Chụp ảnh mới",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onOpenCamera),
                    )
                }
            }
            if (myPhotos.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptySectionHint("Chưa có ảnh nào do app tạo") }
            } else {
                items(myPhotos, key = { "app_${it.id}" }) { media ->
                    PickerTile(isSelected = media.path in selected.value, onClick = { toggle(media.path) }) {
                        AsyncImage(model = media.path, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
            }

            // ---------- Mục 2: Ảnh chọn từ trong máy ----------
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader("ẢNH TRONG MÁY")
                    // Chỉ cần khi đang partial access (đã chọn 1 số ảnh) — full access thì hết cần thêm,
                    // chưa cấp quyền gì thì đã có ô "Cho phép truy cập" riêng bên dưới rồi.
                    if (mediaAccess.canRequestMore) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = "Cho phép truy cập thêm ảnh",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = mediaAccess.requestAccess),
                        )
                    }
                }
            }
            when {
                !mediaAccess.hasAnyAccess -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(onClick = mediaAccess.requestAccess)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Cho phép truy cập ảnh trên máy", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                    }
                }
                devicePhotos.isEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) { EmptySectionHint("Không có ảnh nào trên máy") }
                }
                else -> {
                    items(devicePhotos, key = { "device_$it" }) { path ->
                        PickerTile(isSelected = path in selected.value, onClick = { toggle(path) }) {
                            AsyncImage(model = path, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        if (multiSelect) {
            Text(
                "Selected: ${selected.value.size}",
                color = Color(0xFFF5C518),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// Tiêu đề mục: chữ đậm, viết hoa, bám sát nội dung — phân biệt rõ với cảnh báo "chưa có ảnh" bên dưới
// (chữ nghiêng, canh giữa, thưa dòng) để không bị nhầm 2 loại text với nhau.
@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = modifier.padding(vertical = 6.dp),
    )
}

@Composable
private fun EmptySectionHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PickerTile(isSelected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        content()
        if (isSelected) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).clip(CircleShape).background(Color(0xFFF5C518)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(11.dp)) }
        }
    }
}
