package com.camera.qrscanner.barcode.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Kịch bản "inter → native fake-inter full-màn → tiếp tục" (port JS: inter_X + native_fake_inter_X).
 *
 * - Hiện inter_X nếu đạt tần suất; đóng xong → xét native_fake_inter_X.
 * - native_fake_inter_X là native FULL-MÀN (dùng [NativeAdsFull]) đóng vai "giả interstitial",
 *   hiện SAU inter (hoặc thay inter nếu inter không chạy) khi đạt tần suất riêng.
 * - Cả hai đều qua [AdManager.shouldShowInter] (enable + showRatio + maxShowPerDay).
 *
 * Dùng: tạo state bằng [rememberInterFlow], gọi [InterFlowState.run] tại điểm trigger,
 * và đặt [InterFlowOverlay] trong một Box phủ toàn màn.
 */
class InterFlowState {
    /** unitId của native fake đang cần hiển thị (null = không hiện overlay). */
    var fakeUnitId by mutableStateOf<String?>(null)
        internal set
    internal var onDone: (() -> Unit)? = null

    /**
     * @param interPlacement vd "inter_scan"; @param fakePlacement vd "native_fake_inter_scan".
     * @param onDone chạy sau khi xong toàn bộ (đóng inter + fake, hoặc bỏ qua).
     */
    fun run(
        context: Context,
        activity: Activity?,
        interPlacement: String,
        fakePlacement: String,
        onDone: () -> Unit,
    ) {
        val proceedToFake = {
            if (AdManager.shouldShowInter(context, fakePlacement)) {
                this.onDone = onDone
                this.fakeUnitId = AdManager.getAdUnitId(context, fakePlacement)
            } else {
                onDone()
            }
        }
        if (activity != null && AdManager.shouldShowInter(context, interPlacement)) {
            InterAds.show(activity, AdManager.getAdUnitId(context, interPlacement)) { proceedToFake() }
        } else {
            proceedToFake()
        }
    }

    internal fun finishFake() {
        val done = onDone
        fakeUnitId = null
        onDone = null
        done?.invoke()
    }
}

@Composable
fun rememberInterFlow(): InterFlowState = remember { InterFlowState() }

/**
 * Overlay native fake-inter — bọc trong Dialog windowed FULLSCREEN để phủ TOÀN APP
 * (đè cả bottom nav/Scaffold) giống interstitial, thay vì chỉ trong vùng màn hiện tại.
 * Không cho dismiss ngoài ý muốn; đóng qua nút của [NativeAdsFull].
 */
@Composable
fun InterFlowOverlay(state: InterFlowState) {
    val unitId = state.fakeUnitId ?: return
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        // Ép cửa sổ dialog phủ kín màn (như interstitial), bỏ scrim mờ mặc định.
        val view = LocalView.current
        (view.parent as? DialogWindowProvider)?.window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            w.setDimAmount(0f)
        }
        NativeAdsFull(
            unitId = unitId,
            start = true,
            onClose = { state.finishFake() },
            onError = { state.finishFake() }, // fake lỗi/no-fill → đi tiếp luôn
        )
    }
}
