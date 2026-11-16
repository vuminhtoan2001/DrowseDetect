# DrowsyGuard — Đặc tả xây dựng app cảnh báo buồn ngủ khi lái xe

> File này dành cho AI coding agent. Hãy đọc toàn bộ trước khi viết code, sau đó thực hiện lần lượt theo các **Phase** ở mục 9. Mỗi phase phải build được và chạy được trước khi sang phase tiếp theo. Nếu có điểm chưa rõ, chọn phương án đơn giản nhất phù hợp với đặc tả và ghi chú lại trong `NOTES.md`.

---

## 1. Mục tiêu

App Android viết bằng **Kotlin + Jetpack Compose**, dùng **camera trước** để theo dõi khuôn mặt tài xế theo thời gian thực, phát hiện dấu hiệu buồn ngủ (nhắm mắt lâu, PERCLOS cao, ngáp, gật đầu) và **phát cảnh báo** bằng âm thanh, rung và giọng nói.

Nguyên tắc bắt buộc:

- Toàn bộ xử lý **on-device**, không gửi ảnh/video lên server, không lưu ảnh khuôn mặt.
- Ưu tiên **độ tin cậy của cảnh báo** hơn hiệu ứng UI.
- App là công cụ **hỗ trợ**, không thay thế việc nghỉ ngơi; hiển thị disclaimer ở lần mở đầu tiên.

---

## 2. Tech stack

Dùng phiên bản **stable mới nhất** tại thời điểm build cho mọi thư viện dưới đây, quản lý bằng **Version Catalog** (`gradle/libs.versions.toml`).

| Thành phần | Thư viện |
|---|---|
| Ngôn ngữ | Kotlin, Coroutines, Flow |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Camera | CameraX: `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view` (hoặc `camera-compose` nếu ổn định) |
| Face landmark | MediaPipe Tasks Vision: `com.google.mediapipe:tasks-vision` |
| DI | Hilt (`hilt-android`, `hilt-navigation-compose`) |
| Lưu trữ | Room (lịch sử chuyến đi), DataStore Preferences (cài đặt) |
| Quyền | Accompanist Permissions hoặc `rememberLauncherForActivityResult` |
| Test | JUnit, kotlinx-coroutines-test, Turbine |

Cấu hình SDK:

- `minSdk = 26`
- `compileSdk = 36`, `targetSdk = 36`
- Java/Kotlin JVM target 17

Model MediaPipe: tải `face_landmarker.task` từ
`https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task`
và đặt vào `app/src/main/assets/face_landmarker.task`. Có thể thêm task Gradle tải tự động nếu file chưa tồn tại.

Package name mặc định: `com.example.drowsyguard` (sẽ đổi sau).

---

## 3. Kiến trúc

MVVM + Clean-ish, một module `app`, chia package theo tầng:

```
com.example.drowsyguard
├── DrowsyGuardApp.kt              // @HiltAndroidApp
├── MainActivity.kt                // setContent, keep screen on
├── di/
│   └── AppModule.kt
├── camera/
│   ├── CameraManager.kt           // bind Preview + ImageAnalysis
│   └── FrameAnalyzer.kt           // ImageAnalysis.Analyzer -> MediaPipe
├── detection/
│   ├── FaceLandmarkerHelper.kt    // khởi tạo + detectAsync
│   ├── FaceFeatures.kt            // data class đặc trưng mỗi frame
│   ├── FeatureExtractor.kt        // EAR, MAR, head pose, blendshapes
│   ├── Calibrator.kt              // baseline theo người dùng
│   ├── DrowsinessAnalyzer.kt      // PERCLOS, microsleep, ngáp, gật đầu
│   └── DrowsinessState.kt         // NORMAL / WARNING / DANGER / NO_FACE
├── alert/
│   └── AlertManager.kt            // âm thanh, rung, TTS
├── data/
│   ├── db/ (Room: TripEntity, AlertEventEntity, Dao, AppDatabase)
│   ├── settings/SettingsRepository.kt (DataStore)
│   └── TripRepository.kt
└── ui/
    ├── theme/
    ├── navigation/NavGraph.kt
    ├── onboarding/OnboardingScreen.kt   // disclaimer + xin quyền
    ├── monitor/MonitorScreen.kt         // màn hình chính khi lái
    ├── monitor/MonitorViewModel.kt
    ├── monitor/LandmarkOverlay.kt       // Canvas vẽ điểm mắt/miệng
    ├── calibration/CalibrationScreen.kt
    ├── history/HistoryScreen.kt
    └── settings/SettingsScreen.kt
```

Luồng dữ liệu:

```
CameraX ImageAnalysis (front, ~640x480, KEEP_ONLY_LATEST)
   -> FrameAnalyzer (ImageProxy -> Bitmap, xoay + lật gương)
   -> FaceLandmarkerHelper.detectAsync (LIVE_STREAM)
   -> FeatureExtractor -> FaceFeatures
   -> DrowsinessAnalyzer (cửa sổ thời gian, state machine)
   -> StateFlow<MonitorUiState> -> MonitorScreen
   -> AlertManager (khi state thay đổi)
```

Quy tắc:

- Phân tích chạy trên `Executors.newSingleThreadExecutor()`, **không** chạy trên main thread.
- Luôn `imageProxy.close()` trong `finally`.
- `DrowsinessAnalyzer` là **pure Kotlin**, không phụ thuộc Android, để unit test được (nhận timestamp từ ngoài vào, không gọi `System.currentTimeMillis()` bên trong).

---

## 4. Camera (CameraX)

- `CameraSelector.DEFAULT_FRONT_CAMERA`.
- `ImageAnalysis`: `setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)`, `setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)`, `ResolutionSelector` ưu tiên khoảng 640x480.
- Giới hạn xử lý khoảng **15 fps** (bỏ qua frame nếu chưa đủ ~66 ms từ frame trước).
- Chuyển frame: `imageProxy.toBitmap()`, xoay theo `imageInfo.rotationDegrees`, **lật ngang** vì là camera trước, rồi `BitmapImageBuilder(bitmap).build()`.
- Preview trong Compose: `PreviewView` bọc trong `AndroidView` (hoặc `CameraXViewfinder`), `scaleType = FILL_CENTER`.
- Bind với `LifecycleOwner` của màn hình Monitor; unbind khi rời màn hình.

---

## 5. MediaPipe Face Landmarker

```kotlin
val options = FaceLandmarker.FaceLandmarkerOptions.builder()
    .setBaseOptions(
        BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")
            .setDelegate(Delegate.GPU) // fallback CPU nếu lỗi khởi tạo
            .build()
    )
    .setRunningMode(RunningMode.LIVE_STREAM)
    .setNumFaces(1)
    .setMinFaceDetectionConfidence(0.5f)
    .setMinFacePresenceConfidence(0.5f)
    .setMinTrackingConfidence(0.5f)
    .setOutputFaceBlendshapes(true)
    .setOutputFacialTransformationMatrixes(true)
    .setResultListener { result, image -> onResult(result, image.width, image.height) }
    .setErrorListener { e -> onError(e) }
    .build()
```

- Gọi `detectAsync(mpImage, timestampMs)` với timestamp **tăng dần nghiêm ngặt** (dùng `SystemClock.uptimeMillis()`).
- Nếu khởi tạo GPU thất bại -> thử lại với `Delegate.CPU`, log lại.
- `close()` landmarker trong `onCleared()` của ViewModel / khi dừng giám sát.

---

## 6. Trích xuất đặc trưng (FeatureExtractor)

```kotlin
data class FaceFeatures(
    val timestampMs: Long,
    val faceDetected: Boolean,
    val earLeft: Float,
    val earRight: Float,
    val earAvg: Float,
    val blinkLeft: Float,     // blendshape eyeBlinkLeft (0..1)
    val blinkRight: Float,    // blendshape eyeBlinkRight (0..1)
    val mar: Float,
    val jawOpen: Float,       // blendshape jawOpen (0..1)
    val pitchDeg: Float,
    val yawDeg: Float,
    val rollDeg: Float,
)
```

### 6.1 EAR (Eye Aspect Ratio)

`EAR = (|p2 - p6| + |p3 - p5|) / (2 * |p1 - p4|)` — dùng tọa độ x, y đã nhân với width/height ảnh (không dùng tọa độ chuẩn hóa trực tiếp vì tỉ lệ khung hình sai).

Index landmark MediaPipe (thứ tự p1..p6):

- Mắt trái: `33, 160, 158, 133, 153, 144`
- Mắt phải: `362, 385, 387, 263, 373, 380`

### 6.2 MAR (Mouth Aspect Ratio)

`MAR = |p13 - p14| / |p78 - p308|` (môi trên trong 13, môi dưới trong 14, khóe miệng 78 và 308).

### 6.3 Head pose

Lấy từ `facialTransformationMatrixes` (ma trận 4x4), tính Euler angles pitch/yaw/roll (độ). Nếu không có, xấp xỉ pitch bằng tỉ lệ khoảng cách mũi (1) so với trán (10) và cằm (152).

### 6.4 Blendshapes

Đọc từ `result.faceBlendshapes()` theo `categoryName()`: `eyeBlinkLeft`, `eyeBlinkRight`, `jawOpen`.

### 6.5 Xác định "mắt nhắm" trong 1 frame

Mắt được coi là **nhắm** khi thỏa **một trong hai**:

- `earAvg < baselineEar * EYE_CLOSED_RATIO` (mặc định 0.7), hoặc
- `(blinkLeft + blinkRight) / 2 > BLINK_SCORE_THRESHOLD` (mặc định 0.5).

Nếu `yaw` vượt `MAX_YAW_DEG` (mặc định 35°) thì đánh dấu frame **không đáng tin** cho mắt (tài xế đang nhìn gương/quay đầu), không tính vào PERCLOS.

---

## 7. Phân tích buồn ngủ (DrowsinessAnalyzer)

### 7.1 Tham số mặc định (cho phép chỉnh trong Settings, lưu DataStore)

```kotlin
object DetectionConfig {
    const val CALIBRATION_DURATION_MS = 15_000L
    const val EYE_CLOSED_RATIO = 0.7f
    const val BLINK_SCORE_THRESHOLD = 0.5f
    const val MICROSLEEP_MS = 1_200L          // nhắm liên tục -> DANGER
    const val PERCLOS_WINDOW_MS = 60_000L
    const val PERCLOS_WARNING = 0.15f
    const val PERCLOS_DANGER = 0.25f
    const val YAWN_JAW_OPEN = 0.6f            // hoặc MAR > baselineMar * 2.5
    const val YAWN_MIN_MS = 2_000L
    const val YAWN_WINDOW_MS = 300_000L
    const val YAWN_COUNT_WARNING = 3
    const val NOD_PITCH_DROP_DEG = 15f
    const val NOD_MAX_MS = 1_500L
    const val NO_FACE_TIMEOUT_MS = 3_000L
    const val MAX_YAW_DEG = 35f
    const val STATE_DOWNGRADE_HOLD_MS = 5_000L // hysteresis
}
```

### 7.2 Calibration

- `Calibrator` thu thập `earAvg`, `mar`, `pitch` trong `CALIBRATION_DURATION_MS` khi người dùng nhìn thẳng, mắt mở bình thường.
- Baseline = **median** (không dùng mean, để loại các lần chớp mắt).
- Nếu không đủ frame có mặt (dưới 60%) -> báo lỗi, yêu cầu chỉnh lại vị trí điện thoại.
- Lưu baseline vào DataStore; cho phép calibrate lại từ Settings.

### 7.3 Các tín hiệu

1. **Microsleep**: thời gian mắt nhắm liên tục ≥ `MICROSLEEP_MS` -> `DANGER` ngay lập tức.
2. **PERCLOS**: tỉ lệ thời gian mắt nhắm trong cửa sổ trượt `PERCLOS_WINDOW_MS` (tính theo thời lượng, không theo số frame). ≥ `PERCLOS_WARNING` -> `WARNING`, ≥ `PERCLOS_DANGER` -> `DANGER`. Chỉ đánh giá khi cửa sổ đã có ít nhất 30 giây dữ liệu.
3. **Ngáp**: `jawOpen > YAWN_JAW_OPEN` kéo dài ≥ `YAWN_MIN_MS` tính là 1 lần ngáp. Số lần ngáp trong `YAWN_WINDOW_MS` ≥ `YAWN_COUNT_WARNING` -> `WARNING`.
4. **Gật đầu**: pitch giảm hơn `NOD_PITCH_DROP_DEG` so với baseline rồi hồi lại trong `NOD_MAX_MS` -> tính 1 lần gật; 2 lần trong 60 giây -> `WARNING`. Nếu pitch giảm kéo dài kèm mắt nhắm -> `DANGER`.
5. **Không thấy mặt**: quá `NO_FACE_TIMEOUT_MS` -> state `NO_FACE`, hiện hướng dẫn chỉnh camera (không phát còi).

### 7.4 State machine

```
NORMAL  <-> WARNING <-> DANGER
   \_________ NO_FACE _________/
```

- **Nâng cấp** state ngay lập tức.
- **Hạ cấp** chỉ khi điều kiện của state cao hơn đã không còn trong `STATE_DOWNGRADE_HOLD_MS` (tránh nhấp nháy).
- Output:

```kotlin
data class DrowsinessResult(
    val state: DrowsinessState,
    val reason: AlertReason?,      // MICROSLEEP, PERCLOS, YAWN, HEAD_NOD
    val perclos: Float,
    val eyesClosedMs: Long,
    val yawnCount: Int,
    val nodCount: Int,
)
```

---

## 8. Cảnh báo (AlertManager)

| State | Hành động |
|---|---|
| WARNING | 1 tiếng beep ngắn + rung ngắn + TTS "Bạn có dấu hiệu mệt mỏi, hãy cân nhắc dừng nghỉ" (TTS tối đa 1 lần / 2 phút) |
| DANGER | Còi lặp liên tục âm lượng lớn + rung pattern lặp + nền màn hình đỏ nhấp nháy, cho đến khi state hạ xuống hoặc người dùng chạm nút "Tôi đã tỉnh" |
| NO_FACE | Chỉ hiển thị banner trên UI |

Chi tiết kỹ thuật:

- Âm thanh dùng `SoundPool` với `AudioAttributes.USAGE_ALARM` + `CONTENT_TYPE_SONIFICATION`, file âm trong `res/raw/` (tự tạo file beep ngắn hoặc dùng `ToneGenerator` nếu chưa có asset).
- Rung: `VibratorManager` (API 31+) / `Vibrator`, dùng `VibrationEffect.createWaveform`.
- TTS: `TextToSpeech` với `Locale("vi", "VN")`, fallback tiếng Anh nếu thiết bị không có giọng Việt.
- Mỗi lần vào WARNING/DANGER -> ghi `AlertEventEntity` (thời gian, loại, reason, perclos).

---

## 9. Kế hoạch thực hiện theo Phase

### Phase 1 — Khung project

- Tạo project, Version Catalog, Hilt, Compose, Navigation, theme Material 3 (hỗ trợ dark mode, mặc định dark vì dùng khi lái xe).
- Màn hình trống cho Onboarding, Monitor, Calibration, History, Settings.
- **Hoàn thành khi**: `./gradlew assembleDebug` thành công, điều hướng giữa các màn hình được.

### Phase 2 — Quyền + Camera preview

- Onboarding: disclaimer + xin quyền `CAMERA` (và `POST_NOTIFICATIONS` trên API 33+).
- Monitor hiển thị preview camera trước toàn màn hình.
- `MainActivity` bật `FLAG_KEEP_SCREEN_ON` khi ở màn Monitor.
- **Hoàn thành khi**: thấy preview camera trước, xoay máy không crash.

### Phase 3 — MediaPipe + overlay

- `FaceLandmarkerHelper`, `FrameAnalyzer`, đưa kết quả qua `StateFlow`.
- `LandmarkOverlay` vẽ 12 điểm mắt và 4 điểm miệng lên preview (đúng tỉ lệ, đã tính lật gương và `FILL_CENTER`).
- Panel debug (bật/tắt trong Settings) hiển thị: fps, EAR, MAR, blink scores, jawOpen, pitch/yaw/roll, thời gian inference.
- **Hoàn thành khi**: điểm overlay bám đúng mắt/miệng, EAR giảm rõ khi nhắm mắt.

### Phase 4 — Calibration + Analyzer

- `Calibrator`, `CalibrationScreen` (đếm ngược, thanh tiến trình, kết quả baseline).
- `DrowsinessAnalyzer` + state machine đúng mục 7.
- Unit test cho `DrowsinessAnalyzer` bằng chuỗi `FaceFeatures` giả lập: mắt mở bình thường, chớp mắt bình thường (không được báo), nhắm 1.5 s (DANGER), PERCLOS 20% (WARNING), 3 lần ngáp (WARNING), hysteresis khi hạ cấp.
- **Hoàn thành khi**: toàn bộ unit test pass.

### Phase 5 — Cảnh báo + UI giám sát

- `AlertManager` theo mục 8.
- `MonitorScreen`: chỉ báo trạng thái lớn, dễ nhìn bằng khóe mắt (xanh / vàng / đỏ), nút Bắt đầu / Dừng chuyến, nút "Tôi đã tỉnh" khi DANGER, thời gian chuyến đi.
- **Hoàn thành khi**: nhắm mắt > 1.2 s thì còi kêu ngay, mở mắt 5 s thì tắt.

### Phase 6 — Lưu trữ + Lịch sử + Cài đặt

- Room: `TripEntity(id, startTime, endTime, warningCount, dangerCount)`, `AlertEventEntity(id, tripId, time, state, reason, perclos)`.
- `HistoryScreen`: danh sách chuyến, chi tiết số lần cảnh báo.
- `SettingsScreen`: độ nhạy (Thấp / Trung bình / Cao — ánh xạ sang bộ ngưỡng), bật/tắt TTS, rung, panel debug, calibrate lại.
- **Hoàn thành khi**: dữ liệu còn sau khi tắt mở app.

### Phase 7 — Tối ưu và độ bền

- Giảm fps xuống ~10 khi state NORMAL ổn định > 60 s, tăng lại khi có dấu hiệu.
- Theo dõi `PowerManager.getCurrentThermalStatus()` (API 29+): từ `THERMAL_STATUS_SEVERE` thì giảm fps và báo người dùng.
- Phát hiện thiếu sáng (độ sáng trung bình vùng mặt thấp) -> banner "Ánh sáng yếu, độ chính xác giảm".
- Xử lý vòng đời: app xuống nền thì dừng phân tích và hiện notification nhắc quay lại; không cố chạy camera ở nền.
- **Hoàn thành khi**: chạy liên tục 30 phút không crash, không rò bộ nhớ (kiểm tra bằng Profiler).

---

## 10. AndroidManifest

```xml
<uses-feature android:name="android.hardware.camera.front" android:required="true" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Không thêm foreground service ở bản đầu. Nếu sau này cần, phải dùng `foregroundServiceType="camera"` + quyền `FOREGROUND_SERVICE_CAMERA`, và service chỉ được khởi động khi app đang ở foreground (giới hạn từ Android 14).

---

## 11. Yêu cầu chất lượng code

- Kotlin idiomatic, không dùng `!!` trừ khi có lý do rõ ràng.
- Mọi ngưỡng/hằng số nằm trong `DetectionConfig` hoặc Settings, không hard-code rải rác.
- Không log dữ liệu landmark ra Logcat ở bản release.
- Compose: state hoisting, `collectAsStateWithLifecycle()`, preview cho các composable chính.
- Chuỗi hiển thị đặt trong `strings.xml` (tiếng Việt mặc định, có `values-en`).

---

## 12. Giới hạn đã biết (ghi rõ trong app và tài liệu)

- Camera điện thoại không có hồng ngoại: ban đêm độ chính xác giảm đáng kể.
- Kính râm che mắt: chỉ còn tín hiệu ngáp và gật đầu.
- Vị trí gắn điện thoại ảnh hưởng lớn: khuyến nghị đặt chính diện, ngang tầm mắt, cách 40–70 cm.
- App không phải thiết bị an toàn được chứng nhận.
