package com.camera.studio.photoeditor.ui.editphoto

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
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
import com.camera.studio.photoeditor.ui.shared.editor.DrawCanvas
import com.camera.studio.photoeditor.ui.shared.editor.DrawStroke
import com.camera.studio.photoeditor.ui.shared.editor.DrawToolPanel
import com.camera.studio.photoeditor.ui.shared.editor.DraggableLayerHost
import com.camera.studio.photoeditor.ui.shared.editor.EditorLayer
import com.camera.studio.photoeditor.ui.shared.editor.LayerTransform
import com.camera.studio.photoeditor.ui.shared.editor.centerTransform
import com.camera.studio.photoeditor.ui.shared.editor.withTransform
import com.camera.studio.photoeditor.ui.shared.editor.FramePicker
import com.camera.studio.photoeditor.ui.shared.editor.StickerLayerContent
import com.camera.studio.photoeditor.ui.shared.editor.StickerPicker
import com.camera.studio.photoeditor.ui.shared.editor.TextLayerContent
import com.camera.studio.photoeditor.ui.shared.editor.TextToolbar
import com.camera.studio.photoeditor.ui.shared.editor.ToolPanelScaffold
import com.camera.studio.photoeditor.ui.shared.filter.FilterOption
import com.camera.studio.photoeditor.ui.shared.filter.FilterPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class EditPhotoTool { NONE, FRAME, TEXT, FILTER, STICKER, REMOVEBG, DRAW }

/**
 * Edit Photo — Frame/Text/Filter/Sticker/Remove Background/Draw + Rotate, khớp mockup Board 3.
 * Export bake tại ĐÚNG kích thước canvas hiển thị trên màn (không phải độ phân giải ảnh gốc) để vị
 * trí layer/nét vẽ khớp 100% những gì thấy trên preview — đơn giản & chính xác hơn scale-density.
 */
@Composable
fun EditPhotoScreen(
    sourcePath: String,
    onBack: () -> Unit,
    onSaved: (path: String) -> Unit,
    viewModel: EditPhotoViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(sourcePath) {
        baseBitmap = withContext(Dispatchers.IO) { viewModel.decodeSource(sourcePath) }
    }

    var frameAssetPath by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf<FilterOption?>(null) }
    val layers = remember { mutableStateListOf<EditorLayer>() }
    var selectedLayerId by remember { mutableStateOf<Long?>(null) }
    val drawStrokes = remember { mutableStateListOf<DrawStroke>() }
    val redoStack = remember { mutableStateListOf<DrawStroke>() }
    var activeTool by remember { mutableStateOf(EditPhotoTool.NONE) }
    var drawColor by remember { mutableStateOf(Color.White) }
    var drawWidth by remember { mutableStateOf(10f) }
    var drawAlpha by remember { mutableStateOf(1f) }
    var isRemovingBg by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }
    var nextId by remember { mutableLongStateOf(1L) }

    fun addTextLayer() {
        val id = nextId++
        layers.add(EditorLayer.TextLayer(id = id, text = "Text", transform = centerTransform(canvasSizePx)))
        selectedLayerId = id
        activeTool = EditPhotoTool.TEXT
    }

    fun rotateBase() {
        val bmp = baseBitmap ?: return
        val m = Matrix().apply { postRotate(90f) }
        baseBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    fun exportAndSave() {
        val act = activity ?: return
        val bmp = baseBitmap ?: return
        if (canvasSizePx.width <= 0) return
        isSaving = true
        scope.launch {
            try {
                val exported = renderComposableToBitmap(act, canvasSizePx.width, canvasSizePx.height, density) {
                    EditPhotoCanvasContent(
                        baseBitmap = bmp, filter = selectedFilter, frameAssetPath = frameAssetPath,
                        layers = layers, drawStrokes = drawStrokes, selectedLayerId = null,
                        onLayerTransformChange = { _, _ -> }, onLayerTap = {}, onLayerDelete = {}, onLayerDuplicate = {},
                        drawEnabled = false, onStrokeFinished = {}, drawColor = drawColor, drawWidth = drawWidth, drawAlpha = drawAlpha,
                    )
                }
                val path = viewModel.save(exported)
                isSaving = false
                android.widget.Toast.makeText(context, "Đã lưu vào Gallery", android.widget.Toast.LENGTH_SHORT).show()
                onSaved(path)
            } catch (e: Exception) {
                isSaving = false
                android.widget.Toast.makeText(context, "Lưu ảnh thất bại, thử lại", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 22.dp, start = 12.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.clickable {
                    if (activeTool != EditPhotoTool.NONE) { activeTool = EditPhotoTool.NONE; selectedLayerId = null } else onBack()
                },
            )
            Box(
                Modifier.clip(CircleShape).background(Color(0xFFF5C518)).clickable(enabled = !isSaving) { exportAndSave() }.padding(horizontal = 18.dp, vertical = 8.dp),
            ) { Text("Save", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }

        Box(
            Modifier.fillMaxWidth().weight(1f).onSizeChanged { canvasSizePx = it },
            contentAlignment = Alignment.Center,
        ) {
            baseBitmap?.let { bmp ->
                EditPhotoCanvasContent(
                    baseBitmap = bmp, filter = selectedFilter, frameAssetPath = frameAssetPath,
                    layers = layers, drawStrokes = drawStrokes, selectedLayerId = selectedLayerId,
                    onLayerTransformChange = { id, t ->
                        val idx = layers.indexOfFirst { it.id == id }
                        if (idx >= 0) layers[idx] = layers[idx].withTransform(t)
                    },
                    onLayerTap = { id -> selectedLayerId = id; if (layers.find { it.id == id } is EditorLayer.TextLayer) activeTool = EditPhotoTool.TEXT },
                    onLayerDelete = { id -> layers.removeAll { it.id == id }; if (selectedLayerId == id) selectedLayerId = null },
                    onLayerDuplicate = { id ->
                        layers.find { it.id == id }?.let { orig ->
                            val newId = nextId++
                            val offsetTransform = orig.transform.copy(offsetX = orig.transform.offsetX + 24f, offsetY = orig.transform.offsetY + 24f)
                            val copy = if (orig is EditorLayer.TextLayer) orig.copy(id = newId) else (orig as EditorLayer.StickerLayer).copy(id = newId)
                            layers.add(copy.withTransform(offsetTransform))
                        }
                    },
                    drawEnabled = activeTool == EditPhotoTool.DRAW,
                    onStrokeFinished = { drawStrokes.add(it); redoStack.clear() },
                    drawColor = drawColor, drawWidth = drawWidth, drawAlpha = drawAlpha,
                )
            }
            if (isRemovingBg || isSaving) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            // Bottom-sheet overlay: panel công cụ ĐÈ LÊN ảnh (không đẩy/co canvas ở trên) — chỉ 1
            // trong 2 (thanh công cụ chính HOẶC panel đang mở) hiển thị tại 1 thời điểm.
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                when (activeTool) {
                    EditPhotoTool.NONE -> Row(
                        Modifier.fillMaxWidth().background(Color(0xFF0A0B0D)).padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ToolButton(R.drawable.ic_edit_frame, "Frame", false) { activeTool = EditPhotoTool.FRAME }
                        ToolButton(R.drawable.ic_edit_text, "Text", false) { if (layers.none { it.id == selectedLayerId && it is EditorLayer.TextLayer }) addTextLayer() else activeTool = EditPhotoTool.TEXT }
                        ToolButton(R.drawable.ic_edit_filter, "Filter", false) { activeTool = EditPhotoTool.FILTER }
                        ToolButton(R.drawable.ic_edit_sticker, "Sticker", false) { activeTool = EditPhotoTool.STICKER }
                        ToolButton(R.drawable.ic_edit_rotation, "Rotate", false) { rotateBase() }
                        ToolButton(R.drawable.ic_edit_removebg, "RemoveBG", isRemovingBg) {
                            val bmp = baseBitmap ?: return@ToolButton
                            scope.launch {
                                isRemovingBg = true
                                viewModel.removeBackground(bmp).onSuccess { baseBitmap = it }
                                isRemovingBg = false
                            }
                        }
                        ToolButton(R.drawable.ic_edit_draw, "Draw", false) { activeTool = EditPhotoTool.DRAW }
                    }
                    EditPhotoTool.FRAME -> ToolPanelScaffold("Frame", onClose = { activeTool = EditPhotoTool.NONE }) {
                        FramePicker(selectedAssetPath = frameAssetPath, onSelect = { frameAssetPath = it })
                    }
                    EditPhotoTool.TEXT -> (layers.find { it.id == selectedLayerId } as? EditorLayer.TextLayer)?.let { layer ->
                        TextToolbar(
                            layer = layer,
                            onChange = { updated -> val idx = layers.indexOfFirst { it.id == layer.id }; if (idx >= 0) layers[idx] = updated },
                            onDelete = { layers.removeAll { it.id == layer.id }; selectedLayerId = null; activeTool = EditPhotoTool.NONE },
                            showArc = false,
                            modifier = Modifier.background(Color(0xFF15161A)),
                        )
                    }
                    EditPhotoTool.FILTER -> ToolPanelScaffold("Filter", onClose = { activeTool = EditPhotoTool.NONE }) {
                        FilterPicker(selected = selectedFilter, onSelect = { selectedFilter = it }, previewBitmap = baseBitmap, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    EditPhotoTool.STICKER -> ToolPanelScaffold("Stickers", onClose = { activeTool = EditPhotoTool.NONE }) {
                        StickerPicker(onPick = { path -> layers.add(EditorLayer.StickerLayer(id = nextId++, assetPath = path, transform = centerTransform(canvasSizePx))) })
                    }
                    EditPhotoTool.DRAW -> ToolPanelScaffold("Draw", onClose = { activeTool = EditPhotoTool.NONE }) {
                        DrawToolPanel(
                            color = drawColor, onColorChange = { drawColor = it },
                            widthPx = drawWidth, onWidthChange = { drawWidth = it },
                            alpha = drawAlpha, onAlphaChange = { drawAlpha = it },
                            canUndo = drawStrokes.isNotEmpty(), canRedo = redoStack.isNotEmpty(),
                            onUndo = { if (drawStrokes.isNotEmpty()) redoStack.add(drawStrokes.removeAt(drawStrokes.lastIndex)) },
                            onRedo = { if (redoStack.isNotEmpty()) drawStrokes.add(redoStack.removeAt(redoStack.lastIndex)) },
                            onDone = { activeTool = EditPhotoTool.NONE },
                        )
                    }
                    EditPhotoTool.REMOVEBG -> {}
                }
            }
        }
    }
}

@Composable
private fun EditPhotoCanvasContent(
    baseBitmap: Bitmap,
    filter: FilterOption?,
    frameAssetPath: String?,
    layers: List<EditorLayer>,
    drawStrokes: List<DrawStroke>,
    selectedLayerId: Long?,
    onLayerTransformChange: (Long, LayerTransform) -> Unit,
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
        Image(
            bitmap = baseBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = filter?.matrix?.let { ColorFilter.colorMatrix(ComposeColorMatrix(it)) },
            modifier = Modifier.fillMaxSize(),
        )
        DrawCanvas(
            strokes = drawStrokes, currentColor = drawColor, currentWidthPx = drawWidth, currentAlpha = drawAlpha,
            enabled = drawEnabled, onStrokeFinished = onStrokeFinished,
        )
        frameAssetPath?.let { path ->
            AsyncImage(model = AssetCatalog.assetUri(path), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        }
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
private fun ToolButton(iconRes: Int, label: String, active: Boolean, onClick: () -> Unit) {
    val tint = if (active) Color(0xFFF5C518) else Color.White
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Icon(androidx.compose.ui.res.painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, color = tint, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
    }
}
