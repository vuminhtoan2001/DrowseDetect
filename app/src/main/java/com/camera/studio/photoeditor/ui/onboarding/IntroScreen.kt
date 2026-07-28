package com.camera.studio.photoeditor.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brian.base_iap.utils.IAPUtils
import com.camera.studio.photoeditor.R
import com.camera.studio.photoeditor.advertisement.AdPositions
import com.camera.studio.photoeditor.advertisement.NativeAdSlot
import com.camera.studio.photoeditor.firebase.Remote
import com.camera.studio.photoeditor.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class IntroSlide(val image: Int, val headline: String, val subtitle: String, val adPlacement: String)

/**
 * 4 slide onboarding theo `vga52a/screenshot/intro1-4.jpg` (headline: Smart Camera Tools / Edit
 * Made Simple / Collage Maker / [slide 4 nội dung ASSUMPTION — xem docs/09_OPEN_ASSUMPTIONS.md #2]).
 * Ảnh minh hoạ (`R.drawable.intro_1..4`) TẠM dùng ảnh vga34b — thay ảnh thật ở M1.
 *
 * Native ad INLINE theo vị trí (`positionIntrol`, N=native INLINE dưới slide N — xem AdPositions).
 * Mặc định `positionIntrol=[]` (res/xml/config.xml) → KHÔNG hiện ad nào cho tới khi Remote Config
 * đặt giá trị thật. Continue bị disable (dim + spinner) cho tới khi ad slide hiện tại tải xong/lỗi/
 * timeout 5s — giống mọi app khác trong dòng này (checklist mục H).
 */
private val slides = listOf(
    IntroSlide(R.drawable.intro_1, "Smart Camera Tools", "Zoom, grid, flash & pro settings to snap the perfect shot anytime.", "native_intro1"),
    IntroSlide(R.drawable.intro_2, "Edit Made Simple", "Add frames, filters, stickers & text — edit fast, fun, and easy.", "native_intro2"),
    IntroSlide(R.drawable.intro_3, "Collage Maker", "Mix photos, layouts to create memories that shine.", "native_intro3"),
    IntroSlide(R.drawable.intro_4, "Get Started", "Everything you need to capture, edit, and share — all in one app.", "native_intro4"),
)

private const val INTRO_AD_TIMEOUT_MS = 5_000L

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage
    val last = page == slides.lastIndex
    val remote = Remote.instance

    val positions = remember { AdPositions.selected() }
    fun adOn(p: Int) = !IAPUtils.isPremium() &&
        remote.isAdEnabled(slides[p].adPlacement) && remote.adUnit(slides[p].adPlacement).isNotBlank()
    fun inlineOn(p: Int) = adOn(p) && positions.contains(p + 1)

    val hasInline = inlineOn(page)
    val ready = remember { mutableStateMapOf<Int, Boolean>() }
    val pageReady = !hasInline || ready[page] == true

    LaunchedEffect(page, hasInline) {
        if (hasInline && ready[page] != true) { delay(INTRO_AD_TIMEOUT_MS); ready[page] = true }
    }

    fun advance() {
        if (last) onFinish() else scope.launch { pagerState.animateScrollToPage(page + 1) }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { p ->
            val slide = slides[p]
            Column(Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(slide.image),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(slide.headline, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(slide.subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
                if (p == page && hasInline) {
                    NativeAdSlot(slide.adPlacement, Modifier.padding(top = 8.dp), onResolved = { ready[page] = true })
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
            slides.indices.forEach { i ->
                val selected = page == i
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (selected) 18.dp else 8.dp, height = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selected) Primary else MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(50))
                .background(if (pageReady) Primary else Primary.copy(alpha = 0.5f))
                .clickable(enabled = pageReady) { advance() },
            contentAlignment = Alignment.Center,
        ) {
            if (pageReady) {
                Text(if (last) stringResource(R.string.start_btn) else stringResource(R.string.continue_btn), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
