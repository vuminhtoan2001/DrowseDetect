package com.awakedrive.drowsedetect.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cấu hình core dùng chung (gate mở app Intro/Home) — lưu bằng DataStore. Toggle tính năng riêng của
 * app (vibrate/auto-copy/...) khai thêm ở đây khi build tính năng thật, không phải phần khung sườn.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val APP_OPEN_COUNT = intPreferencesKey("app_open_count")   // goToHomeNumber
        val ADS_CAMPAIGN = intPreferencesKey("ads_campaign")       // -1 chưa biết, 0 organic, 1 ads
    }

    /** Tăng số lần mở app (goToHomeNumber) và trả về giá trị mới. Bắt đầu từ 1 ở lần mở đầu. */
    suspend fun incrementAppOpen(): Int {
        var newVal = 1
        dataStore.edit { p ->
            newVal = (p[Keys.APP_OPEN_COUNT] ?: 0) + 1
            p[Keys.APP_OPEN_COUNT] = newVal
        }
        return newVal
    }

    /** Cache kết quả ads/organic (install referrer không đổi) — null nếu chưa xác định. */
    suspend fun cachedAdsCampaign(): Boolean? {
        val v = dataStore.data.first()[Keys.ADS_CAMPAIGN] ?: -1
        return if (v < 0) null else v == 1
    }

    suspend fun setAdsCampaign(value: Boolean) = edit { it[Keys.ADS_CAMPAIGN] = if (value) 1 else 0 }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }
}
