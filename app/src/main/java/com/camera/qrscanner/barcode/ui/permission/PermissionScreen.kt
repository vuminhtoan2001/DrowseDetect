package com.camera.qrscanner.barcode.ui.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.camera.qrscanner.barcode.R
import com.camera.qrscanner.barcode.ads.AdManager
import com.camera.qrscanner.barcode.ads.NativeAdSlot
import com.camera.qrscanner.barcode.ui.common.HandClickHint

@Composable
fun PermissionScreen(onDone: () -> Unit) {
    val context = LocalContext.current

    var perNoti by remember { mutableStateOf(hasNoti(context)) }

    // native_permission: nếu màn có ad → phải đợi ad settle (impression/lỗi) mới cho auto-next.
    val adUnit = AdManager.getAdUnitId(context, "native_permission")
    val adEnabled = AdManager.isAdEnabled("native_permission_enable") && adUnit.isNotBlank()
    var adSettled by remember { mutableStateOf(!adEnabled) } // không ad → coi như đã settle

    val notiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { perNoti = it || hasNoti(context) }

    // Cập nhật lại khi quay về từ Settings
    val owner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                perNoti = hasNoti(context)
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    // Auto-flow: xin notification (thêm quyền tính năng khác khi build app thật — xin sau notification)
    LaunchedEffect(Unit) {
        if (!perNoti && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notiLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Đủ quyền + ad đã settle (nếu có) → tự sang Main
    LaunchedEffect(perNoti, adSettled) {
        if (perNoti && adSettled) onDone()
    }
    // Fallback: ad treo quá lâu vẫn nhả gate để không kẹt màn Permission.
    LaunchedEffect(Unit) {
        if (adEnabled) {
            kotlinx.coroutines.delay(6000)
            adSettled = true
        }
    }

    fun requestNoti() {
        if (perNoti) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notiLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotiSettings(context)
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 24.dp)) {
            Text(
                stringResource(R.string.permission),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center),
            )
            IconButton(onClick = onDone, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "skip", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.img_per),
                contentDescription = null,
                modifier = Modifier.size(width = 160.dp, height = 146.dp),
            )
            Text(
                stringResource(R.string.perSub),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            PermRow(
                title = stringResource(R.string.noti),
                sub = stringResource(R.string.notiSub),
                granted = perNoti,
                showHand = !perNoti,
                onRequest = { requestNoti() },
            )
            // Thêm PermRow khác ở đây khi app thật cần quyền tính năng riêng (camera/location/...).
        }

        // native_permission (lib) — cố định đáy, chừa safe area nav bar (màn này không có bottom nav).
        if (adEnabled) {
            NativeAdSlot(
                slotName = "nativesdfsadd_permission",
                unitId = adUnit,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                onImpressionOrError = { adSettled = true },
            )
        }
    }
}

@Composable
private fun PermRow(
    title: String,
    sub: String,
    granted: Boolean,
    showHand: Boolean,
    onRequest: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(contentAlignment = Alignment.Center) {
            if (granted) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            } else {
                Switch(checked = false, onCheckedChange = { onRequest() })
            }
            if (showHand && !granted) {
                HandClickHint(
                    Modifier.align(Alignment.TopEnd).size(72.dp).padding(top = 0.dp),
                )
            }
        }
    }
}

private fun hasNoti(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun openNotiSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        context.startActivity(intent)
    } catch (_: Exception) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
        )
    }
}
