package com.awakedrive.drowsedetect.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.detection.DetectionConfig
import com.awakedrive.drowsedetect.service.MonitorService
import com.awakedrive.drowsedetect.service.MonitorUiState
import com.awakedrive.drowsedetect.ui.components.CameraPreview
import com.awakedrive.drowsedetect.ui.components.OutlineButton
import com.awakedrive.drowsedetect.ui.theme.BrandCyan
import com.awakedrive.drowsedetect.ui.theme.Navy600
import com.awakedrive.drowsedetect.ui.theme.TextSecondary
import kotlin.math.ceil

/** 15 giây đầu: app "làm quen" với gương mặt người dùng; hiển thị vòng tiến trình quanh khung camera tròn. */
@Composable
internal fun CalibrationContent(ui: MonitorUiState) {
    val context = LocalContext.current
    val totalSec = (DetectionConfig().calibrationDurationMs / 1000).toInt()
    val left = ceil((1f - ui.calibrationProgress) * totalSec).toInt().coerceIn(0, totalSec)

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.calib_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.calib_sub),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
        )
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            CameraPreview(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                shape = CircleShape,
            )
            CircularProgressIndicator(
                progress = { ui.calibrationProgress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 8.dp,
                color = BrandCyan,
                trackColor = Navy600,
            )
        }
        Text(
            stringResource(R.string.calib_seconds, left),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 24.dp),
        )
        OutlineButton(
            stringResource(R.string.calib_cancel),
            onClick = { MonitorService.stop(context) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
