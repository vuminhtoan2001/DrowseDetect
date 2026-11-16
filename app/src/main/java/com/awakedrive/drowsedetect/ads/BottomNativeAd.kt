package com.awakedrive.drowsedetect.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/**
 * Khối native ad cố định ở **đáy màn hình** — khuôn dùng chung cho 8 vị trí của app (`native_tab`,
 * `native_progress`, `native_finished`, `native_history`, `native_bookmark`, `native_setting`,
 * `native_option_download`, `native_password`).
 *
 * Vì sao gom thành một hàm thay vì viết lặp ở từng màn như ví dụ trong `docs/integration/ADS.md` §2:
 * ở đó khuôn được viết trực tiếp tại chỗ dùng vì mỗi app chỉ có vài vị trí. App này có 8 vị trí y hệt
 * nhau, lặp 8 lần thì mỗi lần sửa (đổi lề, thêm `isSmall`, đổi cách đọc cờ bật/tắt) phải sửa 8 chỗ và
 * chắc chắn sẽ có chỗ bị bỏ sót. Chính playbook cũng cho phép gom khi có nhiều hơn một chỗ gọi.
 *
 * Không cần tự kiểm tra premium hay `unitId` rỗng: [AdManager.getAdUnitId] trả `""` khi người dùng đã
 * mua Premium, và [NativeAdSlot] tự ẩn khi `unitId` rỗng.
 *
 * @param placement tên gốc của vị trí, KHÔNG kèm hậu tố `_enable`.
 * @param isSmall dùng cho chỗ chật (bảng chọn chất lượng, màn nhập mã) — khối ad thấp hơn.
 */
@Composable
fun BottomNativeAd(
    placement: String,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false,
) {
    if (!AdManager.isAdEnabled("${placement}_enable")) return
    val context = LocalContext.current
    NativeAdSlot(
        slotName = placement,
        unitId = AdManager.getAdUnitId(context, placement),
        modifier = modifier.fillMaxWidth(),
        isSmall = isSmall,
    )
}
