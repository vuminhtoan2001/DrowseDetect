package com.camera.studio.photoeditor.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore by preferencesDataStore(name = "user_settings")

/**
 * Cấu hình cấp app, lưu qua DataStore. Tối giản lại ở M0 (bản gốc vga34b có thêm rất nhiều field
 * GPS/overlay-template/time-format không dùng cho vga52a — xem lịch sử vga34b-kotlin nếu cần đối
 * chiếu). `viewAllDeviceMedia` phục vụ toggle "xem tất cả media máy" ở Gallery (M7).
 *
 * `hasSeenIntro` (M0-M7) đã bỏ ở M8: MainActivity.resolveStartRoute() gate Intro theo
 * goToHomeNumber/isAdsCampaign (AppStorage + InstallReferrerHelper) giống các app khác, không còn
 * dùng cờ "đã xem 1 lần" đơn giản nữa.
 */
data class UserSettings(
    val viewAllDeviceMedia: Boolean = false,
)

class UserSettingsStore(private val context: Context) {

    private object Keys {
        val VIEW_ALL_DEVICE_MEDIA = booleanPreferencesKey("view_all_device_media")
    }

    val settings: Flow<UserSettings> = context.userSettingsDataStore.data.map { p ->
        UserSettings(
            viewAllDeviceMedia = p[Keys.VIEW_ALL_DEVICE_MEDIA] ?: false,
        )
    }

    suspend fun setViewAllDeviceMedia(enabled: Boolean) {
        context.userSettingsDataStore.edit { it[Keys.VIEW_ALL_DEVICE_MEDIA] = enabled }
    }
}
