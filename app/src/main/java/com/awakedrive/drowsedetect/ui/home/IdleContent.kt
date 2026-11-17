package com.awakedrive.drowsedetect.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.ui.components.GlassCard
import com.awakedrive.drowsedetect.ui.components.GlassIconButton
import com.awakedrive.drowsedetect.ui.components.GradientButton
import com.awakedrive.drowsedetect.ui.theme.BrandBlue
import com.awakedrive.drowsedetect.ui.theme.BrandCyan
import com.awakedrive.drowsedetect.ui.theme.StateRed
import com.awakedrive.drowsedetect.ui.theme.TextSecondary

/** Màn Sẵn sàng: nền ảnh xa lộ ban đêm, vòng sáng + khiên, tiêu đề, nút Bắt đầu nổi. Phần dưới để chừa cho chiếc xe. */
@Composable
internal fun IdleContent(
    message: String?,
    hasBaseline: Boolean,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        // Ảnh cao hơn màn: neo đáy để luôn thấy chiếc xe, cắt bớt phần trời phía trên
        Image(
            painter = painterResource(R.drawable.bg_home),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
        )
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val carZone = maxHeight * 0.26f
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    GlassIconButton(
                        Icons.Filled.Settings,
                        stringResource(R.string.settings_open),
                        onOpenSettings,
                    )
                }
                Spacer(Modifier.weight(0.8f))
                ShieldRing()
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(R.string.home_ready_title),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.home_ready_sub),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (message != null) {
                    GlassCard(Modifier.fillMaxWidth().padding(top = 16.dp), borderColor = StateRed.copy(alpha = 0.7f)) {
                        Text(message, color = ErrorText, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(36.dp))
                GradientButton(
                    text = stringResource(if (hasBaseline) R.string.home_start else R.string.home_start_first),
                    onClick = onStart,
                    icon = Icons.Filled.PlayArrow,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(carZone))
            }
        }
    }
}

/** Vòng sáng neon có cung sáng chạy chậm quanh khiên bảo vệ có dấu tích. */
@Composable
private fun ShieldRing() {
    val angle by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeat(),
        label = "angle",
    )
    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = 100.dp.toPx()
            val topLeft = Offset(c.x - r, c.y - r)
            val box = Size(r * 2, r * 2)

            // vòng mờ lan ra ngoài
            drawCircle(BrandBlue.copy(alpha = 0.10f), radius = r + 18.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            drawCircle(BrandBlue.copy(alpha = 0.06f), radius = r + 32.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            // quầng sáng: nhiều nét chồng, càng ngoài càng mờ
            for (i in 5 downTo 1) {
                drawCircle(BrandCyan.copy(alpha = 0.045f * (6 - i)), radius = r, style = Stroke((6 + i * 7).dp.toPx()))
            }
            // nền trong của vòng
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF0B2A66), Color(0xFF040C24)), center = c, radius = r),
                radius = r - 3.dp.toPx(),
            )
            // vòng chính
            drawCircle(
                Brush.sweepGradient(listOf(BrandBlue, BrandCyan, BrandBlue), center = c),
                radius = r,
                style = Stroke(6.dp.toPx()),
            )
            // cung sáng chạy quanh
            rotate(angle, pivot = c) {
                drawArc(
                    Color.White.copy(alpha = 0.95f), startAngle = -100f, sweepAngle = 60f, useCenter = false,
                    topLeft = topLeft, size = box, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        // khiên + dấu tích
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = Color(0xFF2F8CFF), modifier = Modifier.size(104.dp))
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
        }
    }
}

private fun infiniteRepeat() = infiniteRepeatable<Float>(tween(7000, easing = LinearEasing), RepeatMode.Restart)
