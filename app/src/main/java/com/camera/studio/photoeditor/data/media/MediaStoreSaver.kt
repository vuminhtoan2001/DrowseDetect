package com.camera.studio.photoeditor.data.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

/**
 * Lưu bản công khai ảnh/video vào MediaStore (`Pictures|Movies/CameraStudio`) — xuất hiện ngay
 * trong app Ảnh/Gallery hệ thống, khác với bản riêng của app ở `filesDir`/`cacheDir`.
 *
 * Cùng cách branching API-level với [com.camera.studio.photoeditor.ui.importphoto.ImportPhotoResultViewModel.downloadToPublicStorage]
 * (RELATIVE_PATH chỉ dùng được từ Q trở lên; `minSdk` app này là 28 nên vẫn cần fallback EXTERNAL_CONTENT_URI).
 */
object MediaStoreSaver {

    private const val RELATIVE_DIR_IMAGE = "Pictures/CameraStudio"
    private const val RELATIVE_DIR_VIDEO = "Movies/CameraStudio"

    fun saveImageToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? = runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_DIR_IMAGE)
            }
        }
        val uri = resolver.insert(imagesCollection(), values) ?: return@runCatching null
        resolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
        uri
    }.getOrNull()

    fun saveVideoToGallery(context: Context, sourceFile: File, displayName: String): Uri? = runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, RELATIVE_DIR_VIDEO)
            }
        }
        val uri = resolver.insert(videosCollection(), values) ?: return@runCatching null
        resolver.openOutputStream(uri)?.use { out -> sourceFile.inputStream().use { it.copyTo(out) } }
        uri
    }.getOrNull()

    fun deleteFromGallery(context: Context, uriString: String): Boolean = runCatching {
        context.contentResolver.delete(Uri.parse(uriString), null, null) > 0
    }.getOrDefault(false)

    private fun imagesCollection(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    private fun videosCollection(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
}
