package com.camera.qrscanner.barcode.ads

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tần suất hiển thị inter theo showRatio + maxShowPerDay/ngày
 * (port utils/utils.js `shouldShowAd` + reference AdFrequencyManager).
 *
 * - `ad_show_count_{ngày}_{type}`    = số lần GỌI kiểm tra trong ngày.
 * - `ad_show_count_ad_{ngày}_{type}` = số lần THỰC SỰ hiển thị trong ngày.
 * - Hiện khi `check % showRatio == 0` VÀ `shown < maxShowPerDay`.
 * - Chỉ tăng bộ đếm khi `!noCountNext`. Dữ liệu quá 7 ngày sẽ bị dọn.
 */
object AdFrequencyManager {
    private const val PREFS = "ad_frequency"
    private val DATE_RE = Regex("""\d{4}-\d{2}-\d{2}""")

    fun shouldShow(
        context: Context,
        showRatio: Int,
        maxShowPerDay: Int,
        adType: String,
        noCountNext: Boolean = false,
    ): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val today = today()
            cleanupOld(prefs, today)

            val ratio = if (showRatio <= 0) 1 else showRatio
            val checkKey = "ad_show_count_${today}_$adType"
            val shownKey = "ad_show_count_ad_${today}_$adType"
            val checkCount = prefs.getInt(checkKey, 0)
            val shownCount = prefs.getInt(shownKey, 0)

            var check = checkCount % ratio == 0
            if (shownCount >= maxShowPerDay) check = false

            if (!noCountNext) {
                val e = prefs.edit()
                e.putInt(checkKey, checkCount + 1)
                if (check) e.putInt(shownKey, shownCount + 1)
                e.apply()
            }
            check
        } catch (e: Exception) {
            false
        }
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** Xoá key đếm cũ hơn 7 ngày. */
    private fun cleanupOld(prefs: SharedPreferences, today: String) {
        try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayMs = fmt.parse(today)?.time ?: return
            val editor = prefs.edit()
            var removed = false
            for (key in prefs.all.keys) {
                if (!key.startsWith("ad_show_count_")) continue
                val datePart = DATE_RE.find(key)?.value ?: continue
                val ms = try { fmt.parse(datePart)?.time } catch (e: Exception) { null } ?: continue
                if ((todayMs - ms) / 86_400_000L > 7) { editor.remove(key); removed = true }
            }
            if (removed) editor.apply()
        } catch (e: Exception) {
            // bỏ qua
        }
    }
}
