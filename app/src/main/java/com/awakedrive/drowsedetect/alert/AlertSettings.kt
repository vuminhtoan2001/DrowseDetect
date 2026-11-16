package com.awakedrive.drowsedetect.alert

import android.content.Context

enum class AlarmSound { SIREN, BEEP, SYSTEM }

data class AlertSettings(
    val alarmSound: AlarmSound = AlarmSound.SIREN,
    /** Uri nhạc báo thức hệ thống người dùng chọn; null = nhạc báo thức mặc định của máy. */
    val systemToneUri: String? = null,
    /** 0.2..1.0 */
    val volume: Float = 1f,
    val warningBeep: Boolean = true,
    val voice: Boolean = true,
    val vibrate: Boolean = true,
)

/** SharedPreferences: đọc đồng bộ được từ service lẫn UI. */
object AlertSettingsStore {
    private const val PREFS = "alert_settings"

    fun load(context: Context): AlertSettings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val d = AlertSettings()
        return AlertSettings(
            alarmSound = runCatching { AlarmSound.valueOf(p.getString("sound", null) ?: "") }.getOrDefault(d.alarmSound),
            systemToneUri = p.getString("tone_uri", null),
            volume = p.getFloat("volume", d.volume).coerceIn(0.2f, 1f),
            warningBeep = p.getBoolean("warning_beep", d.warningBeep),
            voice = p.getBoolean("voice", d.voice),
            vibrate = p.getBoolean("vibrate", d.vibrate),
        )
    }

    fun save(context: Context, s: AlertSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("sound", s.alarmSound.name)
            .putString("tone_uri", s.systemToneUri)
            .putFloat("volume", s.volume)
            .putBoolean("warning_beep", s.warningBeep)
            .putBoolean("voice", s.voice)
            .putBoolean("vibrate", s.vibrate)
            .apply()
    }
}
