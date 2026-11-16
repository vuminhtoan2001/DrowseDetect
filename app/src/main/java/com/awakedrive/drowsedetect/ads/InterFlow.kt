package com.awakedrive.drowsedetect.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.google.android.gms.ads.nativead.NativeAd
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob

/**
 * Kịch bản "inter → native-sau-inter full-màn → tiếp tục" (`inter_X` + `native_inter_X`).
 *
 * Trình tự:
 * 1. `run()` xét tần suất cho **cả hai** placement, và **bắt đầu tải `native_inter_X` NGAY**.
 * 2. Hiện `inter_X` nếu đạt tần suất.
 * 3. Inter đóng (hoặc bị bỏ qua) → hiện `native_inter_X` full-màn nếu đạt tần suất.
 * 4. Đóng native → `onDone()`.
 *
 * **Tải song song ở bước 1 là điểm quan trọng.** Bản trước chỉ xin native ở bước 3, nên sau khi tắt
 * inter người dùng nhìn một màn ĐEN (khung của native full-màn) suốt thời gian tải — cảm giác app
 * treo. Giờ tới bước 3 thì ad thường đã sẵn sàng, hiện ra ngay.
 *
 * Cả hai placement đều qua [AdManager.shouldShowInter] (enable + showRatio + maxShowPerDay), mỗi cái
 * một bộ đếm riêng. Hàm đó **tự đếm** nên mỗi placement chỉ được gọi đúng một lần trong một lượt.
 *
 * Dùng ở màn tính năng: lấy state chung bằng `LocalInterFlow.current` rồi gọi [InterFlowState.run]
 * tại điểm trigger. **KHÔNG** tự tạo state và KHÔNG tự nhúng [InterFlowOverlay] — cả hai đã có sẵn
 * một lần duy nhất ở `AppRoot`, xem [LocalInterFlow].
 */
class InterFlowState {

    /**
     * Native-sau-inter đã tải xong, đang chờ hiện. **State này sở hữu ad**, không phải composable —
     * xem [InterFlowOverlay] để biết vì sao.
     */
    var preloadedFake by mutableStateOf<NativeAd?>(null)
        internal set

    /** true = đã tới lúc hiện native-sau-inter (inter đã đóng hoặc bị bỏ qua). */
    var showFake by mutableStateOf(false)
        internal set

    internal var fakeUnitId: String? = null
        private set

    private var fakeFailed = false
    internal var onDone: (() -> Unit)? = null

    /** Đang có một lượt inter/native chạy dở — dùng để chặn lượt thứ hai chen vào. */
    private val running: Boolean
        get() = onDone != null

    /**
     * @param interPlacement vd "inter_download"; @param fakePlacement vd "native_inter_download".
     * @param onDone chạy sau khi xong toàn bộ (đóng inter + native-sau-inter, hoặc bỏ qua).
     *
     * **Native-sau-inter được xin NGAY TỪ ĐÂY, song song với inter.** Bản trước chỉ bắt đầu tải nó
     * sau khi inter đóng, nên người dùng phải ngồi nhìn một màn ĐEN (khung của native full-màn) suốt
     * thời gian tải — cảm giác app treo. Tải song song thì tới lúc inter đóng ad thường đã sẵn sàng,
     * hiện ra ngay; chưa kịp thì chỉ chờ nốt phần còn lại chứ không phải chờ từ đầu.
     */
    fun run(
        context: Context,
        activity: Activity?,
        interPlacement: String,
        fakePlacement: String,
        onDone: () -> Unit,
    ) {
        // Chặn lượt thứ hai chen vào khi lượt trước chưa xong. State này dùng CHUNG cho cả app
        // (xem [LocalInterFlow]) nên nếu không chặn thì `onDone` của lượt trước bị ghi đè và mất
        // luôn — nghĩa là hành động người dùng vừa bấm không bao giờ được thực hiện.
        if (running) {
            onDone()
            return
        }
        this.onDone = onDone
        fakeFailed = false
        preloadedFake?.destroy()
        preloadedFake = null

        // `shouldShowInter` TỰ ĐẾM tần suất nên chỉ được gọi đúng một lần cho mỗi placement — gọi ở
        // đây thay vì sau khi inter đóng, để biết ngay có cần tải native hay không.
        fakeUnitId = if (AdManager.shouldShowInter(context, fakePlacement)) {
            AdManager.getAdUnitId(context, fakePlacement).takeIf { it.isNotBlank() }
        } else {
            null
        }
        fakeUnitId?.let { id ->
            Admob.getInstance().loadNativeAd(context, id, object : NativeCallback() {
                override fun onNativeAdLoaded(ad: NativeAd) {
                    preloadedFake = ad
                }

                override fun onAdFailedToLoad() {
                    fakeFailed = true
                    // Lỗi trong lúc đang hiện màn chờ → đóng luôn, đừng để treo màn đen
                    if (showFake) finishFake()
                }
            })
        }

        val proceedToFake = {
            if (fakeUnitId == null || fakeFailed) finishFake() else showFake = true
        }
        if (activity != null && AdManager.shouldShowInter(context, interPlacement)) {
            InterAds.show(activity, AdManager.getAdUnitId(context, interPlacement)) { proceedToFake() }
        } else {
            proceedToFake()
        }
    }

    internal fun finishFake() {
        val done = onDone
        preloadedFake?.destroy()
        preloadedFake = null
        showFake = false
        fakeUnitId = null
        onDone = null
        done?.invoke()
    }
}

@Composable
fun rememberInterFlow(): InterFlowState = remember { InterFlowState() }

/**
 * [InterFlowState] dùng CHUNG cho toàn app, cấp ở `AppRoot` — mọi màn lấy qua
 * `LocalInterFlow.current` thay vì tự tạo state và tự nhúng [InterFlowOverlay].
 *
 * Vì sao gom về một chỗ:
 * - **Không thể quên nhúng overlay.** Thêm một placement inter mới chỉ cần gọi `run(...)`; trước đây
 *   phải nhớ thêm cả `rememberInterFlow()` lẫn `InterFlowOverlay(...)` vào màn đó, quên một cái là
 *   quảng cáo âm thầm không bao giờ hiện mà không có lỗi nào báo.
 * - **`onDone` vẫn chạy khi màn nguồn đã rời cây giao diện.** Overlay đặt trong từng màn thì màn đó
 *   bị huỷ giữa lúc đang hiện là `finishFake()` không bao giờ được gọi → `onDone` mất, và NativeAd
 *   đang giữ cũng không được `destroy()` (rò bộ nhớ).
 * - **Chỉ một lượt tại một thời điểm**, đúng bản chất quảng cáo toàn màn.
 */
val LocalInterFlow = staticCompositionLocalOf<InterFlowState> {
    error("Chưa cấp InterFlowState — bọc cây giao diện trong CompositionLocalProvider ở AppRoot")
}

/**
 * Overlay native fake-inter — bọc trong Dialog windowed FULLSCREEN để phủ TOÀN APP
 * (đè cả bottom nav/Scaffold) giống interstitial, thay vì chỉ trong vùng màn hiện tại.
 * Không cho dismiss ngoài ý muốn; đóng qua nút của [NativeAdsFull].
 */
@Composable
fun InterFlowOverlay(state: InterFlowState) {
    // Chỉ dựng Dialog khi ĐẾN LÚC hiện. Việc tải nằm ở [InterFlowState] chứ không ở đây, vì nếu
    // preload bằng cách mount composable ở một chỗ rồi lúc hiện lại mount vào trong Dialog thì
    // composable bị huỷ và dựng lại — `DisposableEffect` destroy luôn ad vừa tải, preload thành vô
    // ích. State sống độc lập với cây giao diện nên không có vấn đề đó.
    if (!state.showFake) return
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
            // Ad đã tải sẵn (hoặc đang tải nốt) do state giữ — composable không tự tải lần nữa
            providedAd = state.preloadedFake,
            loadOnMount = false,
            start = true,
            onClose = { state.finishFake() },
            onError = { state.finishFake() },
        )
    }
}
