package com.awakedrive.drowsedetect.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.awakedrive.drowsedetect.detection.Baseline
import com.awakedrive.drowsedetect.detection.DetectionConfig
import com.awakedrive.drowsedetect.service.MonitorUiState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.detection.DrowsinessState
import com.awakedrive.drowsedetect.service.MonitorPhase
import com.awakedrive.drowsedetect.service.MonitorService
import com.awakedrive.drowsedetect.service.MonitorStateHolder

/** Màn chính: bảng điều khiển giám sát buồn ngủ (camera preview + chỉ số trực tiếp + giải thích cơ chế). */
@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val ui by MonitorStateHolder.state.collectAsStateWithLifecycle()
    val running = ui.phase != MonitorPhase.IDLE

    // Android 14+: phải có CAMERA TRƯỚC khi start FGS type camera, và start khi app đang hiển thị (nút bấm).
    val pendingRecalibrate = remember { booleanArrayOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted[Manifest.permission.CAMERA] == true) MonitorService.start(context, pendingRecalibrate[0])
    }
    fun startMonitoring(recalibrate: Boolean) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            MonitorService.start(context, recalibrate)
        } else {
            pendingRecalibrate[0] = recalibrate
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    val state = ui.result?.state
    val bg = when (state) {
        DrowsinessState.DANGER -> Color(0xFFB00020)
        DrowsinessState.WARNING -> Color(0xFFB8860B)
        DrowsinessState.NO_FACE -> Color(0xFF455A64)
        DrowsinessState.NORMAL -> Color(0xFF1B5E20)
        null -> MaterialTheme.colorScheme.background
    }

    Box(Modifier.fillMaxSize().background(bg).statusBarsPadding().padding(16.dp)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.settings_open),
                        tint = if (state == null) MaterialTheme.colorScheme.onBackground else Color.White,
                    )
                }
            }
            Text(
                when {
                    ui.phase == MonitorPhase.CALIBRATING -> stringResource(R.string.monitor_calibrating)
                    state != null -> stateLabel(state)
                    else -> stringResource(R.string.app_name)
                },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = if (state == null) MaterialTheme.colorScheme.onBackground else Color.White,
            )
            if (ui.phase == MonitorPhase.CALIBRATING) {
                LinearProgressIndicator(progress = { ui.calibrationProgress }, modifier = Modifier.fillMaxWidth())
            }
            ui.message?.let { Text(it, color = Color(0xFFFFCDD2), textAlign = TextAlign.Center) }

            CameraPreviewBox(running)

            if (!running) {
                Button(onClick = { startMonitoring(false) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.monitor_start))
                }
                OutlinedButton(onClick = { startMonitoring(true) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.monitor_recalibrate))
                }
            } else {
                if (state == DrowsinessState.DANGER) {
                    Button(onClick = { MonitorService.acknowledge(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.monitor_ack))
                    }
                }
                OutlinedButton(onClick = { MonitorService.stop(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.monitor_stop))
                }
            }

            if (running && ui.phase == MonitorPhase.MONITORING) LiveMetrics(ui)
            ExplainCard()
            DebugPanel(ui, if (state == null) MaterialTheme.colorScheme.onBackground else Color.White)
        }
    }
}

@Composable
private fun stateLabel(s: DrowsinessState) = stringResource(
    when (s) {
        DrowsinessState.NORMAL -> R.string.monitor_state_normal
        DrowsinessState.WARNING -> R.string.monitor_state_warning
        DrowsinessState.DANGER -> R.string.monitor_state_danger
        DrowsinessState.NO_FACE -> R.string.monitor_state_no_face
    },
)

@Composable
private fun DebugPanel(ui: com.awakedrive.drowsedetect.service.MonitorUiState, color: Color) {
    val f = ui.features
    val r = ui.result
    val b = ui.baseline
    fun v(x: Float) = "%.3f".format(x)
    val text = buildString {
        appendLine("phase=${ui.phase}  delegate=${if (ui.usingGpu) "GPU" else "CPU"}")
        appendLine("fps=${"%.1f".format(ui.fps)}  infer=${ui.inferMs}ms")
        if (b != null) appendLine("baseline ear=${v(b.ear)} mar=${v(b.mar)} pitch=${"%.1f".format(b.pitchDeg)}")
        if (f != null) {
            appendLine("face=${f.faceDetected}")
            appendLine("EAR L=${v(f.earLeft)} R=${v(f.earRight)} avg=${v(f.earAvg)}")
            appendLine("blink L=${v(f.blinkLeft)} R=${v(f.blinkRight)}")
            appendLine("MAR=${v(f.mar)}  jawOpen=${v(f.jawOpen)}")
            appendLine("pitch=${"%.1f".format(f.pitchDeg)} yaw=${"%.1f".format(f.yawDeg)} roll=${"%.1f".format(f.rollDeg)}")
        }
        if (r != null) {
            appendLine("state=${r.state} reason=${r.reason}")
            appendLine("closed=${r.eyesClosed} (${r.eyesClosedMs}ms)  PERCLOS=${v(r.perclos)}")
            appendLine("yawn=${r.yawnCount} nod=${r.nodCount}")
        }
    }
    Text(text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
private fun CameraPreviewBox(running: Boolean) {
    if (!running) return
    val context = LocalContext.current
    val preview by MonitorStateHolder.preview.collectAsStateWithLifecycle()
    val view = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    // Gắn surface khi màn đang hiển thị; gỡ khi rời màn (service vẫn chạy ImageAnalysis ở nền).
    DisposableEffect(preview, view) {
        preview?.setSurfaceProvider(view.surfaceProvider)
        onDispose { preview?.setSurfaceProvider(null) }
    }
    AndroidView(
        factory = { view },
        modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(16.dp)),
    )
}

private val CFG = DetectionConfig()

/** Hiển thị trực quan từng chỉ số đang đẩy trạng thái lên mức nào. */
@Composable
private fun LiveMetrics(ui: MonitorUiState) {
    val f = ui.features
    val r = ui.result ?: return
    val base = ui.baseline ?: Baseline.DEFAULT
    val earThreshold = base.ear * CFG.eyeClosedRatio
    fun v(x: Float) = "%.2f".format(x)

    Card(colors = CardDefaults.cardColors(containerColor = Color(0x33000000)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(if (r.eyesClosed) R.string.monitor_eyes_closed else R.string.monitor_eyes_open),
                style = MaterialTheme.typography.titleLarge,
                color = if (r.eyesClosed) Color(0xFFFF8A80) else Color(0xFFB9F6CA),
            )
            Text(stringResource(R.string.monitor_eye_rule, v(earThreshold), v(base.ear)), style = MaterialTheme.typography.bodySmall, color = Color.White)
            if (f != null) {
                Text(
                    stringResource(R.string.monitor_eye_now, v(f.earAvg), v((f.blinkLeft + f.blinkRight) / 2f)),
                    style = MaterialTheme.typography.bodySmall, color = Color.White,
                )
            }
            MetricBar(
                stringResource(R.string.monitor_bar_microsleep, "%.1f".format(r.eyesClosedMs / 1000f)),
                r.eyesClosedMs.toFloat() / CFG.microsleepMs,
            )
            MetricBar(
                stringResource(R.string.monitor_bar_perclos, (r.perclos * 100).toInt()),
                r.perclos / CFG.perclosDanger,
            )
            MetricBar(stringResource(R.string.monitor_bar_yawn, r.yawnCount), r.yawnCount.toFloat() / CFG.yawnCountWarning)
            MetricBar(stringResource(R.string.monitor_bar_nod, r.nodCount), r.nodCount.toFloat() / CFG.nodCountWarning)
        }
    }
}

@Composable
private fun MetricBar(label: String, fraction: Float) {
    val p = fraction.coerceIn(0f, 1f)
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White)
        LinearProgressIndicator(
            progress = { p },
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            color = if (p >= 1f) Color(0xFFFF5252) else if (p >= 0.6f) Color(0xFFFFD740) else Color(0xFF69F0AE),
            trackColor = Color(0x44FFFFFF),
        )
    }
}

@Composable
private fun ExplainCard() {
    var open by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0x33000000)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.monitor_explain_title), style = MaterialTheme.typography.titleMedium, color = Color.White)
                TextButton(onClick = { open = !open }) { Text(if (open) "▲" else "▼", color = Color.White) }
            }
            if (open) Text(stringResource(R.string.monitor_explain_body), style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
    }
}
