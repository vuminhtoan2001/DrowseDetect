package com.camera.studio.photoeditor.ui.wallpaper

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.camera.studio.photoeditor.R
import com.camera.studio.photoeditor.core.renderComposableToBitmap
import com.camera.studio.photoeditor.ui.shared.asset.AssetCatalog
import com.camera.studio.photoeditor.ui.shared.asset.TemplateFrame
import com.camera.studio.photoeditor.ui.shared.asset.TemplateFrameLoader
import com.camera.studio.photoeditor.ui.shared.asset.TemplateFrameSet
import com.camera.studio.photoeditor.ui.shared.asset.centroid
import com.camera.studio.photoeditor.ui.shared.asset.containsPoint
import com.camera.studio.photoeditor.ui.shared.asset.toCanvasPoints
import com.camera.studio.photoeditor.ui.shared.editor.BackgroundPicker
import com.camera.studio.photoeditor.ui.shared.editor.BackgroundState
import com.camera.studio.photoeditor.ui.shared.editor.DrawCanvas
import com.camera.studio.photoeditor.ui.shared.editor.DrawStroke
import com.camera.studio.photoeditor.ui.shared.editor.DrawToolPanel
import com.camera.studio.photoeditor.ui.shared.editor.DraggableLayerHost
import com.camera.studio.photoeditor.ui.shared.editor.EditorLayer
import com.camera.studio.photoeditor.ui.shared.editor.MediaPickerScreen
import com.camera.studio.photoeditor.ui.shared.editor.StickerLayerContent
import com.camera.studio.photoeditor.ui.shared.editor.StickerPicker
import com.camera.studio.photoeditor.ui.shared.editor.TextLayerContent
import com.camera.studio.photoeditor.ui.shared.editor.TextToolbar
import com.camera.studio.photoeditor.ui.shared.editor.ToolPanelScaffold
import com.camera.studio.photoeditor.ui.shared.editor.withTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class WallpaperTool { NONE, ADD, TEXT, BACKGROUND, STICKER, DRAW }

/** Wallpaper Creator editor — đúng 5 tool Add/Text/Background/Sticker/Draw (docs/05). */
@Composable
fun WallpaperEditorScreen(
    templateId: Int,
    onBack: () -> Unit,
    onSaved: (path: String) -> Unit,
    viewModel: WallpaperViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var background by remember {
        mutableStateOf<BackgroundState>(
            if (templateId > 0) BackgroundState.TemplateImage(AssetCatalog.templateBackgroundPath(templateId)) else BackgroundState.SolidColor(Color(0xFF2A2C33)),
        )
    }
    val frameAssetPath = remember { if (templateId > 0) AssetCatalog.templateFramePath(templateId) else null }
    val layers = remember { mutableStateListOf<EditorLayer>() }
    var selectedLayerId by remember { mutableStateOf<Long?>(null) }
    val drawStrokes = remember { mutableStateListOf<DrawStroke>() }
    val redoStack = remember { mutableStateListOf<DrawStroke>() }
    var activeTool by remember { mutableStateOf(WallpaperTool.NONE) }
    var drawColor by remember { mutableStateOf(Color.White) }
    var drawWidth by remember { mutableStateOf(10f) }
    var drawAlpha by remember { mutableStateOf(1f) }
    var isSaving by remember { mutableStateOf(false) }
    var showSetWallpaperDialog by remember { mutableStateOf(false) }
    var savedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }
    var nextId by remember { mutableLongStateOf(1L) }
    var showAddPicker by remember { mutableStateOf(false) }

    // Slot ảnh theo template (quét vùng trong suốt của background.webp) — khớp
    // `wallpaper_example_2_can_edit_image.jpg` (khung + nút "+" ở đúng vị trí lỗ). Rỗng nếu template
    // không có alpha-hole rõ ràng → fallback "Add" tự do như trước.
    var frameSet by remember { mutableStateOf(TemplateFrameSet.EMPTY) }
    val framePhotos = remember { mutableStateMapOf<String, String>() }
    var pendingFrameId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(templateId) {
        frameSet = if (templateId > 0) {
            withContext(Dispatchers.IO) { TemplateFrameLoader.framesFor(context, templateId) }
        } else {
            TemplateFrameSet.EMPTY
        }
        framePhotos.clear()
    }

    fun exportAndSave() {
        val act = activity ?: return
        if (canvasSizePx.width <= 0) return
        isSaving = true
        scope.launch {
            try {
                val exported = renderComposableToBitmap(act, canvasSizePx.width, canvasSizePx.height, density) {
                    WallpaperCanvasContent(
                        background = background, frameAssetPath = frameAssetPath, layers = layers, drawStrokes = drawStrokes,
                        frameSet = frameSet, framePhotos = framePhotos, onFrameTap = {},
                        selectedLayerId = null, onLayerTransformChange = { _, _ -> }, onLayerTap = {}, onLayerDelete = {}, onLayerDuplicate = {},
                        drawEnabled = false, onStrokeFinished = {}, drawColor = drawColor, drawWidth = drawWidth, drawAlpha = drawAlpha,
                    )
                }
                val path = viewModel.save(exported)
                savedBitmap = exported
                isSaving = false
                android.widget.Toast.makeText(context, "Đã lưu vào Gallery", android.widget.Toast.LENGTH_SHORT).show()
                showSetWallpaperDialog = true
                onSaved(path)
            } catch (e: Exception) {
                isSaving = false
                android.widget.Toast.makeText(context, "Lưu wallpaper thất bại, thử lại", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showAddPicker) {
        MediaPickerScreen(
            title = "Add photo",
            multiSelect = false,
            onBack = { showAddPicker = false },
            onConfirm = { picked ->
                layers.add(EditorLayer.StickerLayer(id = nextId++, assetPath = picked.first(), transform = com.camera.studio.photoeditor.ui.shared.editor.centerTransform(canvasSizePx)))
                showAddPicker = false
            },
        )
        return
    }

    if (pendingFrameId != null) {
        MediaPickerScreen(
            title = "Add photo",
            multiSelect = false,
            onBack = { pendingFrameId = null },
            onConfirm = { picked ->
                framePhotos[pendingFrameId!!] = picked.first()
                pendingFrameId = null
            },
        )
        return
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 22.dp, start = 12.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.clickable { if (activeTool != WallpaperTool.NONE) activeTool = WallpaperTool.NONE else onBack() })
            Box(Modifier.clip(CircleShape).background(Color(0xFFF5C518)).clickable(enabled = !isSaving) { exportAndSave() }.padding(horizontal = 18.dp, vertical = 8.dp)) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.aspectRatio(9f / 16f).fillMaxHeight().onSizeChanged { canvasSizePx = it }) {
                WallpaperCanvasContent(
                    background = background, frameAssetPath = frameAssetPath, layers = layers, drawStrokes = drawStrokes,
                    frameSet = frameSet, framePhotos = framePhotos, onFrameTap = { id -> pendingFrameId = id },
                    selectedLayerId = selectedLayerId,
                    onLayerTransformChange = { id, t -> val idx = layers.indexOfFirst { it.id == id }; if (idx >= 0) layers[idx] = layers[idx].withTransform(t) },
                    onLayerTap = { id -> selectedLayerId = id; if (layers.find { it.id == id } is EditorLayer.TextLayer) activeTool = WallpaperTool.TEXT },
                    onLayerDelete = { id -> layers.removeAll { it.id == id }; if (selectedLayerId == id) selectedLayerId = null },
                    onLayerDuplicate = { id ->
                        layers.find { it.id == id }?.let { orig ->
                            val newId = nextId++
                            val t = orig.transform.copy(offsetX = orig.transform.offsetX + 24f, offsetY = orig.transform.offsetY + 24f)
                            val copy = if (orig is EditorLayer.TextLayer) orig.copy(id = newId) else (orig as EditorLayer.StickerLayer).copy(id = newId)
                            layers.add(copy.withTransform(t))
                        }
                    },
                    drawEnabled = activeTool == WallpaperTool.DRAW,
                    onStrokeFinished = { drawStrokes.add(it); redoStack.clear() },
                    drawColor = drawColor, drawWidth = drawWidth, drawAlpha = drawAlpha,
                )
            }
            if (isSaving) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }

            // Bottom-sheet overlay: panel công cụ ĐÈ LÊN canvas — chỉ 1 trong 2 (thanh công cụ chính
            // HOẶC panel đang mở) hiển thị tại 1 thời điểm, không đẩy/co canvas ở trên.
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                when (activeTool) {
                    WallpaperTool.NONE -> Row(Modifier.fillMaxWidth().background(Color(0xFF0A0B0D)).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ToolLabel(R.drawable.ic_add_photo, "Add", false) { showAddPicker = true }
                        ToolLabel(R.drawable.ic_add_text, "Text", false) {
                            if (layers.none { it.id == selectedLayerId && it is EditorLayer.TextLayer }) {
                                val id = nextId++; layers.add(EditorLayer.TextLayer(id = id, text = "Text", transform = com.camera.studio.photoeditor.ui.shared.editor.centerTransform(canvasSizePx))); selectedLayerId = id
                            }
                            activeTool = WallpaperTool.TEXT
                        }
                        ToolLabel(R.drawable.ic_add_background, "Background", false) { activeTool = WallpaperTool.BACKGROUND }
                        ToolLabel(R.drawable.ic_add_sticker, "Sticker", false) { activeTool = WallpaperTool.STICKER }
                        ToolLabel(R.drawable.ic_add_draw, "Draw", false) { activeTool = WallpaperTool.DRAW }
                    }
                    WallpaperTool.ADD -> {}
                    WallpaperTool.BACKGROUND -> ToolPanelScaffold("Background", onClose = { activeTool = WallpaperTool.NONE }) {
                        BackgroundPicker(onSelect = { background = it }, onPickImage = { showAddPicker = true })
                    }
                    WallpaperTool.TEXT -> (layers.find { it.id == selectedLayerId } as? EditorLayer.TextLayer)?.let { layer ->
                        TextToolbar(
                            layer = layer,
                            onChange = { updated -> val idx = layers.indexOfFirst { it.id == layer.id }; if (idx >= 0) layers[idx] = updated },
                            onDelete = { layers.removeAll { it.id == layer.id }; selectedLayerId = null; activeTool = WallpaperTool.NONE },
                            showArc = false,
                            modifier = Modifier.background(Color(0xFF15161A)),
                        )
                    }
                    WallpaperTool.STICKER -> ToolPanelScaffold("Stickers", onClose = { activeTool = WallpaperTool.NONE }) {
                        StickerPicker(onPick = { path -> layers.add(EditorLayer.StickerLayer(id = nextId++, assetPath = path, transform = com.camera.studio.photoeditor.ui.shared.editor.centerTransform(canvasSizePx))) })
                    }
                    WallpaperTool.DRAW -> ToolPanelScaffold("Draw", onClose = { activeTool = WallpaperTool.NONE }) {
                        DrawToolPanel(
                            color = drawColor, onColorChange = { drawColor = it }, widthPx = drawWidth, onWidthChange = { drawWidth = it },
                            alpha = drawAlpha, onAlphaChange = { drawAlpha = it }, canUndo = drawStrokes.isNotEmpty(), canRedo = redoStack.isNotEmpty(),
                            onUndo = { if (drawStrokes.isNotEmpty()) redoStack.add(drawStrokes.removeAt(drawStrokes.lastIndex)) },
                            onRedo = { if (redoStack.isNotEmpty()) drawStrokes.add(redoStack.removeAt(redoStack.lastIndex)) },
                            onDone = { activeTool = WallpaperTool.NONE },
                        )
                    }
                }
            }
        }
    }

    if (showSetWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showSetWallpaperDialog = false },
            title = { Text("Set as Wallpaper?") },
            text = { Text("Đã lưu vào Gallery. Bạn có muốn đặt làm hình nền không?") },
            confirmButton = {
                Row {
                    TextButton(onClick = { savedBitmap?.let { viewModel.setAsWallpaper(it, WallpaperTarget.HOME) }; showSetWallpaperDialog = false }) { Text("Home") }
                    TextButton(onClick = { savedBitmap?.let { viewModel.setAsWallpaper(it, WallpaperTarget.LOCK) }; showSetWallpaperDialog = false }) { Text("Lock") }
                    TextButton(onClick = { savedBitmap?.let { viewModel.setAsWallpaper(it, WallpaperTarget.BOTH) }; showSetWallpaperDialog = false }) { Text("Both") }
                }
            },
            dismissButton = { TextButton(onClick = { showSetWallpaperDialog = false }) { Text("Để sau") } },
        )
    }
}

@Composable
private fun ToolLabel(iconRes: Int, label: String, active: Boolean, onClick: () -> Unit) {
    val tint = if (active) Color(0xFFF5C518) else Color.White
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Icon(androidx.compose.ui.res.painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, color = tint, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun WallpaperCanvasContent(
    background: BackgroundState,
    frameAssetPath: String?,
    layers: List<EditorLayer>,
    drawStrokes: List<DrawStroke>,
    frameSet: TemplateFrameSet,
    framePhotos: Map<String, String>,
    onFrameTap: (String) -> Unit,
    selectedLayerId: Long?,
    onLayerTransformChange: (Long, com.camera.studio.photoeditor.ui.shared.editor.LayerTransform) -> Unit,
    onLayerTap: (Long) -> Unit,
    onLayerDelete: (Long) -> Unit,
    onLayerDuplicate: (Long) -> Unit,
    drawEnabled: Boolean,
    onStrokeFinished: (DrawStroke) -> Unit,
    drawColor: Color,
    drawWidth: Float,
    drawAlpha: Float,
) {
    Box(Modifier.fillMaxSize()) {
        // Thứ tự ĐÚNG: background (ảnh nền trang trí, thường ĐẶC — không phải lỗ trong suốt) → ẢNH
        // SLOT của user → frame (lớp phủ có lỗ trong suốt lộ ra ảnh bên dưới + viền/ruy băng trang
        // trí vẽ đè lên trên). Trước đây vẽ slot-photo DƯỚI CẢ background lẫn frame nên bị background
        // đặc che mất hoàn toàn (đã xác nhận qua phân tích thật: hầu hết template có lỗ trong suốt
        // nằm ở FRAME chứ không phải background — chỉ template 1 (trái tim) là ở background).
        when (background) {
            is BackgroundState.SolidColor -> Box(Modifier.fillMaxSize().background(background.color))
            is BackgroundState.TemplateImage -> AsyncImage(model = AssetCatalog.assetUri(background.assetPath), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            is BackgroundState.UploadedImage -> AsyncImage(model = background.uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            is BackgroundState.Blur -> Box(Modifier.fillMaxSize().background(Color(0xFF2A2C33)))
        }
        if (background is BackgroundState.TemplateImage && frameSet.frames.isNotEmpty()) {
            TemplateFrameLayer(
                frameSet = frameSet,
                framePhotos = framePhotos,
                onFrameTap = onFrameTap,
            )
        }
        frameAssetPath?.let { path -> AsyncImage(model = AssetCatalog.assetUri(path), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        DrawCanvas(strokes = drawStrokes, currentColor = drawColor, currentWidthPx = drawWidth, currentAlpha = drawAlpha, enabled = drawEnabled, onStrokeFinished = onStrokeFinished)
        layers.forEach { layer ->
            key(layer.id) {
                DraggableLayerHost(
                    transform = layer.transform,
                    selected = layer.id == selectedLayerId,
                    onTransformChange = { onLayerTransformChange(layer.id, it) },
                    onTap = { onLayerTap(layer.id) },
                    onDelete = { onLayerDelete(layer.id) },
                    onDuplicate = { onLayerDuplicate(layer.id) },
                ) {
                    when (layer) {
                        is EditorLayer.TextLayer -> TextLayerContent(layer)
                        is EditorLayer.StickerLayer -> StickerLayerContent(layer)
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateFrameLayer(
    frameSet: TemplateFrameSet,
    framePhotos: Map<String, String>,
    onFrameTap: (String) -> Unit,
) {
    val context = LocalContext.current
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSizePx = it }
            .pointerInput(frameSet, canvasSizePx) {
                detectTapGestures { tap ->
                    val canvasWidth = canvasSizePx.width.toFloat()
                    val canvasHeight = canvasSizePx.height.toFloat()
                    val tappedFrame = frameSet.frames.asReversed().firstOrNull { frame ->
                        frame.toCanvasPoints(frameSet.imageWidth, frameSet.imageHeight, canvasWidth, canvasHeight)
                            .containsPoint(tap)
                    }
                    tappedFrame?.let { onFrameTap(it.id) }
                }
            },
    ) {
        frameSet.frames.forEach { frame ->
            val photoPath = framePhotos[frame.id]
            val bitmap = remember(context, photoPath) { photoPath?.let { decodeBitmap(context, it) } }
            Canvas(Modifier.fillMaxSize()) {
                val points = frame.toCanvasPoints(frameSet.imageWidth, frameSet.imageHeight, size.width, size.height)
                if (bitmap != null) {
                    drawPerspectiveBitmap(bitmap, points)
                } else {
                    drawAddButton(points)
                }
            }
        }
    }
}

private fun decodeBitmap(context: android.content.Context, path: String): Bitmap? =
    runCatching {
        when {
            path.startsWith("file:///android_asset/") -> {
                val assetPath = path.removePrefix("file:///android_asset/")
                context.assets.open(assetPath).use(BitmapFactory::decodeStream)
            }
            path.startsWith("images/") -> context.assets.open(path).use(BitmapFactory::decodeStream)
            path.startsWith("file://") -> BitmapFactory.decodeFile(path.removePrefix("file://"))
            else -> BitmapFactory.decodeFile(File(path).absolutePath)
        }
    }.getOrNull()

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPerspectiveBitmap(
    bitmap: Bitmap,
    points: List<Offset>,
) {
    if (points.size < 4 || bitmap.width <= 0 || bitmap.height <= 0) return

    drawIntoCanvas {
        val nativeCanvas = it.nativeCanvas
        val dstBounds = RectF(
            points.minOf { p -> p.x },
            points.minOf { p -> p.y },
            points.maxOf { p -> p.x },
            points.maxOf { p -> p.y },
        )
        val targetAspect = dstBounds.width() / dstBounds.height().coerceAtLeast(1f)
        val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val src = if (bitmapAspect > targetAspect) {
            val cropWidth = bitmap.height * targetAspect
            val left = (bitmap.width - cropWidth) / 2f
            RectF(left, 0f, left + cropWidth, bitmap.height.toFloat())
        } else {
            val cropHeight = bitmap.width / targetAspect
            val top = (bitmap.height - cropHeight) / 2f
            RectF(0f, top, bitmap.width.toFloat(), top + cropHeight)
        }
        val srcPoints = floatArrayOf(src.left, src.top, src.right, src.top, src.right, src.bottom, src.left, src.bottom)
        val dstPoints = floatArrayOf(
            points[0].x, points[0].y,
            points[1].x, points[1].y,
            points[2].x, points[2].y,
            points[3].x, points[3].y,
        )
        val matrix = Matrix().apply { setPolyToPoly(srcPoints, 0, dstPoints, 0, 4) }
        val clipPath = AndroidPath().apply {
            moveTo(points[0].x, points[0].y)
            lineTo(points[1].x, points[1].y)
            lineTo(points[2].x, points[2].y)
            lineTo(points[3].x, points[3].y)
            close()
        }
        nativeCanvas.save()
        nativeCanvas.clipPath(clipPath)
        nativeCanvas.concat(matrix)
        nativeCanvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        nativeCanvas.restore()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAddButton(points: List<Offset>) {
    if (points.isEmpty()) return
    val center = points.centroid()
    drawIntoCanvas {
        val canvas = it.nativeCanvas
        val radius = 18.dp.toPx()
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(220, 255, 255, 255)
        }
        val plusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 4.dp.toPx()
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawCircle(center.x, center.y, radius, circlePaint)
        canvas.drawLine(center.x - radius * 0.45f, center.y, center.x + radius * 0.45f, center.y, plusPaint)
        canvas.drawLine(center.x, center.y - radius * 0.45f, center.x, center.y + radius * 0.45f, plusPaint)
    }
}
