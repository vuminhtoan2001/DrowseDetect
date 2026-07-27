package com.camera.studio.photoeditor.ui.camera

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.camera.studio.photoeditor.R
import kotlin.math.roundToInt
import com.camera.studio.photoeditor.core.BitmapUtils
import com.camera.studio.photoeditor.data.db.SavedMedia
import com.camera.studio.photoeditor.ui.camera.model.CameraTimerOption
import com.camera.studio.photoeditor.ui.camera.model.FocusMode
import com.camera.studio.photoeditor.ui.camera.model.MoreMenuItem
import com.camera.studio.photoeditor.ui.camera.model.ResolutionOption
import com.camera.studio.photoeditor.ui.camera.model.WhiteBalancePreset
import com.camera.studio.photoeditor.ui.camera.model.ampMatrix
import com.camera.studio.photoeditor.ui.camera.model.combineColorMatrices
import com.camera.studio.photoeditor.ui.camera.model.exposureMatrix
import com.camera.studio.photoeditor.ui.shared.collage.CollageCompositor
import com.camera.studio.photoeditor.ui.shared.collage.CollageLayout
import com.camera.studio.photoeditor.ui.shared.collage.CollageLayoutPicker
import com.camera.studio.photoeditor.ui.shared.filter.FilterOption
import com.camera.studio.photoeditor.ui.shared.filter.FilterPicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private enum class ToolbarPanel { NONE, AMP, WB, FILTER, EXPOSURE, ZOOM }

/** 3 tỉ lệ mặc định của camera thật (+ "Full" = không crop) — khớp góp ý, bỏ bớt 3/4, 9/16, 3/2. */
private val CAMERA_RATIOS = listOf(
    com.camera.studio.photoeditor.ui.shared.editor.RatioOption.R16_9,
    com.camera.studio.photoeditor.ui.shared.editor.RatioOption.R4_3,
    com.camera.studio.photoeditor.ui.shared.editor.RatioOption.SQUARE,
)

/** Video (Recorder/Quality FHD-2K-4K trong CameraModels.kt) chỉ xuất ra ở tỉ lệ gốc 16:9 của cảm
 * biến — không có bước transcode/crop pixel thật cho video như ảnh chụp, nên 4:3 và 1:1 KHÔNG thể
 * áp đúng cho video (chọn sẽ ra video sai tỉ lệ, lệch preview/video thật). Video chỉ giữ Full/16:9. */
private val VIDEO_SUPPORTED_RATIOS = listOf(com.camera.studio.photoeditor.ui.shared.editor.RatioOption.R16_9)

/** [RatioOption.widthRatio]/[heightRatio] định nghĩa theo chiều LANDSCAPE quy ước ("16/9", "4/3")
 * — máy quay ở PORTRAIT nên tỉ lệ áp thật cho preview/ảnh phải đảo ngược (vd "16/9" → ảnh CAO,
 * width:height = 9:16), không phải áp thẳng widthRatio/heightRatio (sẽ ra ảnh RỘNG sai hướng). */
private val com.camera.studio.photoeditor.ui.shared.editor.RatioOption.portraitAspect: Float
    get() = heightRatio / widthRatio

/**
 * Camera đầy đủ theo docs/02_CAMERA_TAKE_PHOTO_VIDEO.md + mockup Board 2 (đã sửa theo góp ý: top
 * bar trải đều full-width, WB 2 hàng cố định, More-menu/Resolution/Collage-layout xổ từ header,
 * Video-mode chỉ hiện Focus/Exposure/Resolution SAU khi bấm More-menu).
 */
@Composable
fun CameraScreen(
    initialIsVideo: Boolean = false,
    onBack: () -> Unit,
    onPhotoSaved: () -> Unit,
    onVideoRecorded: (videoUri: String, previewWidthDp: Float) -> Unit,
    onOpenLatest: (path: String, isVideo: Boolean) -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCameraPermission = it }
    DisposableEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(android.Manifest.permission.CAMERA)
        onDispose { }
    }

    var isVideoMode by remember { mutableStateOf(initialIsVideo) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var boundCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // Thumbnail ảnh/video vừa chụp gần nhất — hiện cạnh nút chụp để chụp liên tục mà không rời màn
    // Camera (trước đây chụp xong tự nhảy sang màn "thành công"/xem lại video, chặn chụp liên tục).
    val latestPhoto by viewModel.latestPhoto.collectAsState()
    val latestVideo by viewModel.latestVideo.collectAsState()

    // ---- toolbar / more-menu state ----
    // Mở sẵn panel Filter ngay khi vào màn Camera (khớp app tham khảo: Popular/Adventure/... hiện
    // luôn, không cần bấm icon filter mới thấy).
    var toolbarPanel by remember { mutableStateOf(ToolbarPanel.FILTER) }
    var moreMenuOpen by remember { mutableStateOf(false) }
    var moreMenuActive by remember { mutableStateOf<MoreMenuItem?>(null) }

    var ampPercent by remember { mutableStateOf(0) }
    var wbPreset by remember { mutableStateOf(WhiteBalancePreset.AUTO) }
    var selectedFilter by remember { mutableStateOf<FilterOption?>(null) }
    var exposurePercent by remember { mutableStateOf(0) }
    var zoomRatio by remember { mutableStateOf(1) } // 1..100 (UI range theo yêu cầu) — áp thực tế có clamp theo zoomState.maxZoomRatio của thiết bị
    var gridOn by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }
    var timerOption by remember { mutableStateOf(CameraTimerOption.OFF) }
    var countdownRemaining by remember { mutableStateOf<Int?>(null) }
    var focusMode by remember { mutableStateOf(FocusMode.AUTO) }
    var resolution by remember { mutableStateOf(ResolutionOption.TWO_K) }

    // ---- collage live-shoot state ----
    var collageLayout by remember { mutableStateOf<CollageLayout?>(null) }
    val collagePanes = remember { mutableStateListOf<Bitmap>() }

    // ---- ratio panel state (nút "Full" ở header) ----
    var ratioPanelOpen by remember { mutableStateOf(false) }
    var selectedRatio by remember { mutableStateOf<com.camera.studio.photoeditor.ui.shared.editor.RatioOption?>(null) } // null = Full (không crop)

    // Thumbnail preview của Filter picker dùng 1 ẢNH MẪU cố định (không phải chụp live từ camera mỗi
    // 1.2s như trước — không cần thiết, chỉ cần thấy rõ khác biệt màu giữa các filter).
    val sampleFilterBitmap = remember {
        runCatching { context.assets.open("images/sample_2.webp").use { BitmapFactory.decodeStream(it) } }.getOrNull()
    }

    val combinedMatrix = remember(wbPreset, selectedFilter, ampPercent, exposurePercent) {
        combineColorMatrices(wbPreset.matrix, selectedFilter?.matrix, ampMatrix(ampPercent), exposureMatrix(exposurePercent))
    }
    LaunchedEffect(combinedMatrix, previewView) {
        val pv = previewView ?: return@LaunchedEffect
        if (combinedMatrix == null) {
            pv.setLayerType(View.LAYER_TYPE_NONE, null)
        } else {
            pv.setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply { colorFilter = ColorMatrixColorFilter(combinedMatrix) })
        }
    }

    // Focus thật (CameraX không lộ API bật/tắt macro/infinity/detection trực tiếp — 3 mode đó vẫn
    // chỉ là lựa chọn hiển thị như đã ghi chú ở CameraModels.kt; AUTO/CENTER là 2 mode DÙNG THẬT
    // qua FocusMeteringAction để xác nhận chức năng có hoạt động).
    LaunchedEffect(focusMode, boundCamera, previewView) {
        val camera = boundCamera ?: return@LaunchedEffect
        val pv = previewView ?: return@LaunchedEffect
        when (focusMode) {
            FocusMode.CENTER -> runCatching {
                val point = pv.meteringPointFactory.createPoint(pv.width / 2f, pv.height / 2f)
                camera.cameraControl.startFocusAndMetering(androidx.camera.core.FocusMeteringAction.Builder(point).build())
            }
            FocusMode.AUTO -> runCatching { camera.cameraControl.cancelFocusAndMetering() }
            else -> {}
        }
    }

    // Chuyển sang Video mà tỉ lệ đang chọn (4:3/1:1) không phải tỉ lệ video hỗ trợ (chỉ Full/16:9,
    // xem VIDEO_SUPPORTED_RATIOS) → auto về Full, tránh preview/video lệch tỉ lệ đang hiển thị.
    LaunchedEffect(isVideoMode) {
        if (isVideoMode && selectedRatio != null && selectedRatio !in VIDEO_SUPPORTED_RATIOS) selectedRatio = null
    }

    // Bind/rebind camera CHỈ khi previewView đã sẵn sàng hoặc đổi camera trước-sau/độ phân giải —
    // KHÔNG chạy lại khi đổi tỉ lệ/AMP/WB/zoom (những state đó không cần rebind, xem previewBoxModifier
    // + clipToBounds ở trên) để tránh unbindAll()+rebind liên tục gây chớp đen mỗi lần tương tác.
    LaunchedEffect(previewView, isFrontCamera, resolution) {
        val pv = previewView ?: return@LaunchedEffect
        val providerFuture = ProcessCameraProvider.getInstance(pv.context)
        providerFuture.addListener({
            bindCamera(providerFuture.get(), pv, lifecycleOwner, isFrontCamera, resolution) { capture, video, camera ->
                imageCapture = capture; videoCapture = video; boundCamera = camera
            }
        }, ContextCompat.getMainExecutor(pv.context))
    }

    // Flash trước đây chỉ đổi màu icon, KHÔNG có tác dụng gì với ảnh chụp thật — set trực tiếp lên
    // `ImageCapture` đang bind (flashMode có thể đổi bất kỳ lúc nào, không cần bind lại camera).
    LaunchedEffect(flashOn, imageCapture) {
        imageCapture?.flashMode = if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    // Zoom thật qua CameraX — trước đây dùng setZoomRatio(zoomRatio.coerceIn(minR, maxR)): vì
    // maxZoomRatio thật của thiết bị thường nhỏ hơn nhiều so với 100 (vd chỉ ~2x-8x), giá trị bị
    // clamp rất sớm nên vuốt qua khoảng đầu là hết tác dụng, phần còn lại của thanh trượt (vd
    // 3..100) không thấy zoom đổi nữa. Sửa: quy đổi zoomRatio (1..100) thành tỉ lệ 0f..1f rồi dùng
    // setLinearZoom — API này trải ĐỀU 0f..1f lên toàn bộ khoảng zoom thật [minZoomRatio,
    // maxZoomRatio] của thiết bị, nên vuốt hết thanh trượt 1..100 luôn thấy zoom thay đổi liên tục
    // dù thiết bị hỗ trợ zoom tối đa bao nhiêu.
    LaunchedEffect(zoomRatio, boundCamera) {
        val camera = boundCamera ?: return@LaunchedEffect
        val fraction = (zoomRatio - 1).toFloat() / 99f
        runCatching { camera.cameraControl.setLinearZoom(fraction.coerceIn(0f, 1f)) }
    }

    fun closeAllPanels() { toolbarPanel = ToolbarPanel.NONE; moreMenuOpen = false; moreMenuActive = null; ratioPanelOpen = false }

    // Chỉ 1 nhóm option (ratio/more-menu ở header HOẶC panel ở bottom) được mở tại 1 thời điểm — mở
    // nhóm này phải đóng hẳn nhóm kia, tránh 2 nhóm option cùng hiện như trước.
    fun openToolbarPanel(panel: ToolbarPanel) {
        toolbarPanel = toggle(toolbarPanel, panel)
        moreMenuOpen = false
        moreMenuActive = null
        ratioPanelOpen = false
    }

    fun bakeBitmap(raw: Bitmap): Bitmap {
        val matrix = combinedMatrix ?: return raw
        val out = Bitmap.createBitmap(raw.width, raw.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(raw, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
        return out
    }

    fun capturePhoto(onDone: (Bitmap) -> Unit) {
        val capture = imageCapture ?: return
        isBusy = true
        val photoFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(photoFile).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bitmap = bakeBitmap(BitmapUtils.decodeAndRotate(photoFile))
                    isBusy = false
                    onDone(bitmap)
                }
                override fun onError(exception: ImageCaptureException) { isBusy = false }
            },
        )
    }

    fun performCapture() {
        val layout = collageLayout
        if (layout != null) {
            val paneIndex = collagePanes.size
            capturePhoto { bitmap ->
                // Cắt đúng 1 GÓC/phần của ảnh full-frame vừa chụp — khớp đúng vị trí a/b/c/d trong bố
                // cục, để lúc ghép lại tái tạo đúng ảnh gốc (trước đây lưu nguyên cả ảnh full-frame
                // cho mỗi khung → xuất ra mỗi vị trí lại là ảnh full, không phải 1/N như đúng ra phải
                // có).
                val rects = layout.panes(bitmap.width, bitmap.height, gap = 0)
                val rect = rects.getOrNull(paneIndex)
                val slice = if (rect != null) {
                    val l = rect.left.coerceIn(0, bitmap.width - 1)
                    val t = rect.top.coerceIn(0, bitmap.height - 1)
                    val w = rect.width().coerceIn(1, bitmap.width - l)
                    val h = rect.height().coerceIn(1, bitmap.height - t)
                    Bitmap.createBitmap(bitmap, l, t, w, h)
                } else bitmap
                collagePanes.add(slice)
                if (collagePanes.size >= layout.paneCount) {
                    scope.launch {
                        // gapPx=0: đây là ghép lại các lát cắt từ 1 ảnh gốc (photo-splitter), không
                        // phải collage trang trí có khoảng cách — có gap sẽ ra viền đen giữa các lát.
                        val composite = CollageCompositor.composite(layout, collagePanes, 1080, 1080, gapPx = 0)
                        viewModel.saveCollageBitmap(composite)
                        collagePanes.clear()
                        collageLayout = null
                        onPhotoSaved()
                    }
                }
            }
            return
        }
        if (!isVideoMode) {
            capturePhoto { bitmap ->
                val ratio = selectedRatio
                val cropped = if (ratio != null) BitmapUtils.centerCropToAspect(bitmap, ratio.portraitAspect, 1f) else bitmap
                // KHÔNG điều hướng đi đâu — ở lại Camera để chụp liên tục, thumbnail cập nhật qua
                // `latestPhoto` (StateFlow từ Room, tự refresh sau khi lưu).
                scope.launch { viewModel.savePhotoBitmap(cropped) }
            }
        } else {
            val video = videoCapture ?: return
            if (!isRecording) {
                val videoFile = File(context.cacheDir, "record_${System.currentTimeMillis()}.mp4")
                recording = video.output
                    .prepareRecording(context, FileOutputOptions.Builder(videoFile).build())
                    .start(ContextCompat.getMainExecutor(context)) { event ->
                        if (event is VideoRecordEvent.Finalize) {
                            isRecording = false
                            // Tương tự ảnh — ở lại Camera để quay liên tục thay vì nhảy sang màn xem lại.
                            scope.launch { viewModel.registerSavedVideo(videoFile.absolutePath) }
                        }
                    }
                isRecording = true
            } else {
                recording?.stop(); recording = null
            }
        }
    }

    // Timer 3s/5s/10s trước đây chỉ đổi label icon, KHÔNG có tác dụng thật — giờ đếm ngược thật rồi
    // mới chụp/quay (chỉ áp khi BẮT ĐẦU 1 lượt mới, không áp khi bấm để DỪNG quay video đang chạy).
    fun onShutterTap() {
        if (timerOption != CameraTimerOption.OFF && !isRecording) {
            scope.launch {
                for (s in timerOption.seconds downTo 1) {
                    countdownRemaining = s
                    delay(1000)
                }
                countdownRemaining = null
                performCapture()
            }
        } else {
            performCapture()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        // Vùng preview co theo tỉ lệ đã chọn (Full = full màn, còn lại letterbox đen 2 bên/trên-dưới)
        // — khớp góp ý "chọn option nào cũng cần co ảnh và preview theo option đó". Modifier.aspectRatio
        // (cách cũ) KHÔNG co được vì Box cha đã bị ép kích thước CHÍNH XÁC (exact constraints từ
        // fillMaxSize lan xuống từ root) — aspectRatio chỉ co khi hướng đó còn khoảng trống linh hoạt,
        // nên preview trước đây luôn full màn dù đã chọn 4:3/16:9/1:1 (đúng như ảnh chụp so sánh với
        // app tham khảo). Sửa: tự tính width/height "contain" theo maxWidth/maxHeight của
        // BoxWithConstraints rồi gán .size() tường minh.
        val previewBoxModifier = selectedRatio?.let { ratio ->
            val targetAspect = ratio.portraitAspect
            val heightFromMaxWidth = maxWidth / targetAspect
            if (heightFromMaxWidth <= maxHeight) Modifier.size(maxWidth, heightFromMaxWidth)
            else Modifier.size(maxHeight * targetAspect, maxHeight)
        } ?: Modifier.fillMaxSize()
        // clipToBounds(): PreviewView (AndroidView interop) không tự giới hạn theo kích thước Compose
        // đã tính — nó VẼ TRÀN ra ngoài Box cha (đã xác nhận bằng debug: Box tính đúng 4:3 nhưng
        // preview vẫn vẽ full màn). Compose không tự clip children theo mặc định, nên phải clip tường
        // minh để phần preview thừa ngoài tỉ lệ đã chọn không vẽ đè lên vùng letterbox đen.
        Box(Modifier.align(Alignment.Center).then(previewBoxModifier).clipToBounds()) {
            if (hasCameraPermission) {
                // Bind camera KHÔNG còn nằm trong AndroidView update{} (trước đây chạy lại ở MỌI lần
                // recompose — kể cả đổi AMP/WB/zoom/tỉ lệ — unbindAll()+rebind liên tục gây chớp/giật
                // hình). Giờ tách riêng ra LaunchedEffect(isFrontCamera, resolution) bên dưới, chỉ
                // bind lại đúng lúc cần (đổi camera trước/sau, đổi độ phân giải). Đổi TỈ LỆ giờ chỉ là
                // co/clip ở tầng Compose (previewBoxModifier + clipToBounds ở trên) — không đụng tới
                // camera session đang chạy nên không còn chớp đen khi chuyển tỉ lệ.
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }.also { previewView = it }
                    },
                )
            } else {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera Studio cần quyền Camera để chụp ảnh/quay video.", color = Color.White)
                }
            }

            if (gridOn) GridOverlay()
            collageLayout?.let { layout -> CollageSplitOverlay(layout, collagePanes) }
        }

        // ---------- Top bar: 6 icon gom gần nhau, căn giữa ----------
        // 1 nền tối PHẲNG đồng nhất bọc cả cụm header (icon row + more-menu/ratio-panel khi mở) —
        // cùng kiểu với bottom, khớp góp ý "các option ở header cần nằm trong box như bottom".
        // statusBarsPadding() đẩy nội dung xuống dưới status bar THẬT của hệ thống (trước đây dùng
        // padding(top=22.dp) cố định, một số máy status bar cao hơn 22dp → icon bị đè lên đồng
        // hồ/pin hệ thống); nền đen mờ vẫn phủ full kể cả vùng sau status bar do statusBarsPadding
        // đặt SAU background trong chain modifier.
        Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopCircle(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                TopChip(selectedRatio?.label ?: "Full", active = ratioPanelOpen) {
                    ratioPanelOpen = !ratioPanelOpen
                    moreMenuOpen = false; moreMenuActive = null
                    toolbarPanel = ToolbarPanel.NONE
                }
                TopCircle(onClick = { flashOn = !flashOn }, active = flashOn) { DrawableIcon(R.drawable.ic_flash_on) }
                TopCircle(onClick = { timerOption = nextTimer(timerOption) }, active = timerOption != CameraTimerOption.OFF) {
                    if (timerOption == CameraTimerOption.OFF) DrawableIcon(R.drawable.ic_alarm_clock)
                    else Text(timerOption.seconds.toString(), color = Color(0xFFF5C518), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                TopCircle(onClick = {
                    moreMenuOpen = !moreMenuOpen
                    if (!moreMenuOpen) moreMenuActive = null
                    toolbarPanel = ToolbarPanel.NONE
                    ratioPanelOpen = false
                }, active = moreMenuOpen) { DrawableIcon(R.drawable.ic_more2) }
            }

            if (ratioPanelOpen) {
                // Chỉ 4 tỉ lệ mặc định của camera thật (bỏ 3/4, 9/16, 3/2 dư thừa) — khớp góp ý
                // "chỉ có 16:9, 4:3, 1:1, full". Chọn xong CROP CẢ preview lẫn ảnh chụp theo tỉ lệ đó.
                // Ở chế độ VIDEO chỉ còn Full/16:9 — Recorder (FHD/2K/4K ở CameraModels.kt) luôn xuất
                // ra đúng tỉ lệ gốc 16:9 của cảm biến, không có bước crop/transcode pixel thật cho
                // video như ảnh chụp, nên 4:3/1:1 không áp được đúng cho video.
                val availableRatios = if (isVideoMode) VIDEO_SUPPORTED_RATIOS else CAMERA_RATIOS
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp).padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RatioChip("Full", selectedRatio == null) { selectedRatio = null; ratioPanelOpen = false }
                    availableRatios.forEach { opt ->
                        RatioChip(opt.label, selectedRatio == opt) { selectedRatio = opt; ratioPanelOpen = false }
                    }
                }
            }

            if (moreMenuOpen) {
                val items = if (isVideoMode) {
                    listOf(MoreMenuItem.FOCUS, MoreMenuItem.RESOLUTION)
                } else {
                    listOf(MoreMenuItem.GRID, MoreMenuItem.FOCUS, MoreMenuItem.RESOLUTION, MoreMenuItem.COLLAGE)
                }
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        items.forEach { item ->
                            MoreMenuIcon(item, active = moreMenuActive == item) {
                                moreMenuActive = if (moreMenuActive == item) null else item
                                if (item == MoreMenuItem.GRID) { gridOn = !gridOn; moreMenuActive = null }
                            }
                        }
                    }
                    when (moreMenuActive) {
                        MoreMenuItem.FOCUS -> Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            FocusMode.entries.forEach { mode ->
                                FocusChip(mode, selected = focusMode == mode) { focusMode = mode }
                            }
                        }
                        MoreMenuItem.RESOLUTION -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ResolutionOption.entries.forEach { opt ->
                                    ResolutionChip(opt, selected = resolution == opt) { resolution = opt }
                                }
                            }
                        }
                        MoreMenuItem.COLLAGE -> Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                            CollageLayoutPicker(
                                selected = collageLayout ?: CollageLayout.SINGLE,
                                onSelect = { layout ->
                                    collageLayout = layout
                                    collagePanes.clear()
                                    moreMenuOpen = false
                                    moreMenuActive = null
                                },
                            )
                        }
                        else -> {}
                    }
                }
            }
        }

        // ---------- Bottom: sheet panel + toolbar + mode toggle + capture row ----------
        // 1 nền tối PHẲNG (không gradient) bọc cả cụm — luôn hiện, đồng nhất dù panel nào đang mở
        // hay không mở gì, khớp `takephoto_amp.jpg`/`take_photo_wb.jpg`.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)),
        ) {
            when (toolbarPanel) {
                ToolbarPanel.AMP -> BottomSheetPanel {
                    AdjustSliderBar(value = ampPercent, range = 0..100, onChange = { ampPercent = it }, onClose = { toolbarPanel = ToolbarPanel.NONE }, onAutoReset = { ampPercent = 0 })
                }
                ToolbarPanel.WB -> BottomSheetPanel {
                    WbGrid(wbPreset) { wbPreset = it }
                }
                ToolbarPanel.FILTER -> BottomSheetPanel {
                    FilterPicker(selected = selectedFilter, onSelect = { selectedFilter = it }, previewBitmap = sampleFilterBitmap)
                }
                ToolbarPanel.EXPOSURE -> BottomSheetPanel {
                    AdjustSliderBar(value = exposurePercent, range = -100..100, onChange = { exposurePercent = it }, onClose = { toolbarPanel = ToolbarPanel.NONE }, onAutoReset = { exposurePercent = 0 })
                }
                ToolbarPanel.ZOOM -> BottomSheetPanel {
                    AdjustSliderBar(value = zoomRatio, range = 1..100, onChange = { zoomRatio = it }, onClose = { toolbarPanel = ToolbarPanel.NONE }, onAutoReset = { zoomRatio = 1 }, valueLabel = { "${it}x" })
                }
                ToolbarPanel.NONE -> {}
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    ToolbarIcon("AMP", active = toolbarPanel == ToolbarPanel.AMP) { openToolbarPanel(ToolbarPanel.AMP) }
                    ToolbarIcon("WB", active = toolbarPanel == ToolbarPanel.WB) { openToolbarPanel(ToolbarPanel.WB) }
                    ToolbarIcon("${zoomRatio}x", active = toolbarPanel == ToolbarPanel.ZOOM) { openToolbarPanel(ToolbarPanel.ZOOM) }
                    ToolbarIconDrawable(R.drawable.ic_adjust_brightness, active = toolbarPanel == ToolbarPanel.EXPOSURE) { openToolbarPanel(ToolbarPanel.EXPOSURE) }
                    ToolbarIconDrawable(R.drawable.ic_filter, active = toolbarPanel == ToolbarPanel.FILTER) { openToolbarPanel(ToolbarPanel.FILTER) }
                }
            }

            if (collageLayout == null) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("PHOTO", color = if (!isVideoMode) Color.White else Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isVideoMode = false; closeAllPanels() })
                        Text("VIDEO", color = if (isVideoMode) Color.White else Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isVideoMode = true; closeAllPanels() })
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(bottom = 28.dp, start = 24.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LastCapturedThumb(
                    media = if (isVideoMode) latestVideo else latestPhoto,
                    isVideo = isVideoMode,
                    onClick = onOpenLatest,
                )
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .background(if (isRecording) Color.Red else Color.White, CircleShape)
                        .clickable(enabled = !isBusy) { onShutterTap() },
                )
                IconButton(onClick = { isFrontCamera = !isFrontCamera }) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = null, tint = Color.White)
                }
            }
        }

        if (isBusy) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)

        countdownRemaining?.let { seconds ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Text("$seconds", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun nextTimer(current: CameraTimerOption): CameraTimerOption {
    val entries = CameraTimerOption.entries
    return entries[(entries.indexOf(current) + 1) % entries.size]
}

private fun toggle(current: ToolbarPanel, target: ToolbarPanel) = if (current == target) ToolbarPanel.NONE else target

@Composable
private fun TopCircle(onClick: () -> Unit, active: Boolean = false, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (active) Color(0xFFF5C518).copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun TopChip(label: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) Color(0xFFF5C518).copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (active) Color(0xFFF5C518) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RatioChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFF5C518).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (selected) Color(0xFFF5C518) else Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DrawableIcon(resId: Int, tint: Color = Color.White, sizeDp: Int = 18) {
    Icon(painterResource(resId), contentDescription = null, tint = tint, modifier = Modifier.size(sizeDp.dp))
}

@Composable
private fun MoreMenuIcon(item: MoreMenuItem, active: Boolean, onClick: () -> Unit) {
    val (resId, label) = when (item) {
        MoreMenuItem.GRID -> R.drawable.ic_grid to "Grid"
        MoreMenuItem.FOCUS -> R.drawable.ic_focus to "Focus"
        MoreMenuItem.EXPOSURE -> R.drawable.ic_adjust_brightness to "Exposure"
        MoreMenuItem.RESOLUTION -> R.drawable.ic_resolution to "Resolution"
        MoreMenuItem.COLLAGE -> R.drawable.ic_collage to "Collage"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                .background(if (active) Color(0xFFF5C518).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { DrawableIcon(resId, tint = if (active) Color(0xFFF5C518) else Color.White) }
        Text(label, color = if (active) Color(0xFFF5C518) else Color.White.copy(alpha = 0.85f), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FocusChip(mode: FocusMode, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        val resId = when (mode) {
            FocusMode.AUTO -> R.drawable.ic_focus
            FocusMode.CENTER -> R.drawable.ic_center_focus
            FocusMode.DETECTION -> R.drawable.ic_focus_detection
            FocusMode.INFINITY -> R.drawable.ic_infinity_focus
            FocusMode.MACRO -> R.drawable.ic_macro_focus
        }
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                .background(if (selected) Color(0xFFF5C518).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { DrawableIcon(resId, tint = if (selected) Color(0xFFF5C518) else Color.White) }
        Text(mode.label, color = Color.White, fontSize = 8.sp)
    }
}

@Composable
private fun ResolutionChip(option: ResolutionOption, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(option.label, color = if (selected) Color(0xFFF5C518) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (option.isPremium) Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFF5C518), modifier = Modifier.size(11.dp).padding(start = 3.dp))
        }
        Text("${option.widthPx}x${option.heightPx}", color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp)
    }
}

/**
 * Thanh trượt pill nền tối bo tròn, track/thumb vàng cam, bubble % nổi phía trên thumb theo vị trí
 * kéo — khớp style `takephoto_amp.jpg` (thay cho `Slider` Material mặc định trước đây: phẳng, màu
 * xanh, không pill, không bubble). Dùng chung cho AMP/Exposure.
 */
@Composable
private fun AdjustSliderBar(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    onClose: (() -> Unit)? = null,
    onAutoReset: (() -> Unit)? = null,
    valueLabel: (Int) -> String = { "$it%" },
) {
    val fraction = ((value - range.first).toFloat() / (range.last - range.first).toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 34.dp, end = if (onAutoReset != null) 34.dp else 8.dp)) {
            val bubbleOffset = maxWidth * fraction
            Box(
                Modifier
                    .offset(x = bubbleOffset)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF5C518))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) { Text(valueLabel(value), color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onClose != null) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp).clickable(onClick = onClose))
            }
            ThinSliderTrack(
                fraction = fraction,
                onFractionChange = { f -> onChange((range.first + f * (range.last - range.first)).roundToInt()) },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            if (onAutoReset != null) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).clickable(onClick = onAutoReset),
                    contentAlignment = Alignment.Center,
                ) { Text("A", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/**
 * Slider mỏng tự vẽ (track cao 3dp + thumb tròn nhỏ 12dp) — thay `Slider` Material3 mặc định (thumb
 * to 20dp+track dày) theo đúng góp ý "thanh kéo vẫn to thô, cần thu nhỏ". 1 pointerInput duy nhất
 * xử lý cả chạm-để-nhảy-tới lẫn kéo (không tách tap/drag riêng — tránh lỗi giành consume() đã gặp ở
 * DraggableLayerHost).
 */
@Composable
private fun ThinSliderTrack(fraction: Float, onFractionChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier
            .height(24.dp)
            .pointerInput(Unit) {
                fun updateFromX(x: Float) { onFractionChange((x / size.width.toFloat()).coerceIn(0f, 1f)) }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    updateFromX(down.position.x)
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.positionChanged()) { updateFromX(change.position.x); change.consume() }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.3f)))
        Box(Modifier.fillMaxWidth(fraction.coerceAtLeast(0.015f)).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFF5C518)))
        Box(
            Modifier
                .offset(x = maxWidth * fraction - 6.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5C518)),
        )
    }
}

private const val WB_COLUMNS = 4

@Composable
private fun WbGrid(selected: WhiteBalancePreset, onSelect: (WhiteBalancePreset) -> Unit) {
    val entries = WhiteBalancePreset.entries
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.chunked(WB_COLUMNS).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val missing = WB_COLUMNS - row.size
                if (missing > 0) Spacer(Modifier.weight(missing / 2f))
                row.forEach { preset ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { onSelect(preset) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(preset.label, color = if (preset == selected) Color(0xFFF5C518) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (missing > 0) Spacer(Modifier.weight(missing / 2f))
            }
        }
    }
}

@Composable
private fun BottomSheetPanel(content: @Composable () -> Unit) {
    // KHÔNG tự có nền riêng — dùng chung nền phẳng của Column cha để đồng nhất độ tối dù panel nào
    // đang mở hay đang ở trạng thái icon-row thường (đã sửa theo góp ý "chưa đồng nhất").
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { content() }
}

/** Thumbnail ảnh/video vừa chụp gần nhất, thay cho ô placeholder tĩnh trước đây — bấm để mở lại (xem/sửa). */
@Composable
private fun LastCapturedThumb(media: SavedMedia?, isVideo: Boolean, onClick: (path: String, isVideo: Boolean) -> Unit) {
    val bmp = remember(media?.path, isVideo) {
        media?.path?.let { path ->
            runCatching {
                if (isVideo) {
                    val retriever = android.media.MediaMetadataRetriever()
                    try { retriever.setDataSource(path); retriever.frameAtTime } finally { retriever.release() }
                } else {
                    android.graphics.BitmapFactory.decodeFile(path)
                }
            }.getOrNull()
        }
    }
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(enabled = media != null) { media?.let { onClick(it.path, isVideo) } },
    ) {
        bmp?.let {
            Image(
                bitmap = it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}

@Composable
private fun ToolbarIcon(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(if (active) Color(0xFFF5C518).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (active) Color(0xFFF5C518) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToolbarIconDrawable(resId: Int, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(if (active) Color(0xFFF5C518).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { DrawableIcon(resId, tint = if (active) Color(0xFFF5C518) else Color.White, sizeDp = 20) }
}

@Composable
private fun GridOverlay() {
    ComposeCanvas(Modifier.fillMaxSize()) {
        val strokeColor = Color.White.copy(alpha = 0.5f)
        val w = size.width; val h = size.height
        drawLine(strokeColor, androidx.compose.ui.geometry.Offset(w / 3, 0f), androidx.compose.ui.geometry.Offset(w / 3, h), 1f)
        drawLine(strokeColor, androidx.compose.ui.geometry.Offset(w * 2 / 3, 0f), androidx.compose.ui.geometry.Offset(w * 2 / 3, h), 1f)
        drawLine(strokeColor, androidx.compose.ui.geometry.Offset(0f, h / 3), androidx.compose.ui.geometry.Offset(w, h / 3), 1f)
        drawLine(strokeColor, androidx.compose.ui.geometry.Offset(0f, h * 2 / 3), androidx.compose.ui.geometry.Offset(w, h * 2 / 3), 1f)
    }
}

/**
 * Che khung collage đúng như app mẫu: chỉ khung ĐANG CHỜ chụp (kế tiếp) để lộ live camera feed
 * (không vẽ gì đè lên); khung ĐÃ CHỤP hiện đúng ảnh đã chụp (frozen, không phải live feed dim nhẹ
 * như trước); khung CHƯA TỚI LƯỢT che đen hẳn — trước đây mọi khung đều để lộ live feed y hệt nhau
 * (chỉ có viền chia), sai với `takephoto_collage.jpg` (nửa phải đen, chỉ nửa trái live).
 */
@Composable
private fun CollageSplitOverlay(layout: CollageLayout, capturedBitmaps: List<Bitmap>) {
    ComposeCanvas(Modifier.fillMaxSize()) {
        val rects = layout.panes(size.width.toInt(), size.height.toInt(), gap = 3)
        rects.forEachIndexed { i, r ->
            val topLeft = androidx.compose.ui.geometry.Offset(r.left.toFloat(), r.top.toFloat())
            val rectSize = androidx.compose.ui.geometry.Size(r.width().toFloat(), r.height().toFloat())
            when {
                i < capturedBitmaps.size -> {
                    val cropped = BitmapUtils.centerCropToAspect(capturedBitmaps[i], rectSize.width, rectSize.height)
                    drawImage(
                        image = cropped.asImageBitmap(),
                        dstOffset = androidx.compose.ui.unit.IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(rectSize.width.roundToInt(), rectSize.height.roundToInt()),
                    )
                }
                i == capturedBitmaps.size -> { /* khung đang chờ chụp — để trống, lộ live feed thật phía dưới */ }
                else -> drawRect(Color.Black, topLeft = topLeft, size = rectSize)
            }
            drawRect(Color.White.copy(alpha = 0.6f), topLeft = topLeft, size = rectSize, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
        }
    }
}

private fun bindCamera(
    provider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    useFrontCamera: Boolean,
    resolution: ResolutionOption,
    onBound: (ImageCapture, VideoCapture<Recorder>, androidx.camera.core.Camera) -> Unit,
) {
    provider.unbindAll()
    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
    // Resolution (FHD/2K/4K) trước đây chỉ áp cho VIDEO (Recorder) — ẢNH CHỤP luôn dùng resolution
    // mặc định của CameraX, chọn 2K/4K không có tác dụng gì với PHOTO. Thêm ResolutionSelector để
    // resolution cũng áp thật cho ImageCapture, khớp đúng ý nghĩa nhãn "FHD/2K/4K" trong UI.
    val resolutionSelector = androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
        .setResolutionStrategy(
            androidx.camera.core.resolutionselector.ResolutionStrategy(
                android.util.Size(resolution.widthPx, resolution.heightPx),
                androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
            ),
        )
        .build()
    val imageCapture = ImageCapture.Builder().setResolutionSelector(resolutionSelector).build()
    val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(resolution.quality))
        .build()
    val videoCapture = VideoCapture.withOutput(recorder)
    val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    // Dùng chung ViewPort với PreviewView khi có (view đã layout xong) để ảnh ImageCapture crop
    // ĐÚNG khớp phần đang thấy trên preview (WYSIWYG) — khớp góp ý "preview cho từng phần chưa
    // đúng" ở chế độ Collage live-shoot. Chưa có viewPort (lần bind đầu, view chưa layout) thì bind
    // thường như trước, tự đúng lại ở lần `update` kế tiếp khi view đã có kích thước.
    val viewPort = previewView.viewPort
    val camera = runCatching {
        if (viewPort != null) {
            val group = androidx.camera.core.UseCaseGroup.Builder()
                .setViewPort(viewPort)
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .addUseCase(videoCapture)
                .build()
            provider.bindToLifecycle(lifecycleOwner, selector, group)
        } else {
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture, videoCapture)
        }
    }.getOrNull()
    if (camera != null) onBound(imageCapture, videoCapture, camera)
}
