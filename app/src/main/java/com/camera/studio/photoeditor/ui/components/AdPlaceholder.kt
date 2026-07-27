package com.camera.studio.photoeditor.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * TẠM THỜI no-op ở M0-M7 (chưa gắn base-application AAR — xem M8 trong kế hoạch).
 * Giữ nguyên chữ ký để các màn hình gọi `NativeAdPlaceholder(...)` không cần sửa lại khi ads bật ở M8,
 * lúc đó thay thân hàm bằng `NativeAdSlot(placement, modifier, isSmall = isSmall)` thật.
 */
@Composable
fun NativeAdPlaceholder(placement: String, modifier: Modifier = Modifier, isSmall: Boolean = false) {
    // no-op
}
