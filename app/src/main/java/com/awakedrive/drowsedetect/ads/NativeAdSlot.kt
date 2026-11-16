package com.awakedrive.drowsedetect.ads

import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob

/** Tìm ComponentActivity qua chuỗi ContextWrapper (LocalContext.current có thể là ContextThemeWrapper). */
private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

/** [AdsViewModel] scope theo Activity (cache sống xuyên điều hướng); fallback owner nếu không thấy Activity. */
@Composable
private fun rememberAdsViewModel(): AdsViewModel {
    val owner = LocalContext.current.findComponentActivity()
        ?: checkNotNull(LocalViewModelStoreOwner.current) { "No ViewModelStoreOwner" }
    return viewModel(viewModelStoreOwner = owner)
}

/**
 * Native ad slot qua lib com.nlbn.ads — cache + skeleton + suppress-on-error (theo ad-origin),
 * NHƯNG skeleton dùng layout dựng sẵn của thư viện (ads_native_loading_short_main / ads_native_bot_loading_2).
 *
 * State (isLoading/isLoaded/nativeAd) nằm trong [AdsViewModel.Slot] scope theo Activity nên KHÔNG
 * reload mỗi lần vào lại composition. Khi vào lại màn: [AdsViewModel.Slot.prepareForEntry] nạp ad MỚI
 * nếu đã quá interval, còn bounce nhanh thì giữ cache (tránh phí request).
 *
 * @param slotName khoá cache (trong list thì thêm hậu tố theo index).
 * @param unitId   ad unit id (rỗng => không hiển thị gì).
 */
@Composable
fun NativeAdSlot(
    slotName: String,
    unitId: String,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false,
    adsViewModel: AdsViewModel = rememberAdsViewModel(),
    onImpression: (() -> Unit)? = null,
    onError: (() -> Unit)? = null,
    onStartLoad: (() -> Unit)? = null,
    /** Bắn khi ad vào trạng thái "chốt" — impression HOẶC lỗi (vd: tắt loading gate ở Intro). */
    onImpressionOrError: (() -> Unit)? = null,
    isAdUiStable: Boolean = false
) {
    if (unitId.isBlank()) return

    val slot = adsViewModel.slot(slotName)
    val isLoading by slot.isLoading.collectAsState()
    val isLoaded by slot.isLoaded.collectAsState()
    val cachedAd by slot.nativeAd.collectAsState()
    var suppressedAfterError by remember { mutableStateOf(false) }

    val minIntervalMs = remember { AdManager.minAdRequestIntervalMs() }

    // Vào lại màn (đổi tab/pop back → composable tạo lại → effect chạy lại):
    // quá interval thì bỏ cache để nạp ad mới (fresh impression), chưa quá thì giữ cache.
    LaunchedEffect(Unit) { slot.prepareForEntry(minIntervalMs) }

    when {
        suppressedAfterError -> {
            // Giữ nguyên chiều cao để không bị nhảy layout
            if(isAdUiStable){
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(if (isSmall) 86.dp else if (isSmall) 220.dp else 280.dp)
                ) {}
                return
            }
            Box(
                modifier = modifier
                    .fillMaxWidth()
            ) {}
        }
        isLoading -> SkeletonNativeAd(modifier.fillMaxWidth(), isSmall)
        isLoaded -> NativeAdsCache(cachedAd, modifier.fillMaxWidth(), isSmall)
        else -> NativeAds(
            unitId = unitId,
            modifier = modifier.fillMaxWidth(),
            isSmall = isSmall,
            loadOnMount = slot.shouldLoadOnMount(),
            onStartLoad = { slot.onStartLoad(); onStartLoad?.invoke() },
            onLoaded = { slot.onLoaded() },
            onLoadedNative = { slot.onLoadedNative(it) },
            onImpression = { slot.onImpression(); onImpression?.invoke(); onImpressionOrError?.invoke() },
            onError = { slot.onError(); suppressedAfterError = true; onError?.invoke(); onImpressionOrError?.invoke() },
        )
    }
}

/** Nạp native ad qua lib và render. KHÔNG destroy ad khi dispose — [AdsViewModel.Slot] sở hữu vòng đời. */
@Composable
private fun NativeAds(
    unitId: String,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false,
    loadOnMount: Boolean = true,
    onStartLoad: () -> Unit = {},
    onLoaded: () -> Unit = {},
    onLoadedNative: (NativeAd) -> Unit = {},
    onImpression: () -> Unit = {},
    onError: () -> Unit = {},
) {
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    val context = LocalContext.current
    LaunchedEffect(loadOnMount) {
        if (!loadOnMount) return@LaunchedEffect
        onStartLoad()
        Admob.getInstance().loadNativeAd(context, unitId, object : NativeCallback() {
            override fun onNativeAdLoaded(ad: NativeAd) { nativeAd = ad; onLoadedNative(ad); onLoaded() }
            override fun onAdFailedToLoad() { onError() }
            override fun onAdImpression() { onImpression() }
        })
    }
    nativeAd?.let { NativeAdsCache(it, modifier, isSmall) }
}

/** Render một NativeAd đã cache qua layout của lib. */
@Composable
private fun NativeAdsCache(nativeAd: NativeAd?, modifier: Modifier = Modifier, isSmall: Boolean = false) {
    val ad = nativeAd ?: return
    val layoutRes = if (isSmall) {
        com.brian.base_application.R.layout.ads_native_bot_no_media_short
    } else {
        com.brian.base_application.R.layout.ads_native_bot_2
    }
    // key(ad): ad đổi → tạo lại AndroidView (factory bind ad mới); ad không đổi → giữ nguyên,
    // KHÔNG bind lại (tránh warning "bind 2 lần" của Admob). Bind đúng 1 lần trong factory.
    key(ad) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { ctx ->
                val v = LayoutInflater.from(ctx).inflate(layoutRes, null) as NativeAdView
                Admob.getInstance().pushAdsToViewCustom(ad, v)
                v
            },
        )
    }
}

/** Skeleton/shimmer khi đang load — dùng layout dựng sẵn trong lib (res của base-application.aar). */
@Composable
private fun SkeletonNativeAd(modifier: Modifier = Modifier, isSmall: Boolean = false) {
    val layoutRes = if (isSmall) {
        com.brian.base_application.R.layout.ads_native_loading_short_main   // ShimmerFrameLayout, tự chạy
    } else {
        com.brian.base_application.R.layout.ads_native_bot_loading_2   // skeleton native full
    }
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx -> LayoutInflater.from(ctx).inflate(layoutRes, null) },
    )
}
