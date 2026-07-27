package com.camera.studio.photoeditor.ui.collage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
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
class CollageViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    fun decode(path: String): Bitmap? = runCatching {
        if (path.startsWith("file:///android_asset/")) {
            appContext.assets.open(path.removePrefix("file:///android_asset/")).use { BitmapFactory.decodeStream(it) }
        } else {
            BitmapFactory.decodeFile(path)
        }
    }.getOrNull()

    suspend fun save(bitmap: Bitmap): String {
        val file = File(appContext.filesDir, "collage_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        mediaRepository.addMedia(file.absolutePath, date, MediaRepository.TYPE_COLLAGE)
        return file.absolutePath
    }
}
