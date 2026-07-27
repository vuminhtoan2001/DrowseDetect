package com.camera.studio.photoeditor.ui.gallery

import android.content.Context
import android.provider.MediaStore
import android.util.Log

/** Một ảnh/video trên máy — [dateAddedSec] là DATE_ADDED (giây epoch) dùng để group theo ngày. */
data class DeviceMediaItem(val path: String, val dateAddedSec: Long)

/** Truy vấn TOÀN BỘ ảnh/video trên máy (MediaStore) — dùng khi bật toggle "xem tất cả media máy". */
object DeviceMediaQuery {

    fun queryImageItems(context: Context, limit: Int = 300): List<DeviceMediaItem> =
        queryItems(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA, limit)

    fun queryVideoItems(context: Context, limit: Int = 300): List<DeviceMediaItem> =
        queryItems(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DATA, limit)

    fun queryImagePaths(context: Context, limit: Int = 300): List<String> = queryImageItems(context, limit).map { it.path }

    private fun queryItems(context: Context, collection: android.net.Uri, dataColumn: String, limit: Int): List<DeviceMediaItem> {
        val result = mutableListOf<DeviceMediaItem>()
        // KHÔNG chèn "LIMIT n" vào sortOrder — một số OEM (Samsung One UI đã gặp thực tế) validate chuỗi
        // sortOrder chặt và ném IllegalArgumentException("Invalid token LIMIT"), bị runCatching nuốt nên
        // trả về danh sách rỗng dù quyền đã cấp đủ. Giới hạn số dòng bằng vòng lặp Kotlin thay vì SQL.
        val error = runCatching {
            context.contentResolver.query(
                collection,
                arrayOf(dataColumn, MediaStore.MediaColumns.DATE_ADDED),
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC",
            )?.use { cursor ->
                val pathCol = cursor.getColumnIndexOrThrow(dataColumn)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                while (cursor.moveToNext() && result.size < limit) {
                    val path = cursor.getString(pathCol) ?: continue
                    result.add(DeviceMediaItem(path, cursor.getLong(dateCol)))
                }
            }
        }.exceptionOrNull()
        if (error != null) Log.w("DeviceMediaQuery", "queryItems($collection) failed", error)
        return result
    }
}
