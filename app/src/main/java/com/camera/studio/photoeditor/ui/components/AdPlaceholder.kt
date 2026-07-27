package com.camera.studio.photoeditor.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.camera.studio.photoeditor.advertisement.NativeAdSlot

/**
 * M8: uỷ quyền sang [NativeAdSlot] thật (base-application AAR). Giữ nguyên chữ ký/tên hàm từ
 * M0-M7 nên GalleryScreen/PermissionScreen/SettingScreen/TakePhotoSuccessfullyScreen không cần sửa.
 */
@Composable
fun NativeAdPlaceholder(placement: String, modifier: Modifier = Modifier, isSmall: Boolean = false) {
    NativeAdSlot(placement = placement, modifier = modifier, isSmall = isSmall)
}
