package com.camera.qrscanner.barcode.ui.language

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.camera.qrscanner.barcode.R
import com.camera.qrscanner.barcode.ads.AdManager
import com.camera.qrscanner.barcode.ads.NativeAdSlot
import com.camera.qrscanner.barcode.platform.AppEvents
import com.camera.qrscanner.barcode.platform.TrackScreen
import com.camera.qrscanner.barcode.ui.nav.Dest
import com.camera.qrscanner.barcode.ui.theme.Bg2
import com.camera.qrscanner.barcode.ui.theme.Border
import com.camera.qrscanner.barcode.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

// Token màu của màn Language, ánh xạ sang bảng màu app (docs LANGUAGE_SCREEN_CUSTOM.md §2.1).
private val BrandBgTop = Bg2
private val BrandBgBottom = Color.White
private val BrandCard = Color.White
private val BrandCardAlt = Border
private val BrandPrimary = Theme
private val BrandTextOnLight = Color(0xFF1A1A1A)
private val BrandTextMuted = Color(0xFF8A8A8A)

private const val PLACEMENT_LANGUAGE = "native_language"
private const val PLACEMENT_LANGUAGE_SELECT = "native_language_select"

private const val AD_LOAD_TIMEOUT_MS = 8_000L
/** Chưa chọn ngôn ngữ nào sau ngần này → tự chọn ngôn ngữ của máy. */
private const val AUTO_SELECT_DEVICE_LANGUAGE_DELAY_MS = 8_000L
/**
 * Đã chọn mà ngần này không đổi → tự confirm. Vẫn đợi cả 2 ad "done" (impression / lỗi / timeout)
 * nên thực tế confirm lúc = max(lần chọn gần nhất + 3s, lúc ad done). Ad impression sớm thì người
 * dùng vẫn có 3s để đổi ý; ad chậm thì đợi ad, tối đa [AD_LOAD_TIMEOUT_MS].
 */
private const val AUTO_CONFIRM_DELAY_MS = 3_000L

/**
 * Màn Language custom (docs/update-sdk-language/LANGUAGE_SCREEN_CUSTOM.md) — thay cho LanguageActivity
 * mặc định của SDK. [onDone] (code, method = click | auto) được gọi đúng 1 lần khi user đã active 1
 * ngôn ngữ VÀ cả 2 native ad (`native_language` → `native_language_select`) đã "done"
 * (impression/lỗi/timeout).
 */
@Composable
fun LanguageScreen(onDone: (code: String, method: String) -> Unit, fromSettings: Boolean = false) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // rememberSaveable: đổi locale / xoay màn làm Activity tạo lại — mất trạng thái sẽ log lại màn.
    var selectedCode by rememberSaveable { mutableStateOf<String?>(null) }
    val hasSelected = selectedCode != null

    // Firebase screen_view (SCREEN_TRACKING.md §3.5g): 2 luồng × 2 trạng thái. Lần đầu active 1
    // ngôn ngữ (tay hoặc auto) → ...LanguageSelect; đổi sang ngôn ngữ khác không log lại.
    TrackScreen(
        when {
            fromSettings && hasSelected -> Dest.LANGUAGE_SELECT
            fromSettings -> Dest.LANGUAGE
            hasSelected -> Dest.FIRST_LANGUAGE_SELECT
            else -> Dest.FIRST_LANGUAGE
        },
    )

    // language_select chỉ lần ĐẦU null → có giá trị; gửi thẳng, không qua lifecycle.
    fun select(code: String, method: String) {
        if (selectedCode == null) {
            AppEvents.log(
                context, "language_select",
                "code" to code, "method" to method, "from_settings" to fromSettings,
            )
        }
        selectedCode = code
    }

    var languageDone by remember { mutableStateOf(false) }
    var selectDone by remember { mutableStateOf(false) }
    var selectStarted by remember { mutableStateOf(false) }

    // Chặn gọi onDone 2 lần (tap tay trùng lúc auto-confirm).
    var confirmed by remember { mutableStateOf(false) }
    fun confirm(code: String, method: String) {
        if (confirmed) return
        confirmed = true
        onDone(code, method)
    }

    val listState = rememberLazyListState()
    var scrollSignal by remember { mutableIntStateOf(0) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect { scrollSignal++ }
    }

    // native_language_select chỉ mount sau khi đã active ngôn ngữ VÀ native_language đã done (§6.2).
    LaunchedEffect(hasSelected, languageDone) {
        if (hasSelected && languageDone) selectStarted = true
    }

    // Timeout chống treo (§6.5) — luôn có lối thoát dù ad không bao giờ gọi callback.
    LaunchedEffect(Unit) {
        delay(AD_LOAD_TIMEOUT_MS)
        if (!languageDone) languageDone = true
    }
    LaunchedEffect(selectStarted) {
        if (!selectStarted) return@LaunchedEffect
        delay(AD_LOAD_TIMEOUT_MS)
        if (!selectDone) selectDone = true
    }

    // Auto-select ngôn ngữ máy nếu user chưa tự chọn (§7.1). Reset khi user scroll danh sách.
    // Chỉ đếm khi RESUMED (SCREEN_TRACKING §3.8): bị che / bấm Home thì dừng, quay lại đếm lại từ đầu.
    LaunchedEffect(scrollSignal, lifecycle) {
        var fired = false
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (fired) return@repeatOnLifecycle
            delay(AUTO_SELECT_DEVICE_LANGUAGE_DELAY_MS)
            fired = true
            if (selectedCode == null) select(detectDeviceLanguageCode(), "auto")
        }
    }

    // Auto-confirm nếu không đổi ngôn ngữ sau 1 khoảng (§7.2). Reset khi đổi ngôn ngữ hoặc scroll —
    // đợi luôn 2 ad done rồi mới gọi onDone (không bỏ qua bước ad để tự next). Gate RESUMED như trên;
    // fired chặn gọi onDone 2 lần.
    LaunchedEffect(selectedCode, scrollSignal, lifecycle) {
        val code = selectedCode ?: return@LaunchedEffect
        var fired = false
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (fired) return@repeatOnLifecycle
            delay(AUTO_CONFIRM_DELAY_MS)
            snapshotFlow { languageDone && selectDone }.filter { it }.first()
            fired = true
            confirm(code, "auto")
        }
    }

    // Activity riêng, không nằm dưới AppRoot → tự vẽ nền thương hiệu (cùng gradient với AppRoot).
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BrandBgTop, BrandBgBottom)))
            .navigationBarsPadding(),
    ) {
        LanguageHeader(
            hasSelected = hasSelected,
            isReady = languageDone && selectDone,
            onConfirm = { selectedCode?.let { confirm(it, "click") } },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
        ) {
            items(SupportedLanguages.list, key = { it.code }) { item ->
                LanguageRow(
                    item = item,
                    selected = selectedCode == item.code,
                    onClick = { select(item.code, "click") },
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // 1 slot ad duy nhất, full width, đổi sang native_language_select ngay khi selectStarted (§6.4).
        if (!selectStarted) {
            LanguageAdStage(
                placement = PLACEMENT_LANGUAGE,
                onStageDone = { languageDone = true },
            )
        } else {
            LanguageAdStage(
                placement = PLACEMENT_LANGUAGE_SELECT,
                onStageDone = { selectDone = true },
            )
        }
    }
}

@Composable
private fun LanguageHeader(hasSelected: Boolean, isReady: Boolean, onConfirm: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.language_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BrandTextOnLight,
            )
            Text(
                stringResource(R.string.language_screen_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = BrandTextMuted,
            )
        }
        Spacer(Modifier.width(12.dp))
        // 3 trạng thái (§6.3): chưa chọn → mờ, KHÔNG bấm được; chờ ad → loading; sẵn sàng → bấm được.
        when {
            !hasSelected -> Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.language_done),
                tint = BrandTextMuted.copy(alpha = 0.35f),
                modifier = Modifier.size(36.dp),
            )
            !isReady -> CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 2.dp,
                color = BrandPrimary,
            )
            else -> Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.language_done),
                tint = BrandPrimary,
                modifier = Modifier.size(36.dp).clickable(onClick = onConfirm),
            )
        }
    }
}

@Composable
private fun LanguageRow(item: LanguageItem, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            // Hàng thường = mặt thẻ xám-xanh nhạt trên nền trắng (cùng kiểu thẻ của app); hàng đang
            // chọn = phủ nhẹ màu chính + viền màu chính.
            .background(if (selected) BrandPrimary.copy(alpha = 0.08f) else BrandCard)
            .border(
                width = 1.dp,
                color = if (selected) BrandPrimary else BrandCardAlt,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.flagEmoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Text(
            item.nativeName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = BrandTextOnLight,
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = BrandPrimary,
                unselectedColor = BrandTextMuted,
            ),
        )
    }
}

/**
 * 1 placement native ad — tự "done" ngay nếu bị disable (Remote Config / thiếu ad unit / premium).
 * `onImpressionOrError` của [NativeAdSlot] bắn đúng lúc impression (không phải lúc load xong) hoặc
 * lỗi — đúng tín hiệu "done" mà §6.1 yêu cầu.
 */
@Composable
private fun LanguageAdStage(placement: String, onStageDone: () -> Unit) {
    val context = LocalContext.current
    val unitId = remember(placement) { AdManager.getAdUnitId(context, placement) }
    val enabled = remember(placement) { AdManager.isAdEnabled("${placement}_enable") && unitId.isNotBlank() }
    if (enabled) {
        NativeAdSlot(
            slotName = placement,
            unitId = unitId,
            modifier = Modifier.fillMaxWidth(),
            onImpressionOrError = onStageDone,
        )
    } else {
        LaunchedEffect(placement) { onStageDone() }
    }
}
