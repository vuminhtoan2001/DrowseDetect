package com.camera.studio.photoeditor

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application host — TẠM THỜI là [Application] thường ở giai đoạn build core feature (M0-M7).
 *
 * Ở M8 sẽ đổi lại thành `MyApplication : BaseApplication()` (thư viện base-application AAR),
 * implement các abstract method cho branding/splash/IAP-key/notification-arrays/ads-config,
 * theo đúng PLAYBOOK_TICH_HOP.md + HANDOFF_NEXT_APP.md — tham khảo bản gốc ở
 * vga34b-kotlin/app/src/main/java/com/camera/geostamp/MyApplication.kt (không sửa file đó).
 */
@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
