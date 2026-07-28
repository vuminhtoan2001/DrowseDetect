package com.camera.studio.photoeditor.ui.importphoto

import android.content.Context
import com.camera.studio.photoeditor.data.media.MediaStoreSaver
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

    suspend fun deleteMedia(path: String) = mediaRepository.deleteMedia(path)

    suspend fun setFavorite(path: String, isFavorite: Boolean) = mediaRepository.setFavorite(path, isFavorite)

    val allMedia = mediaRepository.allMedia

    /**
     * Lưu bản sao vào MediaStore công khai (Pictures/Movies/CameraStudio) — truy cập được từ ứng
     * dụng Ảnh/Gallery hệ thống. Ghi lại `content_uri` vào DB để lần xoá sau xoá luôn bản công khai
     * này (tránh mồ côi — xem [MediaRepository.deleteMedia]).
     */
    suspend fun downloadToPublicStorage(path: String, isVideo: Boolean): Boolean {
        val file = File(path)
        val uri = if (isVideo) {
            MediaStoreSaver.saveVideoToGallery(appContext, file, file.name)
        } else {
            val bitmap = android.graphics.BitmapFactory.decodeFile(path) ?: return false
            MediaStoreSaver.saveImageToGallery(appContext, bitmap, file.nameWithoutExtension)
        }
        uri ?: return false
        mediaRepository.setContentUri(path, uri.toString())
        return true
    }
}
