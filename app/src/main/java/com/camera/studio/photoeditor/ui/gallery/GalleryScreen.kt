package com.camera.studio.photoeditor.ui.gallery

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.camera.studio.photoeditor.R
import com.camera.studio.photoeditor.data.db.SavedMedia
import com.camera.studio.photoeditor.data.repo.MediaRepository
import com.camera.studio.photoeditor.ui.components.AppHeader
import com.camera.studio.photoeditor.ui.components.NativeAdPlaceholder
import com.camera.studio.photoeditor.ui.components.PillTabs
import com.camera.studio.photoeditor.ui.shared.media.DeviceMediaKind
import com.camera.studio.photoeditor.ui.shared.media.rememberMediaAccessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class GalleryTab { PHOTO, VIDEO, TEMPLATES }

/**
 * My Gallery — 3 tab Photo/Video/Templates (Templates = collage/wallpaper đã tạo), toggle "xem tất
 * cả media máy" cho tab Photo/Video, Favorites — khớp docs/06_GALLERY_MANAGER.md + mockup Board 6.
 */
@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    onOpenItem: (path: String, isVideo: Boolean) -> Unit,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val allMedia by viewModel.allMedia.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    var tab by remember { mutableStateOf(GalleryTab.PHOTO) }
    var showFavoritesOnly by remember { mutableStateOf(false) }

    val mediaAccess = rememberMediaAccessState(if (tab == GalleryTab.VIDEO) DeviceMediaKind.VIDEO else DeviceMediaKind.IMAGE)
    val hasMediaPermission = mediaAccess.hasAnyAccess
    val showAddMoreButton = mediaAccess.canRequestMore

    LaunchedEffect(userSettings.viewAllDeviceMedia, tab) {
        if (userSettings.viewAllDeviceMedia && !hasMediaPermission) mediaAccess.requestAccess()
    }

    // Mỗi lần VÀO màn Gallery đều bắt đầu ở chế độ bình thường (không "xem tất cả media máy") dù
    // lần trước đã bật — toggle chỉ có tác dụng tạm trong phiên xem hiện tại, không nhớ lại.
    LaunchedEffect(Unit) {
        if (userSettings.viewAllDeviceMedia) viewModel.setViewAllDeviceMedia(false)
    }

    val baseList = when (tab) {
        GalleryTab.PHOTO -> allMedia.filter { it.type == MediaRepository.TYPE_IMAGE }
        GalleryTab.VIDEO -> allMedia.filter { it.type == MediaRepository.TYPE_VIDEO }
        GalleryTab.TEMPLATES -> allMedia.filter { it.type == MediaRepository.TYPE_COLLAGE || it.type == MediaRepository.TYPE_WALLPAPER }
    }
    val favoritePaths = remember(favorites) { favorites.map { it.path }.toSet() }
    val filtered = if (showFavoritesOnly) baseList.filter { it.path in favoritePaths } else baseList

    // Query MediaStore chạy trên Dispatchers.IO thay vì trực tiếp trong composition (main thread) — với
    // máy có nhiều ảnh, query đồng bộ trên main thread từng gây giật khi vào màn/chuyển tab Photo-Video.
    // Lấy kèm DATE_ADDED để group theo ngày giống tab "My Gallery".
    val deviceOnlyItems by produceState(
        initialValue = emptyList<DeviceMediaItem>(),
        tab, userSettings.viewAllDeviceMedia, hasMediaPermission, mediaAccess.refreshKey,
    ) {
        value = if (userSettings.viewAllDeviceMedia && hasMediaPermission && tab != GalleryTab.TEMPLATES) {
            withContext(Dispatchers.IO) {
                if (tab == GalleryTab.VIDEO) DeviceMediaQuery.queryVideoItems(context) else DeviceMediaQuery.queryImageItems(context)
            }
        } else {
            emptyList()
        }
    }
    // ImageLoader dùng chung cho cả grid — thêm VideoFrameDecoder để AsyncImage tự nhận diện + decode
    // frame video (có cache bộ nhớ/đĩa + tự downsample theo kích thước ô hiển thị, thay vì tự
    // BitmapFactory.decodeFile/MediaMetadataRetriever thủ công trên main thread cho từng item).
    val galleryImageLoader = remember(context) {
        ImageLoader.Builder(context).components { add(VideoFrameDecoder.Factory()) }.build()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppHeader(
            title = stringResource(R.string.saved_items),
            onBack = onBack,
            actions = {
                Icon(
                    if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (showFavoritesOnly) Color(0xFFE53935) else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clickable { showFavoritesOnly = !showFavoritesOnly },
                )
            },
        )
        PillTabs(
            options = listOf("Photo", "Video", "Templates"),
            selected = tab.ordinal,
            onSelect = { tab = GalleryTab.entries[it] },
        )
        if (tab != GalleryTab.TEMPLATES) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Xem tất cả ${if (tab == GalleryTab.VIDEO) "video" else "ảnh"} trong máy", fontSize = 12.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (userSettings.viewAllDeviceMedia && showAddMoreButton) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = "Cho phép truy cập thêm ảnh/video",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp).clickable(onClick = mediaAccess.requestAccess),
                        )
                    }
                    Switch(checked = userSettings.viewAllDeviceMedia, onCheckedChange = { viewModel.setViewAllDeviceMedia(it) })
                }
            }
        }

        if (userSettings.viewAllDeviceMedia && tab != GalleryTab.TEMPLATES) {
            if (deviceOnlyItems.isEmpty()) {
                EmptyState()
            } else {
                // Group theo ngày (Hôm nay/Hôm qua/theo tháng) dựa trên DATE_ADDED — cùng cách hiển thị
                // với tab "My Gallery" bên dưới, thay vì lưới phẳng không rõ mốc thời gian như trước.
                val deviceGroups = remember(deviceOnlyItems) { groupDeviceMediaByDate(deviceOnlyItems) }
                LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(2.dp), modifier = Modifier.fillMaxSize().weight(1f)) {
                    deviceGroups.forEach { (label, itemsInGroup) ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                            )
                        }
                        items(itemsInGroup, key = { it.path }) { item ->
                            DevicePathThumb(item.path, isVideo = tab == GalleryTab.VIDEO, imageLoader = galleryImageLoader, onClick = { onOpenItem(item.path, tab == GalleryTab.VIDEO) })
                        }
                    }
                }
            }
        } else if (filtered.isEmpty()) {
            EmptyState()
        } else {
            // Chia theo nhóm thời gian (Hôm nay/Hôm qua/theo tháng) — khớp góp ý "giống app vga15b,c".
            val groups = remember(filtered) { groupMediaByDate(filtered) }
            LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(2.dp), modifier = Modifier.fillMaxSize().weight(1f)) {
                groups.forEach { (label, mediaInGroup) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        )
                    }
                    items(mediaInGroup, key = { it.id }) { media ->
                        MediaThumb(media, imageLoader = galleryImageLoader, onClick = { onOpenItem(media.path, media.type == MediaRepository.TYPE_VIDEO) })
                    }
                }
            }
        }

        NativeAdPlaceholder("native_gallery", Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

/** Nhãn nhóm ngày dùng chung cho cả "My Gallery" (SavedMedia) và "ảnh trong máy" (DeviceMediaItem). */
private fun dateGroupLabel(cal: Calendar, today: Calendar, yesterday: Calendar): String = when {
    cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Hôm nay"
    cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Hôm qua"
    cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
        SimpleDateFormat("MMMM", Locale("vi", "VN")).format(cal.time).replaceFirstChar { it.uppercase() }
    else -> SimpleDateFormat("MM/yyyy", Locale.US).format(cal.time)
}

/**
 * Nhóm media theo mốc thời gian (Hôm nay/Hôm qua/theo tháng trong năm/tháng-năm cũ hơn) — [media]
 * đã sắp xếp mới nhất trước (query `ORDER BY id DESC`) nên `groupBy` giữ đúng thứ tự nhóm mới→cũ.
 */
private fun groupMediaByDate(media: List<SavedMedia>): List<Pair<String, List<SavedMedia>>> {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val today = Calendar.getInstance()
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    fun label(m: SavedMedia): String {
        val d = runCatching { sdf.parse(m.date) }.getOrNull() ?: return "Khác"
        return dateGroupLabel(Calendar.getInstance().apply { time = d }, today, yesterday)
    }
    return media.groupBy(::label).toList()
}

/**
 * Nhóm ảnh/video trên máy theo mốc thời gian dựa trên DATE_ADDED (MediaStore) — [items] đã sắp xếp
 * mới nhất trước nên `groupBy` giữ đúng thứ tự nhóm mới→cũ, khớp cách hiển thị của "My Gallery".
 */
private fun groupDeviceMediaByDate(items: List<DeviceMediaItem>): List<Pair<String, List<DeviceMediaItem>>> {
    val today = Calendar.getInstance()
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    fun label(item: DeviceMediaItem): String =
        dateGroupLabel(Calendar.getInstance().apply { timeInMillis = item.dateAddedSec * 1000 }, today, yesterday)
    return items.groupBy(::label).toList()
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 80.dp)) {
        Text(stringResource(R.string.capture_moments), color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MediaThumb(media: SavedMedia, imageLoader: ImageLoader, onClick: () -> Unit) {
    Box(Modifier.aspectRatio(1f).padding(2.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = media.path,
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (media.type == MediaRepository.TYPE_VIDEO) {
            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun DevicePathThumb(path: String, isVideo: Boolean, imageLoader: ImageLoader, onClick: () -> Unit) {
    Box(Modifier.aspectRatio(1f).padding(2.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = path,
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isVideo) Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
    }
}
