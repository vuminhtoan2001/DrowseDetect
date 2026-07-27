package com.camera.studio.photoeditor.advertisement

import com.brian.base_iap.utils.IAPUtils
import com.camera.studio.photoeditor.firebase.Remote
import org.json.JSONArray

/**
 * positionIntrol (Intro) — mảng các BIẾN THỂ, mỗi biến thể là danh sách vị trí. Chọn NGẪU NHIÊN
 * 1 biến thể (A/B). Quy ước:
 *   - số N  (1..4)      → native INLINE dưới slide N.
 *   - số NN (11/22/33/44) → native MODAL toàn màn khi RỜI slide N.
 * Premium → rỗng (không ads).
 *
 * `res/xml/config.xml` đặt mặc định `positionIntrol=[]` (quyết định sản phẩm: KHÔNG hiện ad nào
 * trên Intro cho tới khi Remote Config đặt giá trị thật lúc phát hành) — CHỈ fallback về [DEFAULT]
 * khi key thiếu hẳn/blank (Remote Config chưa publish gì), KHÔNG fallback khi giá trị là "[]" hợp lệ
 * (tôn trọng đúng quyết định "tắt hẳn ads Intro" thay vì âm thầm bật lại).
 */
object AdPositions {

    private const val DEFAULT = "[[1,22,3,4],[1,2,33,4],[1,2,3,4]]"

    fun selected(): List<Int> {
        if (IAPUtils.isPremium()) return emptyList()
        val raw = runCatching { Remote.instance.getString("positionIntrol") }.getOrNull()
        if (raw.isNullOrBlank()) return parsePositions(DEFAULT)
        return parsePositions(raw)
    }

    private fun parsePositions(raw: String): List<Int> = try {
        val arr = JSONArray(raw)
        if (arr.length() == 0) emptyList()
        else {
            val chosen = if (arr.opt(0) is JSONArray) arr.getJSONArray((0 until arr.length()).random()) else arr
            (0 until chosen.length()).map { chosen.getInt(it) }
        }
    } catch (e: Exception) { emptyList() }
}
