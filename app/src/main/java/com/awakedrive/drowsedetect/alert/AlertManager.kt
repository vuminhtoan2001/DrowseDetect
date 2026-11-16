package com.awakedrive.drowsedetect.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import com.awakedrive.drowsedetect.detection.DrowsinessState
import java.util.Locale

/**
 * Cảnh báo: WARNING = beep + rung ngắn + TTS (≤ 1 lần/2 phút); DANGER = âm báo (còi/bíp/nhạc báo thức
 * hệ thống) lặp + rung lặp tới khi state hạ hoặc [acknowledge]. NO_FACE không phát gì.
 * [settings] được đọc mỗi lần phát nên đổi cài đặt có hiệu lực ngay. Gọi từ bất kỳ thread nào.
 */
class AlertManager(context: Context, private val settings: () -> AlertSettings) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    private val alarmAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsVietnamese = false
    private var lastTtsAt = -TTS_MIN_GAP_MS

    private var current = DrowsinessState.NORMAL
    private var acknowledged = false

    // Chỉ truy cập trên main thread
    private var sirenOn = false
    private var siren: ToneGenerator? = null
    private var ringtone: Ringtone? = null

    private val toneLoop = object : Runnable {
        override fun run() {
            if (!sirenOn) return
            if (settings().alarmSound == AlarmSound.BEEP) {
                siren?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
                main.postDelayed(this, 450)
            } else {
                siren?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 600)
                main.postDelayed(this, 900)
            }
        }
    }

    init {
        main.post {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val t = tts ?: return@TextToSpeech
                    val vi = t.setLanguage(Locale("vi", "VN"))
                    ttsVietnamese = vi != TextToSpeech.LANG_MISSING_DATA && vi != TextToSpeech.LANG_NOT_SUPPORTED
                    if (!ttsVietnamese) t.setLanguage(Locale.ENGLISH)
                    ttsReady = true
                }
            }
        }
    }

    /** Gọi mỗi khi có kết quả mới; chỉ hành động khi state đổi. */
    fun onState(state: DrowsinessState) {
        if (state == current) return
        current = state
        if (state != DrowsinessState.DANGER) acknowledged = false // "Tôi đã tỉnh" chỉ có hiệu lực cho đợt DANGER này
        main.post {
            when (state) {
                DrowsinessState.DANGER -> if (!acknowledged) startSiren()
                DrowsinessState.WARNING -> { stopSiren(); playWarning() }
                else -> stopSiren()
            }
        }
    }

    /** Người dùng bấm "Tôi đã tỉnh": tắt còi tới khi state rời DANGER rồi quay lại. */
    fun acknowledge() {
        acknowledged = true
        main.post { stopSiren() }
    }

    // ---- Nghe thử trong màn Cài đặt ----
    fun testWarning() { main.post { stopSiren(); playWarning(force = true) } }
    fun testDanger() { main.post { startSiren() } }
    fun stopTest() { main.post { stopSiren() } }

    private fun playWarning(force: Boolean = false) {
        val s = settings()
        if (s.warningBeep) {
            val t = ToneGenerator(AudioManager.STREAM_ALARM, (s.volume * 100).toInt())
            t.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            main.postDelayed({ t.release() }, 600)
        }
        if (s.vibrate) vibrate(longArrayOf(0, 300), -1)
        if (s.voice) speakWarning(force)
    }

    private fun startSiren() {
        if (sirenOn) return
        sirenOn = true
        val s = settings()
        var played = false
        if (s.alarmSound == AlarmSound.SYSTEM) {
            val uri = s.systemToneUri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val r = RingtoneManager.getRingtone(appContext, uri)
            if (r != null) {
                r.audioAttributes = alarmAttrs
                r.isLooping = true
                r.volume = s.volume
                r.play()
                ringtone = r
                played = true
            }
        }
        if (!played) {
            siren = ToneGenerator(AudioManager.STREAM_ALARM, (s.volume * 100).toInt())
            main.post(toneLoop)
        }
        if (s.vibrate) vibrate(longArrayOf(0, 600, 300), 0)
    }

    private fun stopSiren() {
        sirenOn = false
        main.removeCallbacks(toneLoop)
        siren?.let { it.stopTone(); it.release() }
        siren = null
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
    }

    private fun vibrate(pattern: LongArray, repeat: Int) {
        val v = vibrator ?: return
        // Rung từ service nền bị Android 10+ chặn trừ khi khai usage ALARM
        @Suppress("DEPRECATION")
        v.vibrate(VibrationEffect.createWaveform(pattern, repeat), alarmAttrs)
    }

    private fun speakWarning(force: Boolean) {
        val t = tts
        if (!ttsReady || t == null) return
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastTtsAt < TTS_MIN_GAP_MS) return
        lastTtsAt = now
        val text = if (ttsVietnamese) "Bạn có dấu hiệu mệt mỏi, hãy cân nhắc dừng nghỉ"
        else "You look tired. Please consider taking a break."
        val params = Bundle().apply { putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM) }
        t.speak(text, TextToSpeech.QUEUE_FLUSH, params, "warning")
    }

    fun release() {
        main.post {
            stopSiren()
            tts?.shutdown()
            tts = null
        }
    }

    private companion object {
        const val TTS_MIN_GAP_MS = 2 * 60_000L
    }
}
