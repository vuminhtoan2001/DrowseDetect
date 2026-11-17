package com.awakedrive.drowsedetect.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.detection.DrowsinessState
import com.awakedrive.drowsedetect.service.BaselineStore
import com.awakedrive.drowsedetect.service.MonitorPhase
import com.awakedrive.drowsedetect.service.MonitorService
import com.awakedrive.drowsedetect.service.MonitorStateHolder
import com.awakedrive.drowsedetect.ui.components.HomeBackground
import com.awakedrive.drowsedetect.ui.settings.openOverlaySettings
import com.awakedrive.drowsedetect.ui.theme.BrandBlue
import com.awakedrive.drowsedetect.ui.theme.StateAmber
import com.awakedrive.drowsedetect.ui.theme.StateGreen
import com.awakedrive.drowsedetect.ui.theme.StateRed
import com.awakedrive.drowsedetect.ui.theme.TextSecondary

/** Màn chính: tự chuyển giữa Sẵn sàng → Làm quen khuôn mặt → Đang giám sát → Tổng kết chuyến. */
@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val ui by MonitorStateHolder.state.collectAsStateWithLifecycle()
    val summary by MonitorStateHolder.lastSummary.collectAsStateWithLifecycle()
    val running = ui.phase != MonitorPhase.IDLE

    // Giữ màn hình sáng khi đang giám sát (người dùng cần thấy khung camera / cảnh báo)
    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    // Android 14+: phải có CAMERA TRƯỚC khi start FGS type camera, và start khi app đang hiển thị (nút bấm).
    val pendingRecalibrate = remember { booleanArrayOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted[Manifest.permission.CAMERA] == true) MonitorService.start(context, pendingRecalibrate[0])
    }
    val startMonitoring: (Boolean) -> Unit = { recalibrate ->
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            MonitorService.start(context, recalibrate)
        } else {
            pendingRecalibrate[0] = recalibrate
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    var showDisclaimer by remember { mutableStateOf(!AppPrefs.disclaimerAccepted(context)) }
    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.disclaimer_title)) },
            text = { Text(stringResource(R.string.disclaimer_body)) },
            confirmButton = {
                TextButton(onClick = { AppPrefs.acceptDisclaimer(context); showDisclaimer = false }) {
                    Text(stringResource(R.string.disclaimer_ok))
                }
            },
        )
    }

    // Xin quyền vẽ lên app khác 1 lần (trước lần Bắt đầu đầu tiên) để cảnh báo hiện ngay cả khi đang dùng app khác
    var showOverlayDialog by remember { mutableStateOf(false) }
    if (showOverlayDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.overlay_dialog_title)) },
            text = { Text(stringResource(R.string.overlay_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    AppPrefs.markOverlayAsked(context)
                    showOverlayDialog = false
                    openOverlaySettings(context)
                }) { Text(stringResource(R.string.overlay_dialog_allow)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    AppPrefs.markOverlayAsked(context)
                    showOverlayDialog = false
                    startMonitoring(false)
                }) { Text(stringResource(R.string.overlay_dialog_later)) }
            },
        )
    }

    val tint = if (ui.phase == MonitorPhase.MONITORING) {
        when (ui.result?.state) {
            DrowsinessState.NORMAL -> StateGreen
            DrowsinessState.WARNING -> StateAmber
            DrowsinessState.DANGER -> StateRed
            DrowsinessState.NO_FACE -> TextSecondary
            null -> BrandBlue
        }
    } else BrandBlue

    HomeBackground(tint = tint, overlay = running || summary != null) {
        val sm = summary
        when {
            !running && sm != null -> SummaryScreen(sm, onDone = MonitorStateHolder::clearSummary)
            !running -> IdleContent(
                message = ui.message,
                hasBaseline = BaselineStore.load(context) != null,
                onStart = {
                    if (!Settings.canDrawOverlays(context) && !AppPrefs.overlayAsked(context)) showOverlayDialog = true
                    else startMonitoring(false)
                },
                onOpenSettings = onOpenSettings,
            )
            ui.phase == MonitorPhase.MONITORING -> MonitorContent(ui, onOpenSettings)
            else -> CalibrationContent(ui)
        }
    }
}

private object AppPrefs {
    private const val PREFS = "app_prefs"
    private const val KEY = "disclaimer_ok"

    fun disclaimerAccepted(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun overlayAsked(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("overlay_asked", false)
    fun markOverlayAsked(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("overlay_asked", true).apply()
    fun acceptDisclaimer(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
}

internal val ErrorText = Color(0xFFFFB4BD)

internal fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
