package com.camera.studio.photoeditor.ui.shared.asset

import android.content.Context
import androidx.compose.ui.geometry.Offset
import org.json.JSONObject

/** 1 khung ảnh trong template — polygon 4 điểm góc (TL,TR,BR,BL), toạ độ PIXEL GỐC theo
 * [TemplateFrameSet.imageWidth]/[TemplateFrameSet.imageHeight] (KHÔNG normalize 0..1) — giữ pixel
 * gốc vì bước hiển thị cần áp đúng công thức cover-fit (ảnh gốc 1572×3408 khác tỉ lệ canvas 9:16). */
data class TemplateFrame(val id: String, val points: List<Offset>)

data class TemplateFrameSet(val imageWidth: Float, val imageHeight: Float, val frames: List<TemplateFrame>) {
    companion object {
        val EMPTY = TemplateFrameSet(1f, 1f, emptyList())
    }
}

/**
 * Đọc vị trí khung ảnh của từng wallpaper template từ
 * `assets/images/templates_v2/template_N/data.json` — polygon 4 điểm góc do dò contour alpha-hole
 * (đường viền trong suốt của frame.webp/background.webp) + kiểm chứng bằng mắt cho cả 20 template,
 * hỗ trợ đúng cả khung nghiêng/lệch phối cảnh (không chỉ hình chữ nhật xoay như bản cũ).
 */
object TemplateFrameLoader {
    private val cache = mutableMapOf<Int, TemplateFrameSet>()

    fun framesFor(context: Context, templateId: Int): TemplateFrameSet =
        cache.getOrPut(templateId) { load(context, templateId) }

    private fun load(context: Context, templateId: Int): TemplateFrameSet = runCatching {
        val path = "images/templates_v2/template_$templateId/data.json"
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        val obj = JSONObject(text)
        val size = obj.getJSONObject("imageSize")
        val w = size.getDouble("width").toFloat()
        val h = size.getDouble("height").toFloat()
        val framesArr = obj.getJSONArray("frames")
        val frames = (0 until framesArr.length()).map { i ->
            val f = framesArr.getJSONObject(i)
            val ptsArr = f.getJSONArray("points")
            val points = (0 until ptsArr.length()).map { j ->
                val p = ptsArr.getJSONObject(j)
                Offset(p.getDouble("x").toFloat(), p.getDouble("y").toFloat())
            }
            TemplateFrame(id = f.get("id").toString(), points = points)
        }
        TemplateFrameSet(imageWidth = w, imageHeight = h, frames = frames)
    }.getOrDefault(TemplateFrameSet.EMPTY)
}
