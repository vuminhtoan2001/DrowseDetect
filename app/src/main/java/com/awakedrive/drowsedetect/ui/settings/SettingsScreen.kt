package com.awakedrive.drowsedetect.ui.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import com.awakedrive.drowsedetect.ui.components.IconBadge
import com.awakedrive.drowsedetect.ui.theme.Navy900
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awakedrive.drowsedetect.BuildConfig
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.alert.AlarmSound
import com.awakedrive.drowsedetect.alert.AlertManager
import com.awakedrive.drowsedetect.alert.AlertSettings
import com.awakedrive.drowsedetect.alert.AlertSettingsStore
import com.awakedrive.drowsedetect.detection.Sensitivity
import com.awakedrive.drowsedetect.service.BaselineStore
import com.awakedrive.drowsedetect.ui.components.GlassCard
import com.awakedrive.drowsedetect.ui.components.GradientButton
import com.awakedrive.drowsedetect.ui.components.OutlineButton
import com.awakedrive.drowsedetect.ui.components.ScreenBackground
import com.awakedrive.drowsedetect.ui.components.StatusPill
import com.awakedrive.drowsedetect.ui.components.StyledSlider
import com.awakedrive.drowsedetect.ui.theme.BrandBlue
import com.awakedrive.drowsedetect.ui.theme.BrandCyan
import com.awakedrive.drowsedetect.ui.theme.Navy600
import com.awakedrive.drowsedetect.ui.theme.StateAmber
import com.awakedrive.drowsedetect.ui.theme.StateGreen
import com.awakedrive.drowsedetect.ui.theme.StateRed
import com.awakedrive.drowsedetect.ui.theme.TextSecondary

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var s by remember { mutableStateOf(AlertSettingsStore.load(context)) }
    fun update(n: AlertSettings) {
        s = n
        AlertSettingsStore.save(context, n)
    }

    // Dùng chính AlertManager của app để nghe thử; luôn đọc cài đặt mới nhất mỗi lần phát.
    val latest = remember { arrayOf(s) }
    latest[0] = s
    val alert = remember { AlertManager(context) { latest[0] } }
    DisposableEffect(Unit) { onDispose { alert.stopTest(); alert.release() } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = r.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            update(s.copy(alarmSound = AlarmSound.SYSTEM, systemToneUri = uri?.toString()))
        }
    }
    val toneTitle = remember(s.systemToneUri) {
        runCatching {
            val uri = s.systemToneUri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        }.getOrNull() ?: context.getString(R.string.settings_default_tone)
    }

    var canDraw by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    // Người dùng cấp quyền ở màn hệ thống rồi quay lại: cập nhật trạng thái
    LifecycleResumeEffect(Unit) {
        canDraw = Settings.canDrawOverlays(context)
        onPauseOrDispose { }
    }

    ScreenBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back), tint = Color.White)
                }
                Text(
                    stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ================= PHÁT HIỆN =================
                SectionHeader(Icons.Filled.Visibility, R.string.settings_section_detect)
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(stringResource(R.string.settings_sensitivity), style = MaterialTheme.typography.titleMedium)
                        Segmented(
                            options = listOf(
                                R.string.settings_sens_low to Sensitivity.LOW,
                                R.string.settings_sens_med to Sensitivity.MEDIUM,
                                R.string.settings_sens_high to Sensitivity.HIGH,
                            ),
                            selected = s.sensitivity,
                            onSelect = { update(s.copy(sensitivity = it)) },
                        )
                        SubText(R.string.settings_sens_hint)

                        CardDivider()

                        Text(stringResource(R.string.settings_calib_section), style = MaterialTheme.typography.titleMedium)
                        SubText(R.string.settings_calib_hint)
                        OutlineButton(
                            stringResource(R.string.settings_recalibrate),
                            onClick = {
                                BaselineStore.clear(context)
                                Toast.makeText(context, R.string.settings_recalibrate_done, Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = StateRed,
                        )
                    }
                }

                // ================= ÂM THANH =================
                SectionHeader(Icons.Filled.VolumeUp, R.string.settings_section_sound)
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_alarm_sound), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(2.dp))
                        SoundOption(s.alarmSound == AlarmSound.SIREN, R.string.settings_sound_siren) { update(s.copy(alarmSound = AlarmSound.SIREN)) }
                        SoundOption(s.alarmSound == AlarmSound.BEEP, R.string.settings_sound_beep) { update(s.copy(alarmSound = AlarmSound.BEEP)) }
                        SoundOption(s.alarmSound == AlarmSound.SYSTEM, R.string.settings_sound_system) { update(s.copy(alarmSound = AlarmSound.SYSTEM)) }
                        OutlineButton(
                            stringResource(R.string.settings_pick_system, toneTitle),
                            onClick = {
                                picker.launch(
                                    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, s.systemToneUri?.let(Uri::parse)),
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )

                        CardDivider()

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.settings_volume_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusPill("${(s.volume * 100).toInt()}%", BrandCyan)
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VolumeDown, contentDescription = null, tint = TextSecondary)
                            StyledSlider(
                                value = s.volume,
                                onValueChange = { update(s.copy(volume = it)) },
                                valueRange = 0.2f..1f,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            )
                            Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White)
                        }
                        SubText(R.string.settings_volume_hint)
                    }
                }

                // ================= KIỂU CẢNH BÁO =================
                SectionHeader(Icons.Filled.NotificationsActive, R.string.settings_section_style)
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SwitchRow(Icons.Filled.NotificationsActive, R.string.settings_warning_beep, s.warningBeep) { update(s.copy(warningBeep = it)) }
                        CardDivider()
                        SwitchRow(Icons.Filled.RecordVoiceOver, R.string.settings_voice, s.voice) { update(s.copy(voice = it)) }
                        CardDivider()
                        SwitchRow(Icons.Filled.Vibration, R.string.settings_vibrate, s.vibrate) { update(s.copy(vibrate = it)) }
                    }
                }

                // ================= NGHE THỬ =================
                SectionHeader(Icons.Filled.PlayArrow, R.string.settings_section_test)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientButton(stringResource(R.string.settings_test_warning), { alert.testWarning() }, Modifier.fillMaxWidth())
                    GradientButton(
                        stringResource(R.string.settings_test_danger), { alert.testDanger() }, Modifier.fillMaxWidth(),
                        colors = listOf(Color(0xFFFF5A5F), Color(0xFFD7263D)),
                    )
                    OutlineButton(stringResource(R.string.settings_test_stop), { alert.stopTest() }, Modifier.fillMaxWidth())
                }

                // ================= HIỂN THỊ =================
                SectionHeader(Icons.Filled.Layers, R.string.settings_section_display)
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.overlay_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(end = 12.dp))
                            StatusPill(
                                stringResource(if (canDraw) R.string.overlay_on else R.string.overlay_off),
                                if (canDraw) StateGreen else StateAmber,
                            )
                        }
                        SubText(R.string.overlay_desc)
                        OutlineButton(
                            stringResource(if (canDraw) R.string.overlay_manage else R.string.overlay_grant),
                            onClick = { openOverlaySettings(context) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // ================= GIỚI THIỆU =================
                Column(
                    Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.settings_about_text, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.disclaimer_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Tiêu đề nhóm: icon + chữ màu nhấn, cách xa nhóm trước để dễ phân vùng. */
@Composable
private fun SectionHeader(icon: ImageVector, label: Int) {
    Row(Modifier.padding(start = 4.dp, top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(20.dp))
        Text(
            stringResource(label),
            style = MaterialTheme.typography.titleMedium,
            color = BrandCyan,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Chữ phụ: cỡ 13sp, màu sáng hơn trước, giãn dòng thoáng. */
@Composable
private fun SubText(label: Int) {
    Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
}

@Composable
private fun CardDivider() {
    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
}

@Composable
private fun <T> Segmented(
    options: List<Pair<Int, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Navy900.copy(alpha = 0.6f)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (label, value) ->
            val on = value == selected
            Text(
                stringResource(label),
                style = MaterialTheme.typography.labelLarge,
                color = if (on) Color.White else TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) BrandBlue else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(value) }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

/** Mỗi lựa chọn âm báo là một ô riêng, ô đang chọn có viền sáng. */
@Composable
private fun SoundOption(selected: Boolean, label: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) BrandBlue.copy(alpha = 0.22f) else Navy600.copy(alpha = 0.35f))
            .border(1.5.dp, if (selected) BrandCyan else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = BrandCyan, unselectedColor = TextSecondary),
        )
        Text(stringResource(label), modifier = Modifier.padding(start = 14.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, BrandCyan, size = 40.dp)
        Text(
            stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandBlue,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Navy600,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** Mở màn hệ thống cấp quyền "Hiển thị trên ứng dụng khác" cho app này. */
internal fun openOverlaySettings(context: android.content.Context) {
    context.startActivity(
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
