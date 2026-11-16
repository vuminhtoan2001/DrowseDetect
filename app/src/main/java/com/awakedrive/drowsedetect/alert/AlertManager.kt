package com.awakedrive.drowsedetect.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
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
 * Cảnh báo: WARNING = beep + rung ngắn + TTS (≤ 1 lần/2 phút); DANGER = còi lặp + rung lặp tới khi
 * state hạ hoặc [acknowledge]. NO_FACE không phát gì. Gọi từ bất kỳ thread nào (việc phát chạy trên main).
 */
class AlertManager(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsVietnamese = false
    private var lastTtsAt = -TTS_MIN_GAP_MS

    @Volatile var vibrateEnabled = true
    @Volatile var ttsEnabled = true

    private var current = DrowsinessState.NORMAL
    private var acknowledged = false
    private var sirenOn = false

    private val sirenLoop = object : Runnable {
        override fun run() {
            if (!sirenOn) return
            tone.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 600)
            main.postDelayed(this, 900)
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
        val prev = current
        current = state
        if (state != DrowsinessState.DANGER) acknowledged = false // "Tôi đã tỉnh" chỉ có hiệu lực cho đợt DANGER này
        main.post { apply(prev, state) }
    }

    /** Người dùng bấm "Tôi đã tỉnh": tắt còi tới khi state rời DANGER rồi quay lại. */
    fun acknowledge() {
        acknowledged = true
        main.post { stopSiren() }
    }

    private fun apply(prev: DrowsinessState, state: DrowsinessState) {
        when (state) {
            DrowsinessState.DANGER -> if (!acknowledged) startSiren()
            DrowsinessState.WARNING -> {
                stopSiren()
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                vibrate(longArrayOf(0, 300), -1)
                speakWarning()
            }
            else -> stopSiren()
        }
    }

    private fun startSiren() {
        if (sirenOn) return
        sirenOn = true
        main.post(sirenLoop)
        vibrate(longArrayOf(0, 600, 300), 0)
    }

    private fun stopSiren() {
        sirenOn = false
        main.removeCallbacks(sirenLoop)
        tone.stopTone()
        vibrator?.cancel()
    }

    private fun vibrate(pattern: LongArray, repeat: Int) {
        if (!vibrateEnabled) return
        val v = vibrator ?: return
        val effect = VibrationEffect.createWaveform(pattern, repeat)
        // Rung từ service nền (app không ở trước) bị Android 10+ chặn trừ khi khai usage ALARM
        @Suppress("DEPRECATION")
        v.vibrate(
            effect,
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
        )
    }

    private fun speakWarning() {
        val t = tts
        if (!ttsEnabled || !ttsReady || t == null) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastTtsAt < TTS_MIN_GAP_MS) return
        lastTtsAt = now
        val text = if (ttsVietnamese) "Bạn có dấu hiệu mệt mỏi, hãy cân nhắc dừng nghỉ"
        else "You look tired. Please consider taking a break."
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "warning")
    }

    fun release() {
        main.post {
            stopSiren()
            tone.release()
            tts?.shutdown()
            tts = null
        }
    }

    private companion object {
        const val TTS_MIN_GAP_MS = 2 * 60_000L
    }
}
