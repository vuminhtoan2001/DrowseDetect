package com.awakedrive.drowsedetect.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.service.TripSummary
import com.awakedrive.drowsedetect.ui.components.GlassCard
import com.awakedrive.drowsedetect.ui.components.GradientButton
import com.awakedrive.drowsedetect.ui.components.IconBadge
import com.awakedrive.drowsedetect.ui.components.StatTile
import com.awakedrive.drowsedetect.ui.theme.BrandCyan
import com.awakedrive.drowsedetect.ui.theme.StateAmber
import com.awakedrive.drowsedetect.ui.theme.StateGreen
import com.awakedrive.drowsedetect.ui.theme.StateRed
import com.awakedrive.drowsedetect.ui.theme.TextSecondary

/** Tổng kết sau khi kết thúc chuyến: kết luận một câu, vài con số dễ hiểu, biểu đồ và lời khuyên. */
@Composable
internal fun SummaryScreen(sm: TripSummary, onDone: () -> Unit) {
    val safe = sm.warningCount == 0 && sm.dangerCount == 0
    val accent = when {
        sm.dangerCount > 0 -> StateRed
        sm.warningCount > 0 -> StateAmber
        else -> StateGreen
    }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.sum_title), style = MaterialTheme.typography.headlineSmall)

        GlassCard(Modifier.fillMaxWidth(), borderColor = accent.copy(alpha = 0.8f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(if (safe) Icons.Filled.CheckCircle else Icons.Filled.Warning, accent, size = 56.dp)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(
                        if (safe) stringResource(R.string.sum_safe_title)
                        else stringResource(R.string.sum_alerts_title, sm.warningCount + sm.dangerCount),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(if (safe) R.string.sum_safe_sub else R.string.sum_alerts_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(stringResource(R.string.sum_duration), formatDuration(sm.durationMs), Modifier.weight(1.3f))
            StatTile(stringResource(R.string.sum_warnings), "${sm.warningCount}", Modifier.weight(1f))
            StatTile(stringResource(R.string.sum_dangers), "${sm.dangerCount}", Modifier.weight(1f))
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sum_chart), style = MaterialTheme.typography.titleMedium)
            if (sm.fatigueSamples.size < 2) {
                Text(
                    stringResource(R.string.sum_chart_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                FatigueChart(sm.fatigueSamples, Modifier.fillMaxWidth().height(140.dp).padding(top = 12.dp))
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Filled.Lightbulb, StateAmber, size = 40.dp)
                Text(
                    stringResource(
                        when {
                            sm.dangerCount > 0 -> R.string.sum_tip_danger
                            sm.warningCount > 0 -> R.string.sum_tip_warn
                            else -> R.string.sum_tip_safe
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }

        GradientButton(stringResource(R.string.sum_done), onDone, Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp))
    }
}

/** Đường biểu diễn mức mệt mỏi (0..1) theo thời gian, nền chia 3 vùng thấp/vừa/cao. */
@Composable
private fun FatigueChart(samples: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // vạch chia vùng: 0.35 và 0.7
        listOf(0.35f, 0.7f).forEach { v ->
            val y = h - v * h
            drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
        }
        val n = samples.size
        fun pt(i: Int) = Offset(w * i / (n - 1), h - samples[i].coerceIn(0f, 1f) * h)
        val line = Path().apply {
            moveTo(pt(0).x, pt(0).y)
            for (i in 1 until n) lineTo(pt(i).x, pt(i).y)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(BrandCyan.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, BrandCyan, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}
