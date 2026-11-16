package com.awakedrive.drowsedetect.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.alert.AlertSettingsStore
import com.awakedrive.drowsedetect.detection.AlertReason
import com.awakedrive.drowsedetect.detection.DetectionConfig
import com.awakedrive.drowsedetect.detection.DrowsinessState
import com.awakedrive.drowsedetect.detection.fatigueLevel
import com.awakedrive.drowsedetect.detection.forSensitivity
import com.awakedrive.drowsedetect.service.MonitorService
import com.awakedrive.drowsedetect.service.MonitorUiState
import com.awakedrive.drowsedetect.ui.components.CameraPreview
import com.awakedrive.drowsedetect.ui.components.GlassCard
import com.awakedrive.drowsedetect.ui.components.GradientButton
import com.awakedrive.drowsedetect.ui.components.IconBadge
import com.awakedrive.drowsedetect.ui.components.OutlineButton
import com.awakedrive.drowsedetect.ui.components.RecBadge
import com.awakedrive.drowsedetect.ui.components.StatTile
import com.awakedrive.drowsedetect.ui.components.StatusPill
import com.awakedrive.drowsedetect.ui.theme.Navy600
import com.awakedrive.drowsedetect.ui.theme.Navy900
import com.awakedrive.drowsedetect.ui.theme.StateAmber
import com.awakedrive.drowsedetect.ui.theme.StateGreen
import com.awakedrive.drowsedetect.ui.theme.StateRed
import com.awakedrive.drowsedetect.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/** Màn đang giám sát: chỉ hiển thị thông tin người lái hiểu được (trạng thái, mức mệt, thời gian). */
@Composable
internal fun MonitorContent(ui: MonitorUiState, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val res = ui.result
    val state = res?.state ?: DrowsinessState.NORMAL
    val cfg = remember { DetectionConfig.forSensitivity(AlertSettingsStore.load(context).sensitivity) }
    val fatigue by animateFloatAsState(res?.let { fatigueLevel(it, cfg) } ?: 0f, label = "fatigue")

    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = SystemClock.elapsedRealtime() } }
    val elapsed = if (ui.monitoringStartMs > 0) now - ui.monitoringStartMs else 0L

    var showEndConfirm by remember { mutableStateOf(false) }
    var acked by remember { mutableStateOf(false) }
    LaunchedEffect(state) { if (state != DrowsinessState.DANGER) acked = false }

    val color = stateColor(state)
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.mon_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_open), tint = Color.White)
                }
            }

            CameraPreview(
                modifier = Modifier.fillMaxWidth().height(240.dp),
                borderColor = color,
            ) {
                RecBadge(stringResource(R.string.mon_rec), Modifier.align(Alignment.TopStart))
                if (res != null && state != DrowsinessState.NO_FACE) {
                    StatusPill(
                        stringResource(if (res.eyesClosed) R.string.eyes_closed else R.string.eyes_open),
                        if (res.eyesClosed) StateAmber else StateGreen,
                        Modifier.align(Alignment.BottomStart).padding(12.dp),
                    )
                }
            }

            StatusCard(state, res?.reason, color)

            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.fatigue_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    val (label, c) = when {
                        fatigue < 0.35f -> R.string.fatigue_low to StateGreen
                        fatigue < 0.7f -> R.string.fatigue_mid to StateAmber
                        else -> R.string.fatigue_high to StateRed
                    }
                    Text(stringResource(label), style = MaterialTheme.typography.titleMedium, color = c)
                }
                Spacer(Modifier.height(10.dp))
                FatigueBar(fatigue)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(stringResource(R.string.stat_time), formatDuration(elapsed), Modifier.weight(1.3f))
                StatTile(stringResource(R.string.stat_yawn), "${res?.yawnCount ?: 0}", Modifier.weight(1f))
                StatTile(stringResource(R.string.stat_nod), "${res?.nodCount ?: 0}", Modifier.weight(1f))
            }

            OutlineButton(
                stringResource(R.string.mon_end),
                onClick = { showEndConfirm = true },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                color = StateRed,
            )
        }

        if (state == DrowsinessState.DANGER && !acked) {
            AlertOverlay(
                reason = res?.reason,
                onAwake = { MonitorService.acknowledge(context); acked = true },
                onFindRest = {
                    MonitorService.acknowledge(context); acked = true
                    openRestStops(context, context.getString(R.string.rest_query))
                },
            )
        }
    }

    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            title = { Text(stringResource(R.string.end_confirm_title)) },
            confirmButton = {
                TextButton(onClick = { showEndConfirm = false; MonitorService.stop(context) }) {
                    Text(stringResource(R.string.end_confirm_yes), color = StateRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = false }) { Text(stringResource(R.string.end_confirm_no)) }
            },
        )
    }
}

internal fun stateColor(s: DrowsinessState): Color = when (s) {
    DrowsinessState.NORMAL -> StateGreen
    DrowsinessState.WARNING -> StateAmber
    DrowsinessState.DANGER -> StateRed
    DrowsinessState.NO_FACE -> TextSecondary
}

@Composable
private fun StatusCard(state: DrowsinessState, reason: AlertReason?, color: Color) {
    val icon: ImageVector
    val title: Int
    val sub: Int
    val pill: Int
    when (state) {
        DrowsinessState.NORMAL -> { icon = Icons.Filled.CheckCircle; title = R.string.mon_ok_title; sub = R.string.mon_ok_sub; pill = R.string.pill_ok }
        DrowsinessState.WARNING -> { icon = Icons.Filled.Warning; title = R.string.mon_warn_title; sub = R.string.mon_warn_sub; pill = R.string.pill_warn }
        DrowsinessState.DANGER -> { icon = Icons.Filled.Error; title = R.string.mon_danger_title; sub = R.string.mon_warn_sub; pill = R.string.pill_danger }
        DrowsinessState.NO_FACE -> { icon = Icons.Filled.VisibilityOff; title = R.string.mon_noface_title; sub = R.string.mon_noface_sub; pill = R.string.pill_noface }
    }
    GlassCard(Modifier.fillMaxWidth(), borderColor = color.copy(alpha = 0.8f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, color, size = 52.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
                // Khi cảnh báo, nói rõ LÝ DO bằng lời dễ hiểu thay vì con số
                val subText = reasonText(reason).takeIf { state == DrowsinessState.WARNING || state == DrowsinessState.DANGER }
                Text(
                    subText?.let { stringResource(it) } ?: stringResource(sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            StatusPill(stringResource(pill), color)
        }
    }
}

internal fun reasonText(reason: AlertReason?): Int? = when (reason) {
    AlertReason.MICROSLEEP -> R.string.alert_reason_microsleep
    AlertReason.PERCLOS -> R.string.alert_reason_perclos
    AlertReason.YAWN -> R.string.alert_reason_yawn
    AlertReason.HEAD_NOD -> R.string.alert_reason_nod
    null -> null
}

@Composable
private fun FatigueBar(level: Float) {
    val color = when {
        level < 0.35f -> StateGreen
        level < 0.7f -> StateAmber
        else -> StateRed
    }
    Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Navy600)) {
        Box(
            Modifier.fillMaxWidth(level.coerceIn(0.03f, 1f)).height(12.dp).clip(RoundedCornerShape(6.dp))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color))),
        )
    }
}

/** Toàn màn đỏ nhấp nháy khi NGUY HIỂM, kèm 2 hành động rõ ràng. */
@Composable
private fun AlertOverlay(reason: AlertReason?, onAwake: () -> Unit, onFindRest: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "alpha",
    )
    Box(Modifier.fillMaxSize().background(Navy900)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(StateRed.copy(alpha = pulse), Color(0xFF1A0008)))))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(120.dp))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.alert_title), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            reasonText(reason)?.let {
                Text(stringResource(it), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            }
            Text(
                stringResource(R.string.alert_sub),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFFFD6DA),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
            )
            GradientButton(
                stringResource(R.string.alert_find_rest), onFindRest,
                modifier = Modifier.fillMaxWidth(), icon = Icons.Filled.Place,
                colors = listOf(Color(0xFFFF5A5F), Color(0xFFD7263D)),
            )
            Spacer(Modifier.height(14.dp))
            OutlineButton(stringResource(R.string.monitor_ack), onAwake, Modifier.fillMaxWidth())
        }
    }
}

private fun openRestStops(context: Context, query: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query))))
    } catch (_: ActivityNotFoundException) {
        // máy không có app bản đồ: bỏ qua, người dùng vẫn tắt được cảnh báo bằng nút "Tôi đã tỉnh"
    }
}
