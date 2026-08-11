package com.camera.qrscanner.barcode.platform

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Áp per-app locale. API33+ dùng LocaleManager, dưới đó dùng AppCompatDelegate.
 * Gọi khi SDK báo đã lưu ngôn ngữ (BaseApplication.notifyLanguageSaved).
 */
object LocaleHelper {
    fun updateLocale(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(languageCode)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        }
    }
}
