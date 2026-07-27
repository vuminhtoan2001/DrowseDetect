package com.camera.studio.photoeditor.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File

object BitmapUtils {

    /** Đọc file JPEG (CameraX takePicture) + tự xoay đúng chiều theo EXIF orientation. */
    fun decodeAndRotate(file: File): Bitmap {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val orientation = ExifInterface(file.absolutePath)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        return applyExifRotation(bitmap, orientation)
    }

    /**
     * Đọc ảnh từ content:// Uri (Photo Picker của "Edit photo") + tự xoay theo EXIF + downsample
     * nếu ảnh quá lớn (tránh OOM với ảnh gallery độ phân giải cao, ví dụ 4000×3000+).
     */
    fun decodeAndRotateFromUri(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
        val resolver = context.contentResolver
        // LƯU Ý: BitmapFactory.decodeStream với inJustDecodeBounds=true LUÔN trả về null (bounds ghi
        // vào `options` như side-effect) — KHÔNG được coi giá trị trả về null này là "mở stream lỗi".
        val boundsStream = resolver.openInputStream(uri) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val dataStream = resolver.openInputStream(uri) ?: return null
        val bitmap = dataStream.use { BitmapFactory.decodeStream(it, null, decodeOptions) } ?: return null

        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        return applyExifRotation(bitmap, orientation)
    }

    /**
     * Đọc ảnh từ path hệ thống file (Wallpaper slot/Collage pane) + downsample nếu quá lớn (tránh
     * OOM khi 1 template có nhiều khung — vd template_13 có 11 khung, mỗi khung 1 ảnh gallery
     * full-res) + tự xoay theo EXIF, giống [decodeAndRotateFromUri] nhưng nhận thẳng path string
     * như `CollageViewModel.decode`.
     */
    fun decodeSampled(path: String, maxDimension: Int = 1600): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeFile(path, decodeOptions) ?: return null

        val orientation = runCatching {
            ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        return applyExifRotation(bitmap, orientation)
    }

    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        if (matrix.isIdentity) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Cắt ảnh theo tỉ lệ [widthRatio]:[heightRatio] kiểu "center-crop" (giống CSS `resizeMode: cover`
     * mà RN dùng khi hiện ảnh trong khung đã resize theo tỉ lệ màn hình đã chọn: 1:1 / 3:4 / 9:16).
     * [widthRatio]/[heightRatio] <= 0 → trả về ảnh gốc (tỉ lệ "Full").
     */
    fun centerCropToAspect(bitmap: Bitmap, widthRatio: Float, heightRatio: Float): Bitmap {
        if (widthRatio <= 0f || heightRatio <= 0f) return bitmap
        val targetAspect = widthRatio / heightRatio
        val srcAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        var cropWidth = bitmap.width
        var cropHeight = bitmap.height
        if (srcAspect > targetAspect) {
            // Ảnh gốc rộng hơn tỉ lệ đích → cắt 2 bên.
            cropWidth = (bitmap.height * targetAspect).toInt().coerceAtMost(bitmap.width)
        } else {
            // Ảnh gốc cao hơn tỉ lệ đích → cắt trên/dưới.
            cropHeight = (bitmap.width / targetAspect).toInt().coerceAtMost(bitmap.height)
        }
        val x = (bitmap.width - cropWidth) / 2
        val y = (bitmap.height - cropHeight) / 2
        return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight)
    }
}
