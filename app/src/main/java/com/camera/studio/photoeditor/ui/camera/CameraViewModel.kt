package com.camera.studio.photoeditor.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camera.studio.photoeditor.data.db.SavedMedia
import com.camera.studio.photoeditor.data.media.MediaStoreSaver
import com.camera.studio.photoeditor.data.repo.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * M0: chỉ giữ phần lưu ảnh/video cốt lõi (Room + file). Control mới (AMP/WB/Filter/More-menu/
 * Resolution/Collage-live-shoot — xem docs/02_CAMERA_TAKE_PHOTO_VIDEO.md) sẽ thêm ở M2.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val latestPhoto: StateFlow<SavedMedia?> = mediaRepository.mediaByType(MediaRepository.TYPE_IMAGE)
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val latestVideo: StateFlow<SavedMedia?> = mediaRepository.mediaByType(MediaRepository.TYPE_VIDEO)
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Lưu ảnh vào file riêng của app + Room + bản công khai MediaStore. Trả về path đã lưu. */
    suspend fun savePhotoBitmap(bitmap: Bitmap): String {
        val fileName = "camera_${System.currentTimeMillis()}.jpg"
        val file = File(appContext.filesDir, fileName)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val contentUri = MediaStoreSaver.saveImageToGallery(appContext, bitmap, fileName.removeSuffix(".jpg"))
        mediaRepository.addMedia(file.absolutePath, date, MediaRepository.TYPE_IMAGE, contentUri?.toString())
        return file.absolutePath
    }

    /**
     * Lưu ảnh ghép từ Camera More-menu → Collage (chụp lần lượt từng phần rồi ghép lại thành 1 ảnh
     * gốc — "photo splitter", không phải collage trang trí) — lưu kind = IMAGE, hiện ở Gallery tab
     * "Photo" như 1 ảnh chụp bình thường (trước đây lưu kind = COLLAGE khiến nó lọt vào tab
     * "Templates" sai chỗ).
     */
    suspend fun saveCollageBitmap(bitmap: Bitmap): String {
        val fileName = "collage_${System.currentTimeMillis()}.jpg"
        val file = File(appContext.filesDir, fileName)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val contentUri = MediaStoreSaver.saveImageToGallery(appContext, bitmap, fileName.removeSuffix(".jpg"))
        mediaRepository.addMedia(file.absolutePath, date, MediaRepository.TYPE_IMAGE, contentUri?.toString())
        return file.absolutePath
    }

    /** [videoFile] là file .mp4 CameraX vừa ghi xong (cacheDir) — sao vào MediaStore rồi đăng ký Room. */
    suspend fun registerSavedVideo(videoFile: File) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val contentUri = MediaStoreSaver.saveVideoToGallery(appContext, videoFile, videoFile.nameWithoutExtension)
        mediaRepository.addMedia(videoFile.absolutePath, date, MediaRepository.TYPE_VIDEO, contentUri?.toString())
    }

    fun decodeBitmap(path: String): Bitmap? = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
}
