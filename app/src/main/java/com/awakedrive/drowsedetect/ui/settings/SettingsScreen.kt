package com.awakedrive.drowsedetect.ui.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.alert.AlarmSound
import com.awakedrive.drowsedetect.alert.AlertManager
import com.awakedrive.drowsedetect.alert.AlertSettings
import com.awakedrive.drowsedetect.alert.AlertSettingsStore

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var s by remember { mutableStateOf(AlertSettingsStore.load(context)) }
    fun update(n: AlertSettings) {
        s = n
        AlertSettingsStore.save(context, n)
    }

    // Dùng chính AlertManager của app để nghe thử, đọc cài đặt mới nhất mỗi lần phát.
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

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                }
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.settings_alarm_sound), style = MaterialTheme.typography.titleMedium)
                SoundOption(s.alarmSound == AlarmSound.SIREN, R.string.settings_sound_siren) { update(s.copy(alarmSound = AlarmSound.SIREN)) }
                SoundOption(s.alarmSound == AlarmSound.BEEP, R.string.settings_sound_beep) { update(s.copy(alarmSound = AlarmSound.BEEP)) }
                SoundOption(s.alarmSound == AlarmSound.SYSTEM, R.string.settings_sound_system) { update(s.copy(alarmSound = AlarmSound.SYSTEM)) }
                OutlinedButton(
                    onClick = {
                        picker.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, s.systemToneUri?.let(Uri::parse)),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.settings_pick_system, toneTitle)) }

                Text(
                    stringResource(R.string.settings_volume, (s.volume * 100).toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Slider(value = s.volume, onValueChange = { update(s.copy(volume = it)) }, valueRange = 0.2f..1f)
                Text(stringResource(R.string.settings_volume_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                SwitchRow(R.string.settings_warning_beep, s.warningBeep) { update(s.copy(warningBeep = it)) }
                SwitchRow(R.string.settings_voice, s.voice) { update(s.copy(voice = it)) }
                SwitchRow(R.string.settings_vibrate, s.vibrate) { update(s.copy(vibrate = it)) }

                Button(onClick = { alert.testWarning() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(stringResource(R.string.settings_test_warning))
                }
                Button(onClick = { alert.testDanger() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_test_danger))
                }
                OutlinedButton(onClick = { alert.stopTest() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_test_stop))
                }
            }
        }
    }
}

@Composable
private fun SoundOption(selected: Boolean, label: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(stringResource(label), modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SwitchRow(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
