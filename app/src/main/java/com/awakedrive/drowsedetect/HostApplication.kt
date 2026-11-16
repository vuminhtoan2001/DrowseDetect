package com.awakedrive.drowsedetect

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.brian.base_application.BaseApplication
import com.brian.base_application.language.LanguageRouter
import com.brian.base_iap.utils.FirebaseRemoteConfigUtil
import com.brian.base_iap.utils.IAPUtils
import com.awakedrive.drowsedetect.platform.HomeReached
import com.awakedrive.drowsedetect.platform.ScreenTracker
import com.awakedrive.drowsedetect.ui.language.MyLanguageActivity
import com.awakedrive.drowsedetect.ui.nav.Dest
import com.nlbn.ads.util.AppFlyer
import dagger.hilt.android.HiltAndroidApp
import com.nlbn.ads.util.AppUtil

/**
 * Application của app, kế thừa SDK base-application (xem docs/integration/PLAYBOOK_TICH_HOP.md).
 * SDK là launcher: tự lo Splash → Inter → Language → IAP → getHomeActivity().
 *
 * TODO-SDK: điền giá trị thật cho admob_app_id / facebook_app_id / facebook_client_token /
 *   app_flyer_id / public_license_key và IAP product ids (Play Console).
 */
@HiltAndroidApp
class HostApplication : BaseApplication() {

    override fun onCreate() {
        AppUtil.purchasedProvider = { true }
        // PHẢI gán TRƯỚC super.onCreate() — SDK đọc giá trị này khi dựng luồng Splash (LANGUAGE_SCREEN_CUSTOM.md §3).
        LanguageRouter.customActivityClass = MyLanguageActivity::class.java
        super.onCreate()
        ScreenTracker.init()
        trackLibScreens()
        // Default cờ bật/tắt ads app tự hiển thị (xem docs/integration/ADS.md §0.2). Console override sau khi fetch.
        FirebaseRemoteConfigUtil.getInstance().setAppDefaults(
            mapOf(
                "native_language_enable" to true,
                "native_language_select_enable" to true,
                "native_permission_enable" to true,
                "native_intro1_enable" to true,
                "native_intro2_enable" to true,
                "native_intro3_enable" to true,
                "native_intro4_enable" to true,
                "positionIntrol" to "[[1,22,3,4],[1,2,33,4],[1,2,3,4]]",
                "count_app_open" to 3L,
                "organic_number_not_guide" to 1L,
            ),
        )
    }

    /**
     * Màn của lib (không sửa được code) → log ở onActivityResumed, so tên class bằng chuỗi
     * (SCREEN_TRACKING.md §3.6). AdActivity (quảng cáo) cố ý bỏ qua.
     */
    private fun trackLibScreens() {
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                val firstRun = !HomeReached.isReached(activity) // chưa từng vào Home
                val name = when (activity.javaClass.name) {
                    LIB_SPLASH_ACTIVITY -> Dest.SPLASH
                    LIB_NOTI_PERMISSION_ACTIVITY -> if (firstRun) Dest.FIRST_NOTI_PERMISSION else Dest.NOTI_PERMISSION
                    LIB_IAP_ACTIVITY -> if (firstRun) Dest.FIRST_IAP else Dest.IAP
                    else -> return
                }
                ScreenTracker.log(activity, name)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private companion object {
        const val LIB_SPLASH_ACTIVITY = "com.brian.base_application.start.SplashActivity"
        const val LIB_NOTI_PERMISSION_ACTIVITY = "com.brian.base_application.start.RequestNotificationPermissionActivity"
        const val LIB_IAP_ACTIVITY = "com.brian.base_iap.iap.IapActivity"
    }

    // ----- Splash & thương hiệu -----
    override fun getAppNameRes(): Int = R.string.app_name
    override fun getIconSplashRes(): Int = R.drawable.icon_app
    override fun getSplashLoadingRes(): Int = R.raw.splash_loading

    // ----- Định tuyến / năng lực -----
    override fun getHomeActivity(): Class<out Activity> = MainActivity::class.java
    override fun hasForegroundServicePermission(): Boolean = true

    // ----- Khởi tạo -----
    override fun setupKoin() { /* không dùng Koin */ }
    override fun initAppFlyerId() {
        AppFlyer.getInstance()
            .initAppFlyer(this, getString(R.string.app_flyer_id), BuildConfig.DEBUG, false, true)
    }

    override fun notifyLanguageSaved(code: String) {
        getSharedPreferences(packageName, MODE_PRIVATE).edit()
            .putString("language_pres", code).apply()
        com.awakedrive.drowsedetect.platform.LocaleHelper.updateLocale(this, code)
    }

    override fun getPrivacyPolicyUrl(): String = "https://sites.google.com/view/nigeriacamera789/policy"
    override fun getTermsOfUseUrl(): String = "https://sites.google.com/view/nigeriacamera789/term-of-use"

    override fun iapPremiumKey(): String = "release_premium_access"
    override fun iapPremiumWeeklyKey(): String = "release-weekly-plan"
    override fun iapPremiumMonthlyKey(): String = "release-monthly-plan"
    override fun iapPremiumYearlyKey(): String = "release-yearly-plan"
    override fun iapPublicKey(): String = getString(R.string.public_license_key)

    // ----- 5 tính năng màn IAP (text + icon đi cặp, icon khớp nội dung) -----
    override fun getFeature1TextRes(): Int? = R.string.feature1_text          // Priority support
    override fun getFeature1IconRes(): Int = R.drawable.ic_feature_support
    override fun getFeature2TextRes(): Int? = R.string.feature2_text          // Unlimited scan & create
    override fun getFeature2IconRes(): Int = R.drawable.ic_feature_unlimited
    override fun getFeature3TextRes(): Int? = R.string.feature3_text          // Fast, no interruptions
    override fun getFeature3IconRes(): Int = R.drawable.ic_feature_fast
    override fun getFeature4TextRes(): Int? = R.string.feature4_text          // Premium QR styles
    override fun getFeature4IconRes(): Int = R.drawable.ic_feature_premium
    override fun getFeature5TextRes(): Int? = R.string.feature5_text          // Remove all ads
    override fun getFeature5IconRes(): Int = R.drawable.ic_feature_noads

    // ----- Dialog xin quyền thông báo -----
    override fun getNotiTitleRes(): Int = R.string.sdk_noti_title
    override fun getNotiContentRes(): Int = R.string.sdk_noti_content

    // ----- Bộ thông báo (ghép theo index) -----
    override fun getNotificationImages(): IntArray = intArrayOf(
        R.drawable.ic_web, R.drawable.ic_wifi, R.drawable.ic_contact,
        R.drawable.ic_text, R.drawable.ic_calendar,
    )
    override fun getNotificationIconRes(): Int = R.drawable.icon_app
    override fun getNotificationChannelPrefix(): String = "awake_drive"
    override fun getNewFileNotiContentRes(): Int = R.string.sdk_new_file_content
    override fun getScreenshotNotiTitleRes(): Int = R.string.sdk_screenshot_title
    override fun getRecentDocumentsTitleRes(): Int = R.string.sdk_recent_title
    override fun getOpenTextRes(): Int = R.string.OPEN
    override fun getScanDocumentRes(): Int = R.string.scan
    override fun getWidgetButtonBackgroundRes(): Int = R.drawable.bg_widget_button
    override fun getDailyCallOpenAppContentRes(): Int = R.string.sdk_daily_content
    override fun getCheckNowTextRes(): Int = R.string.sdk_check_now
    override fun getDocumentPreviewRes(): Int = R.drawable.intro1
    override fun getFullScreenNoti1Res(): Int = R.string.sdk_fullscreen_1
    override fun getFullScreenNoti2Res(): Int = R.string.sdk_fullscreen_2
    override fun getNotificationTitles2ArrayRes(): Int = R.array.notification_title2
    override fun getNotificationMessages2ArrayRes(): Int = R.array.notification_message2
    override fun getNotificationButtons2ArrayRes(): Int = R.array.notification_button2
    override fun getNotificationOutAppTitleRes(): Int = R.string.sdk_out_app_title
    override fun getNotificationOutAppContentRes(): Int = R.string.sdk_out_app_content

    // ----- Tuỳ chọn -----
    override fun isPurchased(): Boolean = IAPUtils.isPremium()
    override fun enableAdsResume(): Boolean = !IAPUtils.isPremium()
    override fun buildDebug(): Boolean = BuildConfig.DEBUG
}
