package com.awakedrive.drowsedetect.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.awakedrive.drowsedetect.MainActivity
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.alert.AlertManager
import com.awakedrive.drowsedetect.alert.AlertSettingsStore
import com.awakedrive.drowsedetect.alert.DangerOverlay
import com.awakedrive.drowsedetect.ui.alert.openRestStops
import com.awakedrive.drowsedetect.camera.FaceLandmarkerHelper
import com.awakedrive.drowsedetect.camera.FrameAnalyzer
import com.awakedrive.drowsedetect.detection.Baseline
import com.awakedrive.drowsedetect.detection.CalibrationResult
import com.awakedrive.drowsedetect.detection.Calibrator
import com.awakedrive.drowsedetect.detection.DetectionConfig
import com.awakedrive.drowsedetect.detection.DrowsinessAnalyzer
import com.awakedrive.drowsedetect.detection.DrowsinessState
import com.awakedrive.drowsedetect.detection.DrowsinessResult
import com.awakedrive.drowsedetect.detection.FaceFeatures
import com.awakedrive.drowsedetect.detection.fatigueLevel
import com.awakedrive.drowsedetect.detection.forSensitivity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Foreground service `camera`: giữ camera trước + MediaPipe chạy khi app xuống nền.
 *
 * Ràng buộc Android (while-in-use):
 *  - Chỉ được START khi app đang hiển thị (Activity resumed). Android 12+ chặn start FGS từ nền
 *    (ForegroundServiceStartNotAllowedException); Android 14+ kiểm tra thêm CAMERA đã cấp + FGS type camera.
 *  - Service đã start đúng cách thì giữ được quyền camera khi app xuống nền; KHÔNG được tự restart từ
 *    nền (nên START_NOT_STICKY), nếu bị kill người dùng phải mở app bấm Bắt đầu lại.
 *  - Nếu user thu hồi quyền CAMERA / app khác chiếm camera, CameraX báo qua cameraState -> hiển thị lỗi.
 */
class MonitorService : LifecycleService() {
    private var cfg = DetectionConfig()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var alert: AlertManager? = null
    private var helper: FaceLandmarkerHelper? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var started = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlay: DangerOverlay? = null
    @Volatile private var dangerAcked = false

    // Chỉ truy cập trên thread kết quả của MediaPipe sau khi bind camera.
    @Volatile private var calibrator: Calibrator? = null
    @Volatile private var analyzer: DrowsinessAnalyzer? = null
    private var lastFrameTs = 0L
    private var fps = 0f
    private var lastNotifState: DrowsinessState? = null

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> start(intent.getBooleanExtra(EXTRA_RECALIBRATE, false))
            ACTION_ACK -> acknowledgeDanger()
            ACTION_STOP -> stopSelf()
            else -> stopSelf()
        }
        return START_NOT_STICKY // không tự restart từ nền (sẽ bị chặn)
    }

    private fun start(recalibrate: Boolean) {
        if (started) return
        createChannel()
        // Phải gọi startForeground NGAY (≤ 5s sau startForegroundService), kể cả khi sẽ thất bại.
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, buildNotification(getString(R.string.monitor_notif_starting)),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA,
            )
        } catch (e: Exception) {
            // SecurityException (thiếu CAMERA / FGS camera) hoặc ForegroundServiceStartNotAllowedException (đang ở nền)
            Log.e(TAG, "startForeground thất bại", e)
            fail(getString(R.string.monitor_err_fgs, e.javaClass.simpleName))
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            fail(getString(R.string.monitor_err_permission))
            return
        }
        started = true
        cfg = DetectionConfig.forSensitivity(AlertSettingsStore.load(this).sensitivity)

        val saved = BaselineStore.load(this)
        val needCalibrate = recalibrate || saved == null
        MonitorStateHolder.update {
            MonitorUiState(
                phase = if (needCalibrate) MonitorPhase.CALIBRATING else MonitorPhase.STARTING,
                baseline = saved,
            )
        }
        if (needCalibrate) calibrator = Calibrator(cfg) else analyzer = DrowsinessAnalyzer(cfg, saved ?: Baseline.DEFAULT)
        if (!needCalibrate) beginMonitoring()

        alert = AlertManager(this) { AlertSettingsStore.load(this) }
        overlay = DangerOverlay(this)
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "awakedrive:monitor").apply { acquire() }

        try {
            helper = FaceLandmarkerHelper(this, ::onFeatures) { e ->
                Log.e(TAG, "MediaPipe lỗi", e)
                MonitorStateHolder.update { it.copy(message = e.message) }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Không khởi tạo được FaceLandmarker", e)
            fail(getString(R.string.monitor_err_model, e.message ?: ""))
            return
        }
        MonitorStateHolder.update { it.copy(usingGpu = helper?.usingGpu == true) }

        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                bindCamera(future.get())
            } catch (e: Exception) {
                Log.e(TAG, "Bind camera thất bại", e)
                fail(getString(R.string.monitor_err_camera, e.message ?: ""))
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera(provider: ProcessCameraProvider) {
        val h = helper ?: return
        cameraProvider = provider
        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                    ).build(),
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { it.setAnalyzer(executor, FrameAnalyzer(h)) }
        provider.unbindAll()
        // Preview chỉ hiển thị khi UI gắn SurfaceProvider (app đang mở); ở nền nó không có surface,
        // ImageAnalysis vẫn chạy. Không vẽ gì lên preview.
        val preview = Preview.Builder().build()
        val camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
        MonitorStateHolder.preview.value = preview
        camera.cameraInfo.cameraState.observe(this) { st ->
            val err = st.error
            when {
                err != null -> {
                    Log.w(TAG, "Camera error code=${err.code}")
                    MonitorStateHolder.update { it.copy(message = getString(R.string.monitor_err_camera, "code ${err.code}")) }
                }
                st.type == CameraState.Type.OPEN -> MonitorStateHolder.update { it.copy(message = null) }
                else -> Unit
            }
        }
    }

    /** Chạy trên thread MediaPipe, tuần tự. */
    private fun onFeatures(f: FaceFeatures, inferMs: Long) {
        if (lastFrameTs != 0L && f.timestampMs > lastFrameTs) {
            val inst = 1000f / (f.timestampMs - lastFrameTs)
            fps = if (fps == 0f) inst else fps * 0.9f + inst * 0.1f
        }
        lastFrameTs = f.timestampMs

        val cal = calibrator
        if (cal != null) {
            cal.add(f)
            val progress = cal.progress(f.timestampMs)
            if (!cal.isDone(f.timestampMs)) {
                MonitorStateHolder.update { it.copy(features = f, fps = fps, inferMs = inferMs, calibrationProgress = progress) }
                return
            }
            calibrator = null
            when (val r = cal.result()) {
                is CalibrationResult.Success -> {
                    BaselineStore.save(this, r.baseline)
                    analyzer = DrowsinessAnalyzer(cfg, r.baseline)
                    MonitorStateHolder.update { it.copy(baseline = r.baseline, calibrationProgress = 1f, message = null) }
                    beginMonitoring()
                }
                is CalibrationResult.Failure -> {
                    val msg = getString(
                        if (r.reason == CalibrationResult.Reason.NOT_ENOUGH_FACE) R.string.monitor_err_calib_face
                        else R.string.monitor_err_calib_eyes,
                    )
                    // stopSelf từ main để chắc chắn; giữ message cho UI
                    ContextCompat.getMainExecutor(this).execute { fail(msg) }
                }
            }
            return
        }

        val a = analyzer ?: return
        val res = a.process(f)
        alert?.onState(res.state)
        trackTrip(res, f.timestampMs)
        updateOverlay(res)
        MonitorStateHolder.update {
            it.copy(result = res, features = f, fps = fps, inferMs = inferMs, warningCount = warnings, dangerCount = dangers, dangerAcked = dangerAcked)
        }
        if (res.state != lastNotifState) {
            lastNotifState = res.state
            getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(stateText(res.state)))
        }
    }

    // ---- Tổng kết chuyến (ghi từ thread MediaPipe, đọc ở onDestroy) ----
    private val tripLock = Any()
    private var tripStartMs = 0L
    private var warnings = 0
    private var dangers = 0
    private var prevState = DrowsinessState.NORMAL
    private var lastSampleTs = 0L
    private val fatigueSamples = ArrayList<Float>()

    private fun acknowledgeDanger() {
        dangerAcked = true
        alert?.acknowledge()
        MonitorStateHolder.update { it.copy(dangerAcked = true) }
        mainHandler.post { overlay?.hide() }
    }

    /**
     * Màn cảnh báo toàn màn hiện NGAY khi NGUY HIỂM nếu app đang ở nền và đã cấp quyền "hiển thị trên ứng dụng
     * khác"; tự đóng khi state rời DANGER (người dùng tỉnh lại) hoặc khi bấm "Tôi đã tỉnh". Khi app đang mở thì
     * màn trong app đảm nhiệm, overlay không hiện (và bị gỡ nếu đang hiện lúc user quay lại app).
     */
    private fun updateOverlay(res: DrowsinessResult) {
        if (res.state != DrowsinessState.DANGER) dangerAcked = false
        val should = res.state == DrowsinessState.DANGER && !dangerAcked &&
            !AppForeground.visible && Settings.canDrawOverlays(this)
        mainHandler.post {
            val o = overlay ?: return@post
            if (should && !o.isShowing) {
                wakeScreen()
                o.show(
                    res.reason,
                    onAwake = { acknowledgeDanger() },
                    onFindRest = {
                        acknowledgeDanger()
                        openRestStops(this, getString(R.string.rest_query))
                    },
                )
            } else if (!should && o.isShowing) {
                o.hide()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "awakedrive:alert",
        ).acquire(5_000L)
    }

    private fun beginMonitoring() {
        synchronized(tripLock) { tripStartMs = SystemClock.elapsedRealtime() }
        MonitorStateHolder.update { it.copy(phase = MonitorPhase.MONITORING, monitoringStartMs = tripStartMs) }
    }

    private fun trackTrip(res: DrowsinessResult, tsMs: Long) {
        synchronized(tripLock) {
            if (res.state != prevState) {
                if (res.state == DrowsinessState.WARNING && prevState.level < DrowsinessState.WARNING.level) warnings++
                if (res.state == DrowsinessState.DANGER) dangers++
                prevState = res.state
            }
            if (tsMs - lastSampleTs >= SAMPLE_EVERY_MS && res.state != DrowsinessState.NO_FACE) {
                lastSampleTs = tsMs
                fatigueSamples += fatigueLevel(res, cfg)
            }
        }
    }

    private fun stateText(s: DrowsinessState) = getString(
        when (s) {
            DrowsinessState.NORMAL -> R.string.monitor_state_normal
            DrowsinessState.WARNING -> R.string.monitor_state_warning
            DrowsinessState.DANGER -> R.string.monitor_state_danger
            DrowsinessState.NO_FACE -> R.string.monitor_state_no_face
        },
    )

    private fun fail(message: String) {
        MonitorStateHolder.update { MonitorUiState(phase = MonitorPhase.IDLE, message = message) }
        stopSelf()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        overlay?.hide()
        MonitorStateHolder.preview.value = null
        cameraProvider?.unbindAll()
        helper?.close()
        alert?.release()
        wakeLock?.takeIf { it.isHeld }?.release()
        executor.shutdown()
        calibrator = null
        analyzer = null
        synchronized(tripLock) {
            if (tripStartMs > 0) {
                val duration = SystemClock.elapsedRealtime() - tripStartMs
                if (duration >= MIN_TRIP_MS) {
                    MonitorStateHolder.lastSummary.value = TripSummary(duration, warnings, dangers, fatigueSamples.toList())
                }
            }
        }
        // Giữ message lỗi (nếu fail() vừa đặt), còn lại về IDLE
        MonitorStateHolder.update { MonitorUiState(phase = MonitorPhase.IDLE, baseline = it.baseline, message = it.message) }
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.monitor_channel_name), NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, MonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_app)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, getString(R.string.monitor_stop), stop)
            .build()
    }

    companion object {
        private const val TAG = "MonitorService"
        private const val CHANNEL_ID = "awake_drive_monitor"
        private const val NOTIF_ID = 1001
        private const val SAMPLE_EVERY_MS = 5_000L
        private const val MIN_TRIP_MS = 10_000L
        const val ACTION_START = "com.awakedrive.drowsedetect.action.START"
        const val ACTION_STOP = "com.awakedrive.drowsedetect.action.STOP"
        const val ACTION_ACK = "com.awakedrive.drowsedetect.action.ACK"
        const val EXTRA_RECALIBRATE = "recalibrate"

        /** Gọi từ UI khi app đang hiển thị và đã có quyền CAMERA. */
        fun start(context: Context, recalibrate: Boolean = false) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MonitorService::class.java).setAction(ACTION_START).putExtra(EXTRA_RECALIBRATE, recalibrate),
            )
        }

        fun stop(context: Context) {
            context.startService(Intent(context, MonitorService::class.java).setAction(ACTION_STOP))
        }

        fun acknowledge(context: Context) {
            context.startService(Intent(context, MonitorService::class.java).setAction(ACTION_ACK))
        }
    }
}
