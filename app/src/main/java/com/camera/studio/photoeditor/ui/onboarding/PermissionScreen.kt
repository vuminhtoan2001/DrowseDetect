package com.camera.studio.photoeditor.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.camera.studio.photoeditor.R
import com.camera.studio.photoeditor.ui.components.NativeAdPlaceholder
import com.camera.studio.photoeditor.ui.theme.Primary

private fun hasPermission(context: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/**
 * 2 quyền cần cho core feature ở M0-M7: Camera + Microphone (quay video). Xin TUẦN TỰ — quyền kế
 * tiếp chỉ xin SAU KHI hệ thống trả lời quyền trước. Notification (POST_NOTIFICATIONS, Android 13+)
 * sẽ thêm lại ở M8 cùng base-application AAR (đã bỏ khỏi Manifest ở M0). Quyền Storage/Media KHÔNG
 * xin ở đây — xin theo tính năng lúc Save (M2/M4) hoặc lúc bật toggle "xem tất cả media máy" ở
 * Gallery (M7), theo đúng docs/01_ONBOARDING_HOME_SETTINGS.md §5.
 */
@Composable
fun PermissionScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCamera by remember { mutableStateOf(hasPermission(context, Manifest.permission.CAMERA)) }
    var hasAudio by remember { mutableStateOf(hasPermission(context, Manifest.permission.RECORD_AUDIO)) }

    // stage: 0=camera, 1=audio, 2=done. Tăng dần SAU MỖI callback.
    var stage by remember { mutableIntStateOf(0) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it; stage = 1 }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasAudio = it; stage = 2 }

    fun refresh() {
        hasCamera = hasPermission(context, Manifest.permission.CAMERA)
        hasAudio = hasPermission(context, Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(stage) {
        when (stage) {
            0 -> if (hasCamera) stage = 1 else cameraLauncher.launch(Manifest.permission.CAMERA)
            1 -> if (hasAudio) stage = 2 else audioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val allGranted = hasCamera && hasAudio
    LaunchedEffect(allGranted) { if (allGranted) onDone() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.permission_title), fontSize = 26.sp, modifier = Modifier.padding(16.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.img_permission),
                contentDescription = null,
                modifier = Modifier.size(165.dp).clip(CircleShape),
            )
            Text(
                stringResource(R.string.grant_permission_sub),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            PermissionRow(stringResource(R.string.camera), hasCamera) { cameraLauncher.launch(Manifest.permission.CAMERA) }
            PermissionRow(stringResource(R.string.audio), hasAudio) { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) }

            Text(
                stringResource(R.string.skip),
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).clickable(onClick = onDone),
            )
        }

        NativeAdPlaceholder("native_permission", Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, onRequest: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title)
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            if (granted) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary)
            } else {
                Switch(
                    checked = false,
                    onCheckedChange = { onRequest() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Primary),
                    modifier = Modifier.scale(0.65f),
                )
            }
        }
    }
}
