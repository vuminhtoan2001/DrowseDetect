package com.camera.qrscanner.barcode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.camera.qrscanner.barcode.ui.nav.AppNav

/**
 * Điểm vào UI: chọn Intro (onboarding) hay Home theo chính sách số-lần-mở + nguồn cài
 * (xem [AppViewModel]). SDK lo Splash→Language→IAP trước khi mở Activity này.
 */
@Composable
fun AppRoot() {
    val vm: AppViewModel = hiltViewModel()
    val startDestination by vm.startDestination.collectAsStateWithLifecycle()
    when (val dest = startDestination) {
        null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) // đang tính gate
        else -> AppNav(startDestination = dest)
    }
}
