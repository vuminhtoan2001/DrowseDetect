package com.camera.studio.photoeditor.ui.importphoto

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.camera.studio.photoeditor.data.repo.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ImportPhotoResultViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    suspend fun deleteByPath(path: String) = mediaRepository.deleteByPath(path)

    suspend fun setFavorite(path: String, isFavorite: Boolean) = mediaRepository.setFavorite(path, isFavorite)

    val allMedia = mediaRepository.allMedia

    /** Lưu bản sao vào MediaStore công khai (Pictures/Movies) — truy cập được từ ứng dụng Ảnh/Gallery hệ thống. */
    fun downloadToPublicStorage(path: String, isVideo: Boolean): Boolean = runCatching {
        val file = File(path)
        val resolver = appContext.contentResolver
        val collection: android.net.Uri
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(if (isVideo) MediaStore.Video.Media.RELATIVE_PATH else MediaStore.Images.Media.RELATIVE_PATH, if (isVideo) "Movies/CameraStudio" else "Pictures/CameraStudio")
            }
        }
        collection = if (isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = resolver.insert(collection, values) ?: return@runCatching false
        resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
        true
    }.getOrDefault(false)
}
