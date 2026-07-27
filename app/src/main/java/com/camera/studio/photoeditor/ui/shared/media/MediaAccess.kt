package com.camera.studio.photoeditor.ui.shared.media

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

enum class DeviceMediaKind { IMAGE, VIDEO }

/**
 * Snapshot trạng thái quyền truy cập ảnh/video trên máy tại thời điểm recomposition hiện tại — dùng
 * chung cho Gallery ("Xem tất cả ảnh trong máy") và các picker Edit Photo/Collage Maker/Wallpaper.
 *
 * Android 14+ (API 34) có thêm quyền "Chọn ảnh/video" (partial access) READ_MEDIA_VISUAL_USER_SELECTED
 * — bấm "Cho phép tất cả" thì có full permission, bấm "Chọn ảnh" thì CHỈ có quyền partial (không có
 * full). Phải coi CẢ HAI là "đã có quyền" — chỉ check full permission sẽ luôn false ở chế độ chọn ảnh.
 */
data class MediaAccessState(
    val hasFullAccess: Boolean,
    val hasPartialAccess: Boolean,
    /** Tăng mỗi khi quyền/lựa chọn ảnh có thể đã đổi — dùng làm key để query lại danh sách ảnh máy. */
    val refreshKey: Int,
    val requestAccess: () -> Unit,
) {
    val hasAnyAccess: Boolean get() = hasFullAccess || hasPartialAccess

    /** Chỉ cần mời "cấp thêm" khi đang ở access MỘT PHẦN — có full access rồi thì không còn gì để thêm. */
    val canRequestMore: Boolean get() = hasPartialAccess && !hasFullAccess
}

@Composable
fun rememberMediaAccessState(kind: DeviceMediaKind = DeviceMediaKind.IMAGE): MediaAccessState {
    val context = LocalContext.current
    val isUpsideDownCakePlus = Build.VERSION.SDK_INT >= 34
    val mediaPermission = if (Build.VERSION.SDK_INT >= 33) {
        if (kind == DeviceMediaKind.VIDEO) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    fun isGranted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    fun isFullGranted() = isGranted(mediaPermission)
    fun isPartialGranted() = isUpsideDownCakePlus && isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

    var hasFullAccess by remember(kind) { mutableStateOf(isFullGranted()) }
    var hasPartialAccess by remember(kind) { mutableStateOf(isPartialGranted()) }
    var refreshKey by remember { mutableStateOf(0) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasFullAccess = isFullGranted()
        hasPartialAccess = isPartialGranted()
        refreshKey++
    }
    fun requestAccess() {
        val perms = if (isUpsideDownCakePlus) {
            arrayOf(mediaPermission, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else {
            arrayOf(mediaPermission)
        }
        launcher.launch(perms)
    }

    return MediaAccessState(hasFullAccess, hasPartialAccess, refreshKey, ::requestAccess)
}
