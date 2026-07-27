package com.camera.studio.photoeditor.data.removebg

import android.graphics.Bitmap

/**
 * Xoá nền ảnh — gọi API server (bạn gửi contract sau, xem docs/03_EDIT_PHOTO.md §6). Tách interface
 * để cắm implementation thật (Retrofit/OkHttp) mà không phải sửa lại UI Edit Photo.
 */
interface BackgroundRemovalService {
    suspend fun removeBackground(bitmap: Bitmap): Result<Bitmap>
}

/**
 * Stub tạm thời (chưa có API thật) — trả nguyên ảnh gốc sau độ trễ giả lập, để UI loading/error
 * state có thể build & test trước khi nối API thật.
 */
class StubBackgroundRemovalService : BackgroundRemovalService {
    override suspend fun removeBackground(bitmap: Bitmap): Result<Bitmap> {
        kotlinx.coroutines.delay(1200)
        return Result.success(bitmap)
    }
}
