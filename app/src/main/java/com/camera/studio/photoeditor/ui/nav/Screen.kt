package com.camera.studio.photoeditor.ui.nav

/**
 * Toàn bộ route trong MainActivity (Compose NavHost).
 * Lib base-application (gắn ở M8) sở hữu Splash/Intro-check/Language/IAP shell; ở đây chỉ là các
 * màn của app. Camera/Gallery/EditPhotoPicker/CollagePicker/WallpaperHome hiện là placeholder
 * `ComingSoon` — thay bằng màn thật khi bắt đầu tính năng tương ứng.
 */
enum class Screen(val route: String) {
    Intro("intro"),
    Permission("permission"),
    Home("home"),
    Camera("camera"),
    Gallery("gallery"),
    EditPhotoPicker("edit_photo_picker"),
    CollagePicker("collage_picker"),
    WallpaperHome("wallpaper_home"),
    Setting("setting"),
}
