package com.awakedrive.drowsedetect.camera

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.awakedrive.drowsedetect.detection.FaceFeatures
import com.awakedrive.drowsedetect.detection.FeatureExtractor
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * Bọc MediaPipe FaceLandmarker (LIVE_STREAM). [onFeatures] được gọi trên thread của MediaPipe,
 * tuần tự từng frame; inferMs = độ trễ từ lúc gửi frame tới lúc có kết quả.
 */
class FaceLandmarkerHelper(
    context: Context,
    private val onFeatures: (FaceFeatures, inferMs: Long) -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    private var landmarker: FaceLandmarker? = null
    private var lastTs = -1L
    var usingGpu = false
        private set

    init {
        landmarker = try {
            build(context, Delegate.GPU).also { usingGpu = true }
        } catch (e: Throwable) {
            Log.w(TAG, "GPU delegate lỗi, chuyển CPU: ${e.message}")
            build(context, Delegate.CPU)
        }
    }

    private fun build(context: Context, delegate: Delegate): FaceLandmarker {
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder().setModelAssetPath(MODEL_ASSET).setDelegate(delegate).build(),
            )
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setOutputFaceBlendshapes(true)
            .setOutputFacialTransformationMatrixes(true)
            .setResultListener { result, input -> handle(result, input.width, input.height) }
            .setErrorListener { e -> onError(e) }
            .build()
        return FaceLandmarker.createFromOptions(context, options)
    }

    /** Gửi 1 frame; timestamp tăng dần nghiêm ngặt. */
    @Synchronized
    fun detectAsync(bitmap: Bitmap) {
        val lm = landmarker ?: return
        var ts = SystemClock.uptimeMillis()
        if (ts <= lastTs) ts = lastTs + 1
        lastTs = ts
        lm.detectAsync(BitmapImageBuilder(bitmap).build(), ts)
    }

    private fun handle(result: FaceLandmarkerResult, w: Int, h: Int) {
        val ts = result.timestampMs()
        val inferMs = SystemClock.uptimeMillis() - ts
        val faces = result.faceLandmarks()
        if (faces.isEmpty()) {
            onFeatures(FaceFeatures.noFace(ts), inferMs)
            return
        }
        val lms = faces[0]
        val xs = FloatArray(lms.size) { lms[it].x() }
        val ys = FloatArray(lms.size) { lms[it].y() }
        val blend = HashMap<String, Float>()
        result.faceBlendshapes().ifPresent { list ->
            list.firstOrNull()?.forEach { blend[it.categoryName()] = it.score() }
        }
        var matrix: FloatArray? = null
        result.facialTransformationMatrixes().ifPresent { list -> matrix = list.firstOrNull() }
        onFeatures(FeatureExtractor.extract(ts, xs, ys, w, h, blend, matrix), inferMs)
    }

    @Synchronized
    fun close() {
        landmarker?.close()
        landmarker = null
    }

    companion object {
        private const val TAG = "FaceLandmarkerHelper"
        const val MODEL_ASSET = "face_landmarker.task"
    }
}
