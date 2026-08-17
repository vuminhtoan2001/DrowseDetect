package com.camera.qrscanner.barcode.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.runtime.key
import com.camera.qrscanner.barcode.R
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import kotlinx.coroutines.delay

/**
 * Native ad "toàn màn" kiểu inter (port NativeAdsFull của app mẫu) — NHƯNG load/show qua lib
 * com.nlbn.ads (Admob.loadNativeAd + pushAdsToViewCustom) thay cho GMS AdLoader trực tiếp,
 * và bỏ log Appsflyer/Slack (lib tự lo revenue).
 *
 * Ad được preload ngay khi vào cây; overlay đen chỉ hiện khi [start] = true. Có đếm ngược
 * [countdownSec] giây rồi mới cho bấm nút đóng (giống hành vi inter).
 *
 * @param unitId  ad unit id (rỗng => không làm gì).
 * @param start   bật overlay full-screen + đếm ngược (thường gắn với showFull state của caller).
 * @param onClose bấm nút đóng (sau khi hết đếm ngược) → caller điều hướng tiếp.
 */
@Composable
fun NativeAdsFull(
    modifier: Modifier = Modifier,
    unitId: String,
    name: String? = null,
    buttonText: String? = null,
    countdownSec: Int = 2,
    loadOnMount: Boolean = true,
    /**
     * Ad đã được tải sẵn từ bên ngoài (xem [InterFlowState]). Có giá trị thì composable này KHÔNG tự
     * tải nữa, và cũng KHÔNG destroy khi rời cây — chủ sở hữu là bên truyền vào.
     */
    providedAd: NativeAd? = null,
    onError: (() -> Unit)? = null,
    onLoaded: (() -> Unit)? = null,
    onLoadedNative: ((NativeAd) -> Unit)? = null,
    onImpression: (() -> Unit)? = null,
    onClose: () -> Unit = {},
    start: Boolean = true,
) {
    if (unitId.isBlank()) return

    var selfLoadedAd by remember { mutableStateOf<NativeAd?>(null) }
    val nativeAd = providedAd ?: selfLoadedAd
    var counter by remember { mutableStateOf(countdownSec) }
    val context = LocalContext.current

    // Preload ad khi vào cây (dù đang ẩn) — giống loadOnMount của app mẫu.
    LaunchedEffect(loadOnMount, unitId, providedAd != null) {
        if (!loadOnMount || providedAd != null) return@LaunchedEffect
        Admob.getInstance().loadNativeAd(context, unitId, object : NativeCallback() {
            override fun onNativeAdLoaded(ad: NativeAd) {
                selfLoadedAd?.destroy()
                selfLoadedAd = ad
                onLoadedNative?.invoke(ad)
                onLoaded?.invoke()
            }
            override fun onAdFailedToLoad() { onError?.invoke() }
            override fun onAdImpression() { onImpression?.invoke() }
        })
    }

    // Đếm ngược lại từ đầu mỗi lần overlay bật.
    LaunchedEffect(start) {
        if (start) {
            counter = countdownSec
            while (counter > 0) {
                delay(1000)
                counter -= 1
            }
        }
    }

    // Chỉ dọn ad do CHÍNH nó tải. `providedAd` là của bên ngoài — destroy ở đây thì bên ngoài còn
    // giữ tham chiếu tới một ad đã chết.
    DisposableEffect(Unit) {
        onDispose { selfLoadedAd?.destroy(); selfLoadedAd = null }
    }

    if (!start) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .zIndex(1000f),
    ) {
        nativeAd?.let { ad ->
            key(ad) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        LayoutInflater.from(ctx).inflate(R.layout.ad_native_full, null).also { root ->
                            bindNativeAd(root as NativeAdView, ad, buttonText)
                        }
                    },
                )
            }
        }

        // Nút đóng: hết đếm ngược mới bấm được (trước đó hiện số giây).
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp)
                .size(28.dp)
                .background(Color.White, CircleShape)
                .clickable(enabled = counter == 0) { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            if (counter == 0) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(text = counter.toString(), color = Color(0xFF6B7280), fontSize = 14.sp)
            }
        }
    }
}

/** Bind NativeAd vào NativeAdView tự dựng (view như app mẫu; lib không có layout full). */
private fun bindNativeAd(adView: NativeAdView, ad: NativeAd, buttonText: String?) {
    val media = adView.findViewById<MediaView>(R.id.ad_media)
    adView.mediaView = media

    val headline = adView.findViewById<TextView>(R.id.ad_headline)
    adView.headlineView = headline
    headline.text = ad.headline

    val body = adView.findViewById<TextView>(R.id.ad_body)
    adView.bodyView = body
    body.text = ad.body
    body.visibility = if (ad.body.isNullOrEmpty()) View.GONE else View.VISIBLE

    val advertiser = adView.findViewById<TextView>(R.id.ad_advertiser)
    adView.advertiserView = advertiser
    advertiser.text = ad.advertiser
    advertiser.visibility = if (ad.advertiser.isNullOrEmpty()) View.GONE else View.VISIBLE

    val icon = adView.findViewById<ImageView>(R.id.ad_app_icon)
    adView.iconView = icon
    val iconDrawable = ad.icon?.drawable
    if (iconDrawable != null) {
        icon.setImageDrawable(iconDrawable)
        icon.visibility = View.VISIBLE
    } else {
        icon.visibility = View.GONE
    }

    val cta = adView.findViewById<Button>(R.id.ad_call_to_action)
    adView.callToActionView = cta
    cta.text = ad.callToAction ?: buttonText.orEmpty()

    adView.setNativeAd(ad)
}
