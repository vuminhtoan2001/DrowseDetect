package com.camera.studio.photoeditor.ui.importphoto

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.camera.studio.photoeditor.R
import com.camera.studio.photoeditor.ui.components.AppHeader
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Photo/Video Detail — Delete/Favorite/Info/Edit/Download/Share, khớp `detail_video.jpg`/mockup Board 6. */
@Composable
fun ImportPhotoResultScreen(
    path: String,
    isVideo: Boolean,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onEdit: (path: String) -> Unit = {},
    viewModel: ImportPhotoResultViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val allMedia by viewModel.allMedia.collectAsState(initial = emptyList())
    val isFavorite = allMedia.find { it.path == path }?.isFavorite == true

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppHeader(
            title = if (isVideo) stringResource(R.string.video) else stringResource(R.string.image),
            onBack = onBack,
            actions = {
                RoundIconButton(Icons.Default.Share, background = Color(0xFF0C87FF)) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = if (isVideo) "video/*" else "image/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
            },
        )

        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            if (isVideo) {
                VideoPlayerView(path)
            } else {
                val bmp = remember(path) { android.graphics.BitmapFactory.decodeFile(path) }
                bmp?.let {
                    Image(bitmap = it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ActionButton(Icons.Default.Delete, "Delete") { showDeleteDialog = true }
            ActionButton(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite", tint = if (isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onBackground) {
                scope.launch { viewModel.setFavorite(path, !isFavorite) }
            }
            ActionButton(Icons.Default.Info, "Info") { showInfoDialog = true }
            if (!isVideo) ActionButton(Icons.Default.Edit, "Edit") { onEdit(path) }
            ActionButton(Icons.Default.Download, "Download") {
                scope.launch { viewModel.downloadToPublicStorage(path, isVideo) }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(if (isVideo) stringResource(R.string.delete_video) else stringResource(R.string.delete_photo)) },
            text = { Text(if (isVideo) stringResource(R.string.ques_delete_video) else stringResource(R.string.ques_delete_photo)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        viewModel.deleteMedia(path)
                        showDeleteDialog = false
                        onDeleted()
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (showInfoDialog) {
        val file = File(path)
        val sizeKb = runCatching { file.length() / 1024 }.getOrDefault(0L)
        val modified = runCatching { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(file.lastModified())) }.getOrDefault("")
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(if (isVideo) "Detail video" else "Detail photo") },
            text = {
                Column {
                    Text("Name: ${file.name}", fontSize = 13.sp)
                    Text("Size: ${sizeKb}KB", fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    Text("Date: $modified", fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
            },
            confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.onBackground, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Box (KHÔNG dùng Material3 IconButton) — IconButton tự áp `minimumInteractiveComponentSize()`
 * (nới vùng chạm lên tối thiểu 48dp dù đã set .size(32.dp)), khiến 2 nút cạnh nhau LOOK như dính
 * dù đã có spacedBy giữa 2 IconButton — dùng Box.clickable thường để kích thước = đúng .size().
 */
@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color = Color(0xFFCBCBCB),
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun VideoPlayerView(path: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(File(path).toURI().toString()))
            prepare()
        }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = true } },
        modifier = Modifier.fillMaxSize(),
    )
}
