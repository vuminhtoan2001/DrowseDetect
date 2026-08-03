package com.camera.studio.photoeditor

import android.app.Activity
import com.brian.base_application.BaseApplication
import com.brian.base_iap.utils.FirebaseRemoteConfigUtil
import com.brian.base_iap.utils.IAPUtils
import com.camera.studio.photoeditor.core.InstallReferrerHelper
import com.camera.studio.photoeditor.core.LocaleHelper
import com.camera.studio.photoeditor.core.MainActivity
import com.camera.studio.photoeditor.firebase.Remote
import com.nlbn.ads.util.AppFlyer
import dagger.hilt.android.HiltAndroidApp

/**
 * Application host — kế thừa [BaseApplication] của thư viện base-application (M8, thay bản
 * [android.app.Application] tạm dùng ở M0-M7). Khai báo trong AndroidManifest: <application
 * android:name=".MyApplication" ... />
 *
 * LƯU Ý:
 *  - PHẢI gọi super.onCreate() (lib làm toàn bộ init ads/consent/IAP/notification trong đó).
 *  - KHÔNG tự init MobileAds / consent / AppsFlyer ở nơi khác — lib lo (AppsFlyer init tại initAppFlyerId()).
 *
 * TODO trước phát hành (xem review M8):
 *  - Tạo Firebase project thật cho vga52a, thay `app/google-services.json` (đang là mock).
 *  - admob_app_id / facebook_app_id / facebook_client_token / app_flyer_id / public_license_key:
 *    đang là placeholder trong strings.xml — điền giá trị thật.
 *  - iapPremiumKey/Weekly/Monthly/Yearly: đang dùng default*Key() của lib — đổi sang product id
 *    thật khi có key Play Console.
 *  - assets/default_ads_config.json: đổi toàn bộ unit test sang unit thật sau khi có AdMob app.
 */
@HiltAndroidApp
class MyApplication : BaseApplication() {

    override fun onCreate() {
        super.onCreate()   // lib init ads/consent/IAP/notification + set default `ads_config` + tự fetch RC

        // Đăng ký default Remote Config của app (KHÔNG gọi setDefaultsAsync trực tiếp — sẽ xoá `ads_config`).
        FirebaseRemoteConfigUtil.getInstance().setAppDefaultsFromXml(R.xml.config)

        // Tính cache + fetch lại (lib đã tự fetch RC trong super.onCreate()).
        Remote.instance.fetchAndActivate()

        // Xác định nguồn cài (InstallReferrer) SỚM để kịp cache trước khi lib mở Home
        // → MainActivity.resolveStartRoute gate màn Intro theo ads/organic.
        InstallReferrerHelper.resolve(this)
    }

    // ===================== Thương hiệu & Splash =====================
    override fun getAppNameRes(): Int = R.string.app_name
    override fun getIconSplashRes(): Int = R.drawable.logo            // TODO: icon splash riêng
    override fun getSplashLoadingRes(): Int = R.raw.splash_loading

    // ===================== Định tuyến =====================
    override fun getHomeActivity(): Class<out Activity> = MainActivity::class.java

    // ===================== Năng lực =====================
    override fun hasForegroundServicePermission(): Boolean = true

    // ===================== Khởi tạo =====================
    override fun initAppFlyerId() {
        val appFlyerId = getString(R.string.app_flyer_id)
        if (appFlyerId.isBlank() || appFlyerId.startsWith(MOCK_KEY_PREFIX, ignoreCase = true)) {
            return  // key mock — bỏ qua init để tránh AppsFlyer SDK log lỗi vô ích
        }
        AppFlyer.getInstance().initAppFlyer(
            this,
            appFlyerId,
            /* enableLog       */ BuildConfig.DEBUG,
            /* enableDeepLink  */ false,
            /* enableUninstall */ true,
        )
    }

    override fun setupKoin() { /* app không dùng Koin */ }

    // ===================== Ngôn ngữ =====================
    // App đọc ngôn ngữ từ prefs "AppStorage" key "language". Lib gọi hàm này khi user chọn ngôn ngữ
    // (cả màn Language lần đầu LẪN màn Language mở từ Settings) — set per-app locale AndroidX để
    // AppCompat recreate MainActivity theo ngôn ngữ mới.
    override fun notifyLanguageSaved(languageCode: String) {
        getSharedPreferences("AppStorage", MODE_PRIVATE).edit()
            .putString("language", languageCode).apply()
        LocaleHelper.updateLocale(this, languageCode)
    }

    // ===================== Khoá IAP (product id Play Console) =====================
    // Chưa có key thật cho vga52a — dùng default của lib tạm thời (đổi khi có Play Console product).
    override fun iapPremiumKey(): String = defaultIapPremiumKey()
    override fun iapPremiumWeeklyKey(): String = defaultIapPremiumWeeklyKey()
    override fun iapPremiumMonthlyKey(): String = defaultIapPremiumMonthlyKey()
    override fun iapPremiumYearlyKey(): String = defaultIapPremiumYearlyKey()
    override fun iapPublicKey(): String = getString(R.string.public_license_key)

    // ===================== Bộ thông báo (5 phần tử, khớp index) =====================
    override fun getNotificationImages(): IntArray = intArrayOf(
        R.drawable.logo, R.drawable.logo, R.drawable.logo, R.drawable.logo, R.drawable.logo,
    )
    override fun getNotificationTitles2ArrayRes(): Int = R.array.notification_title2
    override fun getNotificationMessages2ArrayRes(): Int = R.array.notification_message2
    override fun getNotificationButtons2ArrayRes(): Int = R.array.notification_button2

    override fun getNotificationIconRes(): Int = R.drawable.logo      // TODO: icon status-bar 24dp alpha
    override fun getNotificationChannelPrefix(): String = "CameraStudio"

    override fun getNewFileNotiContentRes(): Int = R.string.baseapp_new_file_content
    override fun getScreenshotNotiTitleRes(): Int = R.string.baseapp_screenshot_title
    override fun getRecentDocumentsTitleRes(): Int = R.string.baseapp_recent_title
    override fun getOpenTextRes(): Int = R.string.baseapp_open
    override fun getScanDocumentRes(): Int = R.string.baseapp_scan
    override fun getWidgetButtonBackgroundRes(): Int = R.drawable.logo   // TODO: asset riêng
    override fun getDailyCallOpenAppContentRes(): Int = R.string.baseapp_daily_open_content
    override fun getCheckNowTextRes(): Int = R.string.baseapp_check_now
    override fun getDocumentPreviewRes(): Int = R.drawable.logo        // TODO: ảnh 365x174dp
    override fun getFullScreenNoti1Res(): Int = R.string.baseapp_fullscreen_1
    override fun getFullScreenNoti2Res(): Int = R.string.baseapp_fullscreen_2
    override fun getNotificationOutAppTitleRes(): Int = R.string.baseapp_outapp_title
    override fun getNotificationOutAppContentRes(): Int = R.string.baseapp_outapp_content

    // Dialog xin quyền thông báo
    override fun getNotiTitleRes(): Int = R.string.baseapp_noti_title
    override fun getNotiContentRes(): Int = R.string.baseapp_noti_content

    // ===================== Tính năng màn IAP (icon + text đi cặp) =====================
    // "Trải nghiệm không quảng cáo" LUÔN đặt ở slot feature5 (cuối danh sách) — quy ước chung, xem
    // docs/integration/HUONG_DAN_TICH_HOP.md §8.3.1: các tính năng khác biệt hoá (filter/frame/HD/
    // priority) xếp trước vì thuyết phục nâng cấp tốt hơn; ad-free coi như lợi ích "mặc định đi kèm".
    override fun getFeature1IconRes(): Int = R.drawable.ic_filter
    override fun getFeature2IconRes(): Int = R.drawable.ic_edit_frame
    override fun getFeature3IconRes(): Int = R.drawable.ic_premium
    override fun getFeature4IconRes(): Int = R.drawable.ic_shield_check
    override fun getFeature5IconRes(): Int = R.drawable.crown

    override fun getFeature1TextRes(): Int? = R.string.premium_feature2
    override fun getFeature2TextRes(): Int? = R.string.premium_feature3
    override fun getFeature3TextRes(): Int? = R.string.premium_feature4
    override fun getFeature4TextRes(): Int? = R.string.premium_feature5
    override fun getFeature5TextRes(): Int? = R.string.premium_feature1

    // ===================== Cấu hình quảng cáo =====================
    override fun isPurchased(): Boolean = IAPUtils.isPremium()
    override fun enableAdsResume(): Boolean = !IAPUtils.isPremium()
    override fun buildDebug(): Boolean = BuildConfig.DEBUG
    override fun getListTestDeviceId(): MutableList<String> = mutableListOf()

    private companion object {
        const val MOCK_KEY_PREFIX = "mock_"
    }
}
