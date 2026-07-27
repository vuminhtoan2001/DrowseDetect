package com.camera.studio.photoeditor.ui.videoresult

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * M0: video đã được lưu (Room) ngay tại CameraScreen — màn này chỉ để XEM LẠI trước khi xác nhận.
 * Bake overlay/hiệu ứng vào video (Media3 Transformer) sẽ quay lại ở M2 nếu cần (vd. watermark) —
 * hiện chưa cần nên bỏ khỏi M0 cho gọn.
 */
@HiltViewModel
class VideoResultViewModel @Inject constructor() : ViewModel()
