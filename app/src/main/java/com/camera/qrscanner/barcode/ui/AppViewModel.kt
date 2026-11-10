package com.camera.qrscanner.barcode.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brian.base_iap.utils.FirebaseRemoteConfigUtil
import com.camera.qrscanner.barcode.ads.CampaignHelper
import com.camera.qrscanner.barcode.data.repository.SettingsRepository
import com.camera.qrscanner.barcode.platform.AppEvents
import com.camera.qrscanner.barcode.ui.nav.Dest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Gate mở app: quyết định vào ONBOARDING (Intro) hay HOME theo CHÍNH SÁCH số-lần-mở + nguồn cài
 * (port RN AppNavigation: count_app_open + organic_number_not_guide + isAdsCampaign), THAY cho
 * cờ first-open một-lần.
 *
 * - goToHomeNumber = số lần mở app (DataStore, +1 mỗi lần khởi động).
 * - Ads install: goToHomeNumber >= count_app_open → Home; nhỏ hơn → Intro (vài lần đầu xem onboarding).
 * - Organic: < organic_number_not_guide HOẶC >= count_app_open + organic_number_not_guide → Home;
 *   khoảng giữa → Intro (trì hoãn guide cho organic).
 *
 * SDK là launcher (Splash→Language→IAP) rồi mở Activity này; gate chỉ chọn Intro vs Home.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val repo: SettingsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // null = đang tính; else = startDestination cho AppNav.
    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination

    init {
        viewModelScope.launch {
            val goToHomeNumber = repo.incrementAppOpen()
            val isAdsCampaign = resolveCampaign()

            val frc = FirebaseRemoteConfigUtil.getInstance()
            val countAppOpen = frc.getLong("count_app_open").toInt().coerceAtLeast(1)
            val organicNotGuide = frc.getLong("organic_number_not_guide").toInt().coerceAtLeast(0)

            val goToHome = if (isAdsCampaign) {
                goToHomeNumber > countAppOpen
            } else {
                goToHomeNumber <= organicNotGuide ||
                    goToHomeNumber > countAppOpen + organicNotGuide
            }
            // Vì sao user có / không thấy Intro (SCREEN_TRACKING.md §3.10). ViewModel sống qua recreate()
            // nên tự đúng điều kiện "chỉ 1 lần mỗi lần mở app".
            AppEvents.log(
                context, "onboarding_route",
                "intro_shown" to !goToHome,
                "is_ads_campaign" to isAdsCampaign,
                "open_count" to goToHomeNumber,
            )
            _startDestination.value = if (goToHome) Dest.HOME else Dest.ONB_INTRO
        }
    }

    private suspend fun resolveCampaign(): Boolean {
        repo.cachedAdsCampaign()?.let { return it }
        val v = try {
            CampaignHelper.resolveIsAdsCampaign(context)
        } catch (e: Exception) {
            true
        }
        repo.setAdsCampaign(v)
        return v
    }
}
