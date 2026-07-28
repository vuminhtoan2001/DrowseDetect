package com.camera.studio.photoeditor.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Port 1:1 bảng `images` của RN (`config/dbSql/table/imagesService.js`). */
@Entity(tableName = "saved_media", indices = [androidx.room.Index(value = ["path"], unique = true)])
data class SavedMedia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "date") val date: String,
    /** 0 = image, 1 = video, 2 = collage, 3 = wallpaper (xem MediaRepository.TYPE_*). */
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "is_favorite", defaultValue = "0") val isFavorite: Boolean = false,
    /** Uri MediaStore (`content://...`) nếu media đã lưu công khai vào Gallery — null nếu chỉ có bản nội bộ. */
    @ColumnInfo(name = "content_uri") val contentUri: String? = null,
)
