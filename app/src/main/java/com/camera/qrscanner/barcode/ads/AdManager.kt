package com.camera.qrscanner.barcode.ads

import android.content.Context
import com.brian.base_iap.utils.FirebaseRemoteConfigUtil
import com.brian.base_iap.utils.IAPUtils
import org.json.JSONArray
import org.json.JSONObject

/**
 * Nguồn duy nhất cho ad-id + cờ bật/tắt (MIGRATE_ADS_TO_LIB §11).
 * - id  : getAdsConfigValue(key)  (remote 'ads_config' → fallback assets/default_ads_config.json)
 * - cờ  : getBoolean("<key>_enable")
 * - premium → id "" / cờ false (ẩn mọi ads).
 */
object AdManager {

    fun getAdUnitId(context: Context, placement: String): String {
        if (IAPUtils.isPremium()) return ""
        return try {
            FirebaseRemoteConfigUtil.getInstance().getAdsConfigValue(placement)
                .ifBlank { assetId(context, placement) }
        } catch (e: Exception) {
            assetId(context, placement)
        }
    }

    fun isAdEnabled(enableKey: String): Boolean {
        if (IAPUtils.isPremium()) return false
        return try {
            FirebaseRemoteConfigUtil.getInstance().getBoolean(enableKey)
        } catch (e: Exception) {
            true
        }
    }

    fun minAdRequestIntervalMs(): Long = 1_000L

    /**
     * Quyết định có hiện inter tại [placement] không (vd "inter_scan", "inter_create", "inter_menu"):
     * premium/tắt → false; còn lại theo tần suất showRatio + maxShowPerDay (kịch bản JS).
     * Gọi hàm này 1 lần tại thời điểm định hiện (nó tự đếm), rồi mới InterAds.show.
     */
    fun shouldShowInter(context: Context, placement: String): Boolean {
        if (IAPUtils.isPremium()) return false
        if (!isAdEnabled("${placement}_enable")) return false
        val frc = FirebaseRemoteConfigUtil.getInstance()
        val showRatio = try { frc.getLong("${placement}_showRatio").toInt() } catch (e: Exception) { 1 }
        val maxShow = try { frc.getLong("${placement}_maxShowPerDay").toInt() } catch (e: Exception) { 0 }
        return AdFrequencyManager.shouldShow(context, showRatio, maxShow, placement)
    }

    /** Default cho positionIntrol nếu remote chưa set (khớp JS). Console có thể override. */
    private const val DEFAULT_POSITION_INTROL = "[[1,22,3,4],[1,2,33,4],[1,2,3,4]]"

    /**
     * positionIntrol (Intro): mảng các biến thể, mỗi biến thể là list vị trí ads.
     * Chọn NGẪU NHIÊN 1 biến thể (A/B như JS). Số N=native inline ở trang N; NN(11/22/33/44)=modal
     * native toàn màn khi rời trang N. Premium → rỗng (không ads).
     */
    fun getSelectedAdPositions(): List<Int> {
        if (IAPUtils.isPremium()) return emptyList()
        val raw = try {
            FirebaseRemoteConfigUtil.getInstance().getString("positionIntrol")
        } catch (e: Exception) {
            ""
        }.ifBlank { DEFAULT_POSITION_INTROL }
        return parsePositions(raw)
    }

    private fun parsePositions(raw: String): List<Int> = try {
        val arr = JSONArray(raw)
        if (arr.length() == 0) {
            emptyList()
        } else {
            // Hỗ trợ cả mảng-của-mảng (nhiều biến thể → chọn 1) lẫn mảng phẳng.
            val chosen = if (arr.opt(0) is JSONArray) {
                arr.getJSONArray((0 until arr.length()).random())
            } else {
                arr
            }
            (0 until chosen.length()).map { chosen.getInt(it) }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun assetId(context: Context, key: String): String = try {
        val json = context.assets.open("default_ads_config.json")
            .bufferedReader().use { it.readText() }
        JSONObject(json).optString(key, "")
    } catch (e: Exception) {
        ""
    }
}
