package com.camera.qrscanner.barcode.ui.intro

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.camera.qrscanner.barcode.R
import com.camera.qrscanner.barcode.platform.AppEvents
import com.camera.qrscanner.barcode.platform.LogOnboardingExit
import com.camera.qrscanner.barcode.platform.TrackScreen
import com.camera.qrscanner.barcode.ui.nav.Dest
import com.camera.qrscanner.barcode.ui.theme.Theme
import com.camera.qrscanner.barcode.ui.theme.ThemeSecond
import com.camera.qrscanner.barcode.ads.AdManager
import com.camera.qrscanner.barcode.ads.NativeAdSlot
import com.camera.qrscanner.barcode.ads.NativeAdsFull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Slide(@StringRes val title: Int, @StringRes val sub: Int, @DrawableRes val img: Int)

private val SLIDES = listOf(
    Slide(R.string.intro1, R.string.intro1Sub, R.drawable.intro1),
    Slide(R.string.intro2, R.string.intro2Sub, R.drawable.intro2),
    Slide(R.string.intro3, R.string.intro3Sub, R.drawable.intro3),
    Slide(R.string.intro4, R.string.intro4Sub, R.drawable.intro4),
)

// Auto-advance: trang có ad chờ lâu hơn (JS TIMEOUT_DURATION2), trang thường ngắn hơn.
private const val AUTO_NEXT_WITH_AD_MS = 15_000L
private const val AUTO_NEXT_PLAIN_MS = 5_000L
private const val INLINE_AD_TIMEOUT_MS = 5_000L

// Phạm vi preload modal NN theo trang hiện tại (khớp bản mẫu).
private val MODAL_RANGE = listOf(0..3, 0..1, 1..2, 2..3)

@Composable
fun IntroScreen(
    onGoPermission: () -> Unit,
    onGoMain: () -> Unit,
) {
    val context = LocalContext.current
    val pager = rememberPagerState(pageCount = { SLIDES.size })
    val scope = rememberCoroutineScope()

    // Chọn biến thể positionIntrol 1 lần cho phiên intro.
    val selectedAds = remember { AdManager.getSelectedAdPositions() }
    val adsVariant = remember(selectedAds) { selectedAds.joinToString(",") }   // vd "1,22,3,4"
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Firebase screen_view theo slide (SCREEN_TRACKING.md §3.5f): route tổng không log; dùng
    // settledPage (không dùng currentPage: vuốt dở rồi kéo về sẽ log nhầm slide kế).
    val screenName = "${Dest.ONB_INTRO}${pager.settledPage + 1}"
    TrackScreen(screenName, mapOf("ads_variant" to adsVariant))
    LogOnboardingExit(screenName)

    fun logModal(page: Int, result: String) = AppEvents.log(
        context, "intro_modal",
        "page" to (page + 1), "result" to result, "ads_variant" to adsVariant,
    )

    fun introUnit(oneBased: Int) = AdManager.getAdUnitId(context, "native_intro$oneBased")
    fun introOn(oneBased: Int) = AdManager.isAdEnabled("native_intro${oneBased}_enable")
    // Native inline ở trang p (0-based): bật cờ + positionIntrol chứa N + có id.
    fun inlineEnabled(p: Int) = introOn(p + 1) && selectedAds.contains(p + 1) && introUnit(p + 1).isNotBlank()
    // Modal NN toàn màn ở trang p: bật cờ + positionIntrol chứa (N*11) + có id.
    fun modalEnabled(p: Int) = introOn(p + 1) && selectedAds.contains((p + 1) * 11) && introUnit(p + 1).isNotBlank()

    // State theo 4 trang.
    val adLoading = remember { mutableStateListOf(inlineEnabled(0), inlineEnabled(1), inlineEnabled(2), inlineEnabled(3)) }
    val showFull = remember { mutableStateListOf(false, false, false, false) }
    val modalStatus = remember { mutableStateListOf(true, true, true, true) }

    val page = pager.currentPage
    val shouldShowAd = inlineEnabled(page)
    val busy = adLoading.getOrElse(page) { false }

    fun finish() {
        if (hasNoti(context)) onGoMain() else onGoPermission()
    }

    // Bấm nút / auto-next / đóng modal đều gọi hàm này (khớp onButtonClick bản mẫu).
    // method = click | auto | modal_close | modal_error (event intro_next, SCREEN_TRACKING.md §3.10).
    val onButtonClick: (String) -> Unit = { method ->
        scope.launch {
            val p = pager.currentPage
            AppEvents.log(
                context, "intro_next",
                "from_page" to (p + 1), "method" to method, "ads_variant" to adsVariant,
            )
            // Trang có modal NN chưa hiện & còn cho hiện → bật modal, chưa chuyển trang.
            if (modalEnabled(p) && !showFull[p] && modalStatus[p]) {
                showFull[p] = true
                logModal(p, "shown")
            } else {
                for (k in showFull.indices) showFull[k] = false
                if (p < SLIDES.lastIndex) {
                    pager.animateScrollToPage(p + 1)
                } else {
                    finish()
                }
            }
        }
    }

    // Fallback nhả gate loading inline nếu ad không settle.
    LaunchedEffect(page) {
        if (adLoading.getOrElse(page) { false }) {
            delay(INLINE_AD_TIMEOUT_MS)
            if (page < adLoading.size) adLoading[page] = false
        }
    }

    // Auto-advance sau timeout (JS: 15s nếu trang có ad, 5s nếu không).
    // Chỉ đếm khi RESUMED (SCREEN_TRACKING.md §3.8): click native ad mở CH Play / app-open ad / bấm
    // Home thì dừng, quay lại đếm lại từ đầu; mỗi trang chỉ auto 1 lần.
    LaunchedEffect(page, lifecycle) {
        val hasAd = selectedAds.contains(page + 1) || selectedAds.contains((page + 1) * 11)
        var fired = false
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (fired) return@repeatOnLifecycle
            delay(if (hasAd) AUTO_NEXT_WITH_AD_MS else AUTO_NEXT_PLAIN_MS)
            fired = true
            onButtonClick("auto")
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ẢNH NỀN phủ TOÀN màn (tràn cả sau status bar / nav bar). Pager giờ chỉ còn chứa ảnh nên
        // ảnh vẫn trượt ngang khi sang trang.
        HorizontalPager(
            state = pager,
            userScrollEnabled = false, // chỉ tiến bằng nút / auto / đóng modal (như bản mẫu)
            modifier = Modifier.fillMaxSize(),
        ) { p ->
            Image(
                painter = painterResource(SLIDES[p].img),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter, // giữ nguyên phần trên của ảnh minh hoạ
                modifier = Modifier.fillMaxSize(),
            )
        }

        // TOÀN BỘ phần còn lại đè lên ảnh và dồn sát đáy (flex-end): chữ → dots → nút → native ad.
        // Khối này chỉ cao bằng nội dung của nó, phần ảnh còn lại lộ ra ở trên.
        val slide = SLIDES[page]
        val bg = MaterialTheme.colorScheme.background
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Vùng fade cao CỐ ĐỊNH phía trên chữ: ảnh tan dần vào nền nên mép không gắt.
            Box(Modifier.fillMaxWidth().height(INTRO_FADE_HEIGHT).background(introFadeBrush(bg)))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(bg.copy(alpha = INTRO_PANEL_ALPHA))
                    // Chỉ LỚP PHỦ chừa safe area đáy; ảnh nền vẫn tràn hết xuống mép dưới.
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            Text(
                stringResource(slide.title),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Text(
                stringResource(slide.sub),
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )

            // Có ad inline → nút chỉ là text (ad đã có CTA riêng) và dots nằm CÙNG HÀNG với nút:
            // dots dồn sát lề trái, nút vẫn ở CHÍNH GIỮA màn. Dùng Box chứ không dùng Row-với-weight
            // vì Row sẽ đẩy nút lệch sang phải theo bề rộng của dãy dots.
            // Không có ad → dots một hàng riêng ở giữa, bên dưới là nút THEME to.
            if (shouldShowAd) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Dots(page, Modifier.align(Alignment.CenterStart))
                    TextButton(
                        onClick = { onButtonClick("click") },
                        enabled = !busy,
                        // Bóp nhỏ nút so với mặc định Material. Chỉ đổi contentPadding là KHÔNG đủ:
                        // ruột Button còn tự áp defaultMinSize(58×40) nên nút không nhỏ hơn được.
                        // defaultMinSize ở ngoài đặt min-constraint khác 0, Surface truyền xuống
                        // (propagateMinConstraints) nên defaultMinSize bên trong bị vô hiệu.
                        modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                stringResource(if (page != SLIDES.lastIndex) R.string.continue_str else R.string.start),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            } else {
                Dots(page, Modifier.fillMaxWidth().padding(vertical = 8.dp))
                Box(
                    Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Theme, ThemeSecond)))
                        .clickable { onButtonClick("click") },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(if (page != SLIDES.lastIndex) R.string.continue_str else R.string.start).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Native inline của trang hiện tại.
            if (shouldShowAd) {
                NativeAdSlot(
                    slotName = "native_intro${page + 1}",
                    unitId = introUnit(page + 1),
                    isSmall = false,
                    modifier = Modifier.fillMaxWidth(),
                    onStartLoad = { if (page < adLoading.size) adLoading[page] = true },
                    onImpressionOrError = { if (page < adLoading.size) adLoading[page] = false },
                )
            }
            } // panel
        }

        // Modal native toàn màn (NN) — preload theo phạm vi trang, hiện khi showFull[i].
        repeat(SLIDES.size) { i ->
            if (modalEnabled(i) && page in MODAL_RANGE[i]) {
                NativeAdsFull(
                    unitId = introUnit(i + 1),
                    name = "native_intro${i + 1}",
                    buttonText = stringResource(if (i != SLIDES.lastIndex) R.string.continue_str else R.string.start),
                    start = showFull[i],
                    onClose = { logModal(i, "closed"); onButtonClick("modal_close") },
                    onError = {
                        // Đang được yêu cầu hiện mà lỗi → tự đi tiếp, không thì user kẹt (SCREEN_TRACKING.md §3.9).
                        val requested = showFull[i]
                        modalStatus[i] = false
                        showFull[i] = false
                        logModal(i, "error")
                        if (requested) onButtonClick("modal_error")
                    },
                    onLoaded = { if (i < adLoading.size) adLoading[i] = false },
                )
            }
        }
    }
}

/**
 * Dãy chấm chỉ trang — chấm của trang đang xem dài ra thành viên thuốc.
 *
 * Vị trí do [modifier] quyết định, không do hàm này: `fillMaxWidth()` thì `Arrangement.Center` căn
 * giữa (trang không có ad), còn khi đặt trong Box bằng `align(CenterStart)` thì Row chỉ rộng bằng nội
 * dung nên nằm sát lề trái (trang có ad, đứng cùng hàng với nút).
 */
@Composable
private fun Dots(page: Int, modifier: Modifier = Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(SLIDES.size) { i ->
            val active = i == page
            val width by animateDpAsState(if (active) 20.dp else 8.dp, label = "dotWidth")
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (active) Theme else Theme.copy(alpha = 0.25f)),
            )
        }
    }
}

/** Chiều cao vùng ảnh tan vào nền, phía trên title. */
private val INTRO_FADE_HEIGHT = 140.dp

/** Độ đục của nền + khối chữ (< 1 để thấy ảnh mờ phía dưới); vùng fade dùng CHUNG giá trị để chỗ nối không lộ vạch. */
private const val INTRO_PANEL_ALPHA = 0.92f

/** Nhiều stop theo smoothstep: alpha tăng chậm ở mép trên, nhanh ở giữa, chậm lại khi chạm nền đặc. */
private fun introFadeBrush(bg: Color): Brush {
    val steps = 12
    val stops = Array(steps + 1) { i ->
        val t = i / steps.toFloat()
        t to bg.copy(alpha = t * t * (3f - 2f * t) * INTRO_PANEL_ALPHA)
    }
    return Brush.verticalGradient(colorStops = stops)
}

private fun hasNoti(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()
