package com.camera.studio.photoeditor.ui.home

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.camera.studio.photoeditor.R
import com.camera.studio.photoeditor.platform.IapLauncher
import com.camera.studio.photoeditor.ui.components.NativeAdPlaceholder
import com.camera.studio.photoeditor.ui.shared.asset.AssetCatalog

private val AccentYellow = Color(0xFFF5C518)
private val StudioDark = Color(0xFF15161A)

/**
 * Home — hero "TAKE PHOTO" + lưới 2x2 "CREATIVE STUDIO" (Edit Photo/Collage Maker/My Gallery/
 * Settings), theo đúng `vga52a/screenshot/home.jpg` + mockup Board 1.
 * `onOpenEditPhoto`/`onOpenCollageMaker` mặc định no-op — nối route thật khi M4/M5 dựng xong.
 */
@Composable
fun HomeScreen(
    onOpenCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenItem: (path: String, isVideo: Boolean) -> Unit,
    onOpenEditPhoto: () -> Unit = {},
    onOpenCollageMaker: () -> Unit = {},
    onOpenWallpaper: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.app_name),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WorkspacePremium, contentDescription = null, tint = AccentYellow,
                    modifier = Modifier.clickable { IapLauncher.open(context) },
                )
                Icon(
                    Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onOpenSettings),
                )
            }
        }

        // ---------- Hero: TAKE PHOTO ----------
        Box(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .aspectRatio(1.46f)
                .clip(RoundedCornerShape(16.dp))
                .background(StudioDark)
                .clickable(onClick = onOpenCamera),
        ) {
            AsyncImage(
                model = AssetCatalog.assetUri("images/bg_take_photo.webp"),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
            Column(Modifier.align(Alignment.CenterStart).padding(start = 20.dp)) {
                Text("PRO MODE", color = AccentYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.home_take_photo),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    stringResource(R.string.home_take_photo_subtitle),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, end = 90.dp),
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(AccentYellow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.Black)
            }
        }

        Text(
            stringResource(R.string.home_creative_studio),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 10.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            items(homeStudioCards) { card ->
                StudioCard(card = card, onClick = {
                    when (card.title) {
                        R.string.home_edit_photo -> onOpenEditPhoto()
                        R.string.home_collage_maker -> onOpenCollageMaker()
                        R.string.home_wallpaper -> onOpenWallpaper()
                        R.string.home_my_gallery -> onOpenGallery()
                    }
                })
            }
        }

        NativeAdPlaceholder("native_home", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), isSmall = true)
    }
}

private data class StudioCardSpec(val title: Int, val icon: ImageVector)

private val homeStudioCards = listOf(
    StudioCardSpec(R.string.home_edit_photo, Icons.Default.AutoFixHigh),
    StudioCardSpec(R.string.home_collage_maker, Icons.Default.GridView),
    StudioCardSpec(R.string.home_wallpaper, Icons.Default.Wallpaper),
    StudioCardSpec(R.string.home_my_gallery, Icons.Default.PhotoLibrary),
)

@Composable
private fun StudioCard(card: StudioCardSpec, onClick: () -> Unit) {
    Column(
        Modifier
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(AccentYellow.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(card.icon, contentDescription = null, tint = AccentYellow)
        }
        Text(
            stringResource(card.title),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 1,
        )
    }
}
