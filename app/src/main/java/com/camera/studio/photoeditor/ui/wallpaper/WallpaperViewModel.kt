package com.camera.studio.photoeditor.ui.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.camera.studio.photoeditor.data.media.MediaStoreSaver
import com.camera.studio.photoeditor.data.repo.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class WallpaperTarget { HOME, LOCK, BOTH }

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    suspend fun save(bitmap: Bitmap): String {
        val fileName = "wallpaper_${System.currentTimeMillis()}.jpg"
        val file = File(appContext.filesDir, fileName)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val contentUri = MediaStoreSaver.saveImageToGallery(appContext, bitmap, fileName.removeSuffix(".jpg"))
        mediaRepository.addMedia(file.absolutePath, date, MediaRepository.TYPE_WALLPAPER, contentUri?.toString())
        return file.absolutePath
    }

    fun setAsWallpaper(bitmap: Bitmap, target: WallpaperTarget) {
        val manager = WallpaperManager.getInstance(appContext)
        val flag = when (target) {
            WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
            WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
            WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }
        runCatching { manager.setBitmap(bitmap, null, true, flag) }
    }
}
