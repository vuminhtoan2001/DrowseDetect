package com.awakedrive.drowsedetect.ui.alert

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.detection.AlertReason
import com.awakedrive.drowsedetect.ui.components.GradientButton
import com.awakedrive.drowsedetect.ui.components.OutlineButton
import com.awakedrive.drowsedetect.ui.theme.Navy900
import com.awakedrive.drowsedetect.ui.theme.StateRed

internal fun reasonText(reason: AlertReason?): Int? = when (reason) {
    AlertReason.MICROSLEEP -> R.string.alert_reason_microsleep
    AlertReason.PERCLOS -> R.string.alert_reason_perclos
    AlertReason.YAWN -> R.string.alert_reason_yawn
    AlertReason.HEAD_NOD -> R.string.alert_reason_nod
    null -> null
}

/**
 * Toàn màn đỏ nhấp nháy khi NGUY HIỂM, kèm 2 hành động rõ ràng. Dùng cả trong app lẫn trong cửa sổ
 * overlay (hiện trên app khác) nên không phụ thuộc Activity.
 */
@Composable
fun DangerAlertScreen(reason: AlertReason?, onAwake: () -> Unit, onFindRest: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "alpha",
    )
    CompositionLocalProvider(LocalContentColor provides Color.White) {
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
}

/** Mở app bản đồ tìm trạm dừng chân; NEW_TASK để gọi được từ cả Activity lẫn Service. */
fun openRestStops(context: Context, query: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        // máy không có app bản đồ: bỏ qua, người dùng vẫn tắt được cảnh báo bằng nút "Tôi đã tỉnh"
    }
}
