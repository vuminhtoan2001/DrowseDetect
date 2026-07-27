package com.camera.studio.photoeditor.data.repo

import com.camera.studio.photoeditor.data.db.SavedMedia
import com.camera.studio.photoeditor.data.db.SavedMediaDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Port `imagesService.js` (RN) — ảnh/video đã lưu (chụp mới, quay video, edit từ gallery). */
@Singleton
class MediaRepository @Inject constructor(
    private val dao: SavedMediaDao,
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

    suspend fun addMedia(path: String, date: String, type: Int): Long =
        dao.insert(SavedMedia(path = path, date = date, type = type))

    suspend fun latest(): SavedMedia? = dao.getLatest()

    suspend fun latestByType(type: Int): SavedMedia? = dao.getLatestByType(type)

    suspend fun deleteByPath(path: String) = dao.deleteByPath(path)

    suspend fun setFavorite(path: String, isFavorite: Boolean) = dao.setFavorite(path, isFavorite)

    val favorites: Flow<List<SavedMedia>> = dao.observeFavorites()
}
