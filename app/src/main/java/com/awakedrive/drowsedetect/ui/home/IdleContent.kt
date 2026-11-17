package com.awakedrive.drowsedetect.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

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

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier



import androidx.compose.ui.graphics.Color



import androidx.compose.ui.graphics.graphicsLayer
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

/** Ảnh khiên + vòng neon (image_check) có hiệu ứng "thở": phóng nhẹ và sáng dần rồi dịu lại. */
@Composable
private fun ShieldRing() {
    val t = rememberInfiniteTransition(label = "breath")
    val scale by t.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )
    val alpha by t.animateFloat(
        initialValue = 0.88f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Image(
        painter = painterResource(R.drawable.image_check),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(270.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        },
    )
}
