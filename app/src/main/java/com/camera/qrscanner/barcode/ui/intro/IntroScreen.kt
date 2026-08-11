package com.camera.qrscanner.barcode.ui.intro

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.camera.qrscanner.barcode.R
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
    val onButtonClick: () -> Unit = {
        scope.launch {
            val p = pager.currentPage
            // Trang có modal NN chưa hiện & còn cho hiện → bật modal, chưa chuyển trang.
            if (modalEnabled(p) && !showFull[p] && modalStatus[p]) {
                showFull[p] = true
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
    LaunchedEffect(page) {
        val hasAd = selectedAds.contains(page + 1) || selectedAds.contains((page + 1) * 11)
        delay(if (hasAd) AUTO_NEXT_WITH_AD_MS else AUTO_NEXT_PLAIN_MS)
        onButtonClick()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Chỉ chừa safe area đáy (nav bar/gesture) để native ad dưới cùng không bị che.
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            HorizontalPager(
                state = pager,
                userScrollEnabled = false, // chỉ tiến bằng nút / auto / đóng modal (như bản mẫu)
                modifier = Modifier.weight(1f),
            ) { p ->
                val s = SLIDES[p]
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(s.img),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                    Text(
                        stringResource(s.title),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                    Text(
                        stringResource(s.sub),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    )
                }
            }

            // Dots
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(SLIDES.size) { i ->
                    val active = i == page
                    Row(
                        Modifier
                            .padding(horizontal = 2.dp)
                            .size(width = if (active) 20.dp else 8.dp, height = 8.dp)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary else Color(0xFF9BC4FB),
                                CircleShape,
                            ),
                    ) {}
                }
            }

            // Có ad inline → nút chỉ là text (ad đã có CTA riêng); không ad → nút THEME to.
            if (shouldShowAd) {
                TextButton(
                    onClick = onButtonClick,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
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
            } else {
                Button(
                    onClick = onButtonClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(52.dp),
                ) {
                    Text(
                        stringResource(if (page != SLIDES.lastIndex) R.string.continue_str else R.string.start).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            // Native inline của trang hiện tại.
            if (shouldShowAd) {
                NativeAdSlot(
                    slotName = "native_intro${page + 1}",
                    unitId = introUnit(page + 1),
                    isSmall = false,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    onStartLoad = { if (page < adLoading.size) adLoading[page] = true },
                    onImpressionOrError = { if (page < adLoading.size) adLoading[page] = false },
                )
            }

        }

        // Modal native toàn màn (NN) — preload theo phạm vi trang, hiện khi showFull[i].
        repeat(SLIDES.size) { i ->
            if (modalEnabled(i) && page in MODAL_RANGE[i]) {
                NativeAdsFull(
                    unitId = introUnit(i + 1),
                    name = "native_intro${i + 1}",
                    buttonText = stringResource(if (i != SLIDES.lastIndex) R.string.continue_str else R.string.start),
                    start = showFull[i],
                    onClose = { onButtonClick() },
                    onError = { modalStatus[i] = false; showFull[i] = false },
                    onLoaded = { if (i < adLoading.size) adLoading[i] = false },
                )
            }
        }
    }
}

private fun hasNoti(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()
