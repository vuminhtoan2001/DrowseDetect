package com.awakedrive.drowsedetect.service

import android.content.Context
import com.awakedrive.drowsedetect.detection.Baseline
import com.awakedrive.drowsedetect.detection.DrowsinessResult
import com.awakedrive.drowsedetect.detection.FaceFeatures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class MonitorPhase { IDLE, STARTING, CALIBRATING, MONITORING }

data class MonitorUiState(
    val phase: MonitorPhase = MonitorPhase.IDLE,
    val calibrationProgress: Float = 0f,
    val baseline: Baseline? = null,
    val result: DrowsinessResult? = null,
    val features: FaceFeatures? = null,
    val fps: Float = 0f,
    val inferMs: Long = 0L,
    val usingGpu: Boolean = false,
    /** SystemClock.elapsedRealtime() lúc bắt đầu giám sát (sau hiệu chuẩn); 0 nếu chưa. */
    val monitoringStartMs: Long = 0L,
    val warningCount: Int = 0,
    val dangerCount: Int = 0,
    /** Lỗi/ghi chú gần nhất (calibration thất bại, camera bị chiếm, không được phép chạy nền...). */
    val message: String? = null,
)

/** Tổng kết 1 chuyến, hiển thị sau khi dừng. */
data class TripSummary(
    val durationMs: Long,
    val warningCount: Int,
    val dangerCount: Int,
    /** Mức mệt mỏi 0..1 lấy mẫu định kỳ, để vẽ biểu đồ. */
    val fatigueSamples: List<Float>,
)

/** Cầu nối service -> UI (cùng process). */
object MonitorStateHolder {
    private val _state = MutableStateFlow(MonitorUiState())
    val state = _state.asStateFlow()

    /** Preview use case do service sở hữu; UI gắn/gỡ SurfaceProvider khi đang hiển thị. */
    val preview = MutableStateFlow<androidx.camera.core.Preview?>(null)

    /** Tổng kết chuyến gần nhất, UI hiển thị rồi gọi [clearSummary]. */
    val lastSummary = MutableStateFlow<TripSummary?>(null)

    fun clearSummary() { lastSummary.value = null }

    fun update(block: (MonitorUiState) -> MonitorUiState) = _state.update(block)
}

/** Lưu baseline đã calibrate (SharedPreferences là đủ cho 3 số). */
object BaselineStore {
    private const val PREFS = "baseline"

    fun load(context: Context): Baseline? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("ear")) return null
        return Baseline(p.getFloat("ear", 0f), p.getFloat("mar", 0f), p.getFloat("pitch", 0f))
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun save(context: Context, b: Baseline) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("ear", b.ear).putFloat("mar", b.mar).putFloat("pitch", b.pitchDeg).apply()
    }
}
