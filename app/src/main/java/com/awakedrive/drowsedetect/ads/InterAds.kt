package com.awakedrive.drowsedetect.ads

import android.app.Activity
import com.nlbn.ads.callback.AdCallback
import com.nlbn.ads.util.Admob

/**
 * Interstitial qua lib (xem docs/integration/ADS.md). Tự hiện loading khi nạp.
 * unitId rỗng (premium/tắt) → gọi onNext ngay (đi tiếp, không chặn).
 */
object InterAds {
    fun show(activity: Activity, unitId: String, onNext: () -> Unit) {
        if (unitId.isBlank()) { onNext(); return }
        Admob.getInstance().loadAndShowInter(activity, unitId, true, object : AdCallback() {
            override fun onNextAction() { onNext() }
        })
    }
}
