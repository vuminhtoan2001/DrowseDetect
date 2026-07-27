package com.camera.studio.photoeditor.ui.editphoto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import com.camera.studio.photoeditor.core.BitmapUtils
import com.camera.studio.photoeditor.data.removebg.BackgroundRemovalService
import com.camera.studio.photoeditor.data.repo.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class EditPhotoViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val backgroundRemovalService: BackgroundRemovalService,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    /** [path] có thể là URI asset (`file:///android_asset/...`, ảnh Sample) hoặc path file thật (SavedMedia). */
    fun decodeSource(path: String): Bitmap? = runCatching {
        if (path.startsWith("file:///android_asset/")) {
            appContext.assets.open(path.removePrefix("file:///android_asset/")).use { BitmapFactory.decodeStream(it) }
        } else if (path.startsWith("content://")) {
            BitmapUtils.decodeAndRotateFromUri(appContext, android.net.Uri.parse(path))
        } else {
            BitmapFactory.decodeFile(path)
        }
    }.getOrNull()

    suspend fun removeBackground(bitmap: Bitmap): Result<Bitmap> = backgroundRemovalService.removeBackground(bitmap)

    suspend fun save(bitmap: Bitmap): String {
        val file = File(appContext.filesDir, "edit_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        mediaRepository.addMedia(file.absolutePath, date, MediaRepository.TYPE_IMAGE)
        return file.absolutePath
    }
}
