package com.camera.studio.photoeditor.data.repo

import android.content.Context
import com.camera.studio.photoeditor.data.db.SavedMedia
import com.camera.studio.photoeditor.data.db.SavedMediaDao
import com.camera.studio.photoeditor.data.media.MediaStoreSaver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Port `imagesService.js` (RN) — ảnh/video đã lưu (chụp mới, quay video, edit từ gallery). */
@Singleton
class MediaRepository @Inject constructor(
    private val dao: SavedMediaDao,
    @ApplicationContext private val appContext: Context,
) {
    companion object {
        const val TYPE_IMAGE = 0
        const val TYPE_VIDEO = 1
        /** Ảnh ghép từ Collage Maker (hoặc chụp-live-thành-collage ở Camera) — tab "Templates" ở Gallery. */
        const val TYPE_COLLAGE = 2
        /** Ảnh tạo từ Wallpaper Creator — cũng hiện ở tab "Templates" ở Gallery. */
        const val TYPE_WALLPAPER = 3
    }

    val allMedia: Flow<List<SavedMedia>> = dao.observeAll()

    fun mediaByType(type: Int): Flow<List<SavedMedia>> = dao.observeByType(type)

    fun countByType(type: Int): Flow<Int> = dao.observeCountByType(type)

    suspend fun addMedia(path: String, date: String, type: Int, contentUri: String? = null): Long =
        dao.insert(SavedMedia(path = path, date = date, type = type, contentUri = contentUri))

    suspend fun latest(): SavedMedia? = dao.getLatest()

    suspend fun latestByType(type: Int): SavedMedia? = dao.getLatestByType(type)

    /** Xoá 1 media: bản MediaStore công khai (nếu có `content_uri`) + file riêng của app + dòng DB. */
    suspend fun deleteMedia(path: String) {
        val media = dao.getByPath(path)
        media?.contentUri?.let { MediaStoreSaver.deleteFromGallery(appContext, it) }
        runCatching { File(path).delete() }
        dao.deleteByPath(path)
    }

    suspend fun setFavorite(path: String, isFavorite: Boolean) = dao.setFavorite(path, isFavorite)

    /** Gán `content_uri` sau khi tạo bản MediaStore muộn hơn lúc lưu ban đầu (vd nút "Download"). */
    suspend fun setContentUri(path: String, contentUri: String) = dao.setContentUri(path, contentUri)

    val favorites: Flow<List<SavedMedia>> = dao.observeFavorites()
}
