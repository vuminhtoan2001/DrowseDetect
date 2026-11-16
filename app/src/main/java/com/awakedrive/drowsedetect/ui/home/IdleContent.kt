package com.awakedrive.drowsedetect.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.ui.components.GlassCard
import com.awakedrive.drowsedetect.ui.components.GradientButton
import com.awakedrive.drowsedetect.ui.components.IconBadge
import com.awakedrive.drowsedetect.ui.theme.BrandBlue
import com.awakedrive.drowsedetect.ui.theme.BrandCyan
import com.awakedrive.drowsedetect.ui.theme.Navy700
import com.awakedrive.drowsedetect.ui.theme.StateGreen
import com.awakedrive.drowsedetect.ui.theme.StateRed
import com.awakedrive.drowsedetect.ui.theme.TextSecondary

@Composable
internal fun IdleContent(
    message: String?,
    hasBaseline: Boolean,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Visibility, BrandCyan, size = 40.dp)
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_open), tint = Color.White)
            }
        }

        // Hero: vòng sáng + khiên bảo vệ
        Box(
            Modifier.padding(top = 12.dp).size(190.dp)
                .border(5.dp, Brush.sweepGradient(listOf(BrandBlue, BrandCyan, BrandBlue)), CircleShape)
                .padding(18.dp).clip(CircleShape).background(Navy700),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = StateGreen, modifier = Modifier.size(88.dp))
        }
        Text(stringResource(R.string.home_ready_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.home_ready_sub),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )

        if (message != null) {
            GlassCard(Modifier.fillMaxWidth(), borderColor = StateRed.copy(alpha = 0.7f)) {
                Text(message, color = ErrorText, style = MaterialTheme.typography.bodyMedium)
            }
        }

        GradientButton(
            text = stringResource(if (hasBaseline) R.string.home_start else R.string.home_start_first),
            onClick = onStart,
            icon = Icons.Filled.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            stringResource(R.string.tips_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TipCard(Icons.Filled.PhoneAndroid, R.string.tip_front, Modifier.weight(1f))
            TipCard(Icons.Filled.Visibility, R.string.tip_eye, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TipCard(Icons.Filled.LightMode, R.string.tip_light, Modifier.weight(1f))
            TipCard(Icons.Filled.VolumeUp, R.string.tip_sound, Modifier.weight(1f))
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Text(
                stringResource(R.string.home_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun TipCard(icon: ImageVector, label: Int, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, BrandCyan, size = 36.dp)
            Text(
                stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}
