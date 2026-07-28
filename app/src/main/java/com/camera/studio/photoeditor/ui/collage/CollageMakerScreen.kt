package com.camera.studio.photoeditor.ui.collage

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.camera.studio.photoeditor.ui.shared.collage.CollageLayout
import com.camera.studio.photoeditor.ui.shared.collage.CollageLayoutPicker
import com.camera.studio.photoeditor.ui.shared.editor.BackgroundPicker
import com.camera.studio.photoeditor.ui.shared.editor.BackgroundState
import com.camera.studio.photoeditor.ui.shared.editor.DrawCanvas
import com.camera.studio.photoeditor.ui.shared.editor.DrawStroke
import com.camera.studio.photoeditor.ui.shared.editor.DrawToolPanel
import com.camera.studio.photoeditor.ui.shared.editor.DraggableLayerHost
import com.camera.studio.photoeditor.ui.shared.editor.EditorLayer
import com.camera.studio.photoeditor.ui.shared.editor.FramePicker
import com.camera.studio.photoeditor.ui.shared.editor.MediaPickerScreen
import com.camera.studio.photoeditor.ui.shared.editor.RatioOption
import com.camera.studio.photoeditor.ui.shared.editor.RatioPicker
import com.camera.studio.photoeditor.ui.shared.editor.StickerLayerContent
import com.camera.studio.photoeditor.ui.shared.editor.StickerPicker
import com.camera.studio.photoeditor.ui.shared.editor.TextLayerContent
import com.camera.studio.photoeditor.ui.shared.editor.TextToolbar
import com.camera.studio.photoeditor.ui.shared.editor.ToolPanelScaffold
import com.camera.studio.photoeditor.ui.shared.editor.withTransform
import com.camera.studio.photoeditor.ui.shared.filter.FilterOption
import com.camera.studio.photoeditor.ui.shared.filter.FilterPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class CollageTool { NONE, LAYOUT, RATIO, MARGIN, BACKGROUND, FRAME, TEXT, FILTER, STICKER, ADD, DRAW }

/** 3 preset khoảng cách/viền giữa các ảnh trong collage (docs: "Collage Margin 3 preset"). */
private enum class MarginOption(val label: String, val gapDp: Int) {
    BORDERLESS("Borderless", 0),
    THIN("Thin", 6),
    THICK("Thick", 16),
}

private fun defaultLayoutFor(count: Int): CollageLayout = when (count) {
    0, 1 -> CollageLayout.SINGLE
    2 -> CollageLayout.TWO_VERTICAL
    3 -> CollageLayout.THREE_VERTICAL
    4 -> CollageLayout.FOUR_GRID
    else -> CollageLayout.NINE_GRID
}

/** Collage Maker — Layout/Ratio/Background/Frame/Text/Filter/Sticker/Add/Draw, khớp mockup Board 4. */
@Composable
fun CollageMakerScreen(
    initialPaths: List<String>,
    onBack: () -> Unit,
    onSaved: (path: String) -> Unit,
    viewModel: CollageViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val photoPaths = remember { mutableStateListOf<String>().apply { addAll(initialPaths) } }
    var photoBitmaps by remember { mutableStateOf<List<Bitmap?>>(emptyList()) }
    LaunchedEffect(photoPaths.toList()) {
        photoBitmaps = withContext(Dispatchers.IO) { photoPaths.map { viewModel.decode(it) } }
    }

    var layout by remember { mutableStateOf(defaultLayoutFor(initialPaths.size)) }
    var ratio by remember { mutableStateOf(RatioOption.R4_3) }
    var margin by remember { mutableStateOf(MarginOption.THIN) }
    var background by remember { mutableStateOf<BackgroundState>(BackgroundState.Blur) }
    var frameAssetPath by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf<FilterOption?>(null) }
    val layers = remember { mutableStateListOf<EditorLayer>() }
    var selectedLayerId by remember { mutableStateOf<Long?>(null) }
    val drawStrokes = remember { mutableStateListOf<DrawStroke>() }
    val redoStack = remember { mutableStateListOf<DrawStroke>() }
    var activeTool by remember { mutableStateOf(CollageTool.NONE) }
    var drawColor by remember { mutableStateOf(Color.White) }
    var drawWidth by remember { mutableStateOf(10f) }
    var drawAlpha by remember { mutableStateOf(1f) }
    var isSaving by remember { mutableStateOf(false) }
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }
    var nextId by remember { mutableLongStateOf(1L) }
    var addSlotIndex by remember { mutableStateOf<Int?>(null) }

    fun exportAndSave() {
        val act = activity ?: return
        if (canvasSizePx.width <= 0) return
        isSaving = true
        scope.launch {
            try {
                val exported = renderComposableToBitmap(act, canvasSizePx.width, canvasSizePx.height, density) {
                    CollageCanvasContent(
                        layout = layout, bitmaps = photoBitmaps, background = background, frameAssetPath = frameAssetPath,
                        marginGapDp = margin.gapDp,
                        filter = selectedFilter, layers = layers, drawStrokes = drawStrokes, selectedLayerId = null,
                        onAddAt = {}, onLayerTransformChange = { _, _ -> }, onLayerTap = {}, onLayerDelete = {}, onLayerDuplicate = {},
                        drawEnabled = false, onStrokeFinished = {}, drawColor = drawColor, drawWidth = drawWidth, drawAlpha = drawAlpha,
                    )
                }
                val path = viewModel.save(exported)
                isSaving = false
                android.widget.Toast.makeText(context, "Đã lưu vào Gallery", android.widget.Toast.LENGTH_SHORT).show()
                onSaved(path)
            } catch (e: Exception) {
                isSaving = false
                android.widget.Toast.makeText(context, "Lưu collage thất bại, thử lại", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (addSlotIndex != null) {
        MediaPickerScreen(
            title = "Add photo",
            multiSelect = false,
            onBack = { addSlotIndex = null },
            onConfirm = { picked ->
                val slot = addSlotIndex!!
                val p = picked.first()
                if (slot < photoPaths.size) photoPaths[slot] = p else photoPaths.add(p)
                addSlotIndex = null
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
            Icon(
                Icons.Default.Close, contentDescription = null, tint = Color.White,
                modifier = Modifier.clickable { if (activeTool != CollageTool.NONE) activeTool = CollageTool.NONE else onBack() },
            )
            Text("Collage Maker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Box(Modifier.clip(CircleShape).background(Color(0xFFF5C518)).clickable(enabled = !isSaving) { exportAndSave() }.padding(horizontal = 18.dp, vertical = 8.dp)) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            Box(
                Modifier.align(Alignment.Center).padding(12.dp).aspectRatio(ratio.widthRatio / ratio.heightRatio).fillMaxWidth().onSizeChanged { canvasSizePx = it },
            ) {
                CollageCanvasContent(
                    layout = layout, bitmaps = photoBitmaps, background = background, frameAssetPath = frameAssetPath,
                    marginGapDp = margin.gapDp,
                    filter = selectedFilter, layers = layers, drawStrokes = drawStrokes, selectedLayerId = selectedLayerId,
                    onAddAt = { i -> addSlotIndex = i },
                    onLayerTransformChange = { id, t -> val idx = layers.indexOfFirst { it.id == id }; if (idx >= 0) layers[idx] = layers[idx].withTransform(t) },
                    onLayerTap = { id -> selectedLayerId = id; if (layers.find { it.id == id } is EditorLayer.TextLayer) activeTool = CollageTool.TEXT },
                    onLayerDelete = { id -> layers.removeAll { it.id == id }; if (selectedLayerId == id) selectedLayerId = null },
                    onLayerDuplicate = { id ->
                        layers.find { it.id == id }?.let { orig ->
                            val newId = nextId++
                            val t = orig.transform.copy(offsetX = orig.transform.offsetX + 24f, offsetY = orig.transform.offsetY + 24f)
                            val copy = if (orig is EditorLayer.TextLayer) orig.copy(id = newId) else (orig as EditorLayer.StickerLayer).copy(id = newId)
                            layers.add(copy.withTransform(t))
                        }
                    },
                    drawEnabled = activeTool == CollageTool.DRAW,
                    onStrokeFinished = { drawStrokes.add(it); redoStack.clear() },
                    drawColor = drawColor, drawWidth = drawWidth, drawAlpha = drawAlpha,
                )
            }
            if (isSaving) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }

            // Bottom-sheet overlay: panel công cụ ĐÈ LÊN canvas — chỉ 1 trong 2 (thanh công cụ chính
            // HOẶC panel đang mở) hiển thị tại 1 thời điểm, không đẩy/co canvas ở trên.
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                when (activeTool) {
                    CollageTool.NONE -> Row(
                        Modifier.fillMaxWidth().background(Color(0xFF0A0B0D)).horizontalScroll(rememberScrollState()).padding(vertical = 10.dp, horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        ToolLabel(R.drawable.ic_collage_maker_layout, "Layout", false) { activeTool = CollageTool.LAYOUT }
                        ToolLabel(R.drawable.ic_collage_maker_ratio, "Ratio", false) { activeTool = CollageTool.RATIO }
                        ToolLabel(R.drawable.ic_collage_margin2, "Margin", false) { activeTool = CollageTool.MARGIN }
                        ToolLabel(R.drawable.ic_collage_maker_background, "Background", false) { activeTool = CollageTool.BACKGROUND }
                        ToolLabel(R.drawable.ic_collage_maker_frame, "Frame", false) { activeTool = CollageTool.FRAME }
                        ToolLabel(R.drawable.ic_collage_maker_text, "Text", false) {
                            if (layers.none { it.id == selectedLayerId && it is EditorLayer.TextLayer }) {
                                val id = nextId++; layers.add(EditorLayer.TextLayer(id = id, text = "Text", transform = com.camera.studio.photoeditor.ui.shared.editor.centerTransform(canvasSizePx))); selectedLayerId = id
                            }
                            activeTool = CollageTool.TEXT
                        }
                        ToolLabel(R.drawable.ic_collage_maker_filter, "Filter", false) { activeTool = CollageTool.FILTER }
                        ToolLabel(R.drawable.ic_collage_maker_sticker, "Sticker", false) { activeTool = CollageTool.STICKER }
                        ToolLabel(R.drawable.ic_collage_maker_add, "Add", false) { addSlotIndex = photoPaths.size }
                        ToolLabel(R.drawable.ic_collage_maker_draw, "Draw", false) { activeTool = CollageTool.DRAW }
                    }
                    CollageTool.LAYOUT -> ToolPanelScaffold("Layout", onClose = { activeTool = CollageTool.NONE }) {
                        CollageLayoutPicker(selected = layout, onSelect = { layout = it }, modifier = Modifier.padding(12.dp))
                    }
                    CollageTool.RATIO -> ToolPanelScaffold("Ratio", onClose = { activeTool = CollageTool.NONE }) {
                        RatioPicker(selected = ratio, onSelect = { ratio = it })
                    }
                    CollageTool.MARGIN -> ToolPanelScaffold("Margin", onClose = { activeTool = CollageTool.NONE }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            val icons = mapOf(MarginOption.BORDERLESS to R.drawable.ic_collage_margin1, MarginOption.THIN to R.drawable.ic_collage_margin2, MarginOption.THICK to R.drawable.ic_collage_margin3)
                            MarginOption.entries.forEach { opt ->
                                val active = margin == opt
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { margin = opt }) {
                                    Icon(androidx.compose.ui.res.painterResource(icons.getValue(opt)), contentDescription = null, tint = if (active) Color(0xFFF5C518) else Color.White, modifier = Modifier.size(28.dp))
                                    Text(opt.label, color = if (active) Color(0xFFF5C518) else Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                    CollageTool.BACKGROUND -> ToolPanelScaffold("Background", onClose = { activeTool = CollageTool.NONE }) {
                        BackgroundPicker(onSelect = { background = it }, onPickImage = { addSlotIndex = null })
                    }
                    CollageTool.FRAME -> ToolPanelScaffold("Frame", onClose = { activeTool = CollageTool.NONE }) {
                        FramePicker(selectedAssetPath = frameAssetPath, onSelect = { frameAssetPath = it })
                    }
                    CollageTool.TEXT -> (layers.find { it.id == selectedLayerId } as? EditorLayer.TextLayer)?.let { layer ->
                        TextToolbar(
                            layer = layer,
                            onChange = { updated -> val idx = layers.indexOfFirst { it.id == layer.id }; if (idx >= 0) layers[idx] = updated },
                            onDelete = { layers.removeAll { it.id == layer.id }; selectedLayerId = null; activeTool = CollageTool.NONE },
                            showArc = true,
                            modifier = Modifier.background(Color(0xFF15161A)),
                        )
                    }
                    CollageTool.FILTER -> ToolPanelScaffold("Filter", onClose = { activeTool = CollageTool.NONE }) {
                        FilterPicker(selected = selectedFilter, onSelect = { selectedFilter = it }, previewBitmap = photoBitmaps.firstOrNull(), modifier = Modifier.padding(vertical = 8.dp))
                    }
                    CollageTool.STICKER -> ToolPanelScaffold("Stickers", onClose = { activeTool = CollageTool.NONE }) {
                        StickerPicker(onPick = { path -> layers.add(EditorLayer.StickerLayer(id = nextId++, assetPath = path, transform = com.camera.studio.photoeditor.ui.shared.editor.centerTransform(canvasSizePx))) })
                    }
                    CollageTool.ADD -> {}
                    CollageTool.DRAW -> ToolPanelScaffold("Draw", onClose = { activeTool = CollageTool.NONE }) {
                        DrawToolPanel(
                            color = drawColor, onColorChange = { drawColor = it }, widthPx = drawWidth, onWidthChange = { drawWidth = it },
                            alpha = drawAlpha, onAlphaChange = { drawAlpha = it }, canUndo = drawStrokes.isNotEmpty(), canRedo = redoStack.isNotEmpty(),
                            onUndo = { if (drawStrokes.isNotEmpty()) redoStack.add(drawStrokes.removeAt(drawStrokes.lastIndex)) },
                            onRedo = { if (redoStack.isNotEmpty()) drawStrokes.add(redoStack.removeAt(redoStack.lastIndex)) },
                            onDone = { activeTool = CollageTool.NONE },
                        )
                    }
                }
            }
        }
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
private fun CollageCanvasContent(
    layout: CollageLayout,
    bitmaps: List<Bitmap?>,
    background: BackgroundState,
    frameAssetPath: String?,
    marginGapDp: Int,
    filter: FilterOption?,
    layers: List<EditorLayer>,
    drawStrokes: List<DrawStroke>,
    selectedLayerId: Long?,
    onAddAt: (Int) -> Unit,
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
        when (background) {
            is BackgroundState.Blur -> bitmaps.firstOrNull()?.let {
                Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(18.dp))
            }
            is BackgroundState.SolidColor -> Box(Modifier.fillMaxSize().background(background.color))
            is BackgroundState.TemplateImage -> AsyncImage(model = AssetCatalog.assetUri(background.assetPath), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            is BackgroundState.UploadedImage -> AsyncImage(model = background.uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxSize().padding(marginGapDp.dp)) {
            CollageGrid(layout = layout, bitmaps = bitmaps, filter = filter, onAddAt = onAddAt, gapDp = marginGapDp)
        }
        frameAssetPath?.let { path -> AsyncImage(model = AssetCatalog.assetUri(path), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize()) }
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
