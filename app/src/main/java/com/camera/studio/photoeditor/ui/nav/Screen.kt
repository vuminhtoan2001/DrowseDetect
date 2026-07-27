package com.camera.studio.photoeditor.ui.nav

/**
 * Toàn bộ route trong MainActivity (Compose NavHost).
 * Lib base-application (gắn ở M8) sở hữu Splash/Intro-check/Language/IAP shell; ở đây chỉ là các
 * màn của app.
 */
enum class Screen(val route: String) {
    // Onboarding
    Intro("intro"),
    Permission("permission"),

    // Hub
    Home("home"),

    // Camera & kết quả
    Camera("camera"),
    VideoResult("video_result"),            // args: videoUri, previewWidthDp
    TakePhotoSuccessfully("take_photo_successfully"),

    // Kết quả ảnh/video (Photo/Video Detail — nền tảng cho Gallery Detail ở M7)
    ImportPhotoResult("import_photo_result"), // args: uri, isVideo

    // Gallery (Photo/Video/Templates tabs — xem M7)
    Gallery("gallery"),

    // Edit Photo (M4)
    EditPhotoPicker("edit_photo_picker"),
    EditPhotoEditor("edit_photo_editor"),    // arg: sourcePath

    // Collage Maker (M5)
    CollagePicker("collage_picker"),
    CollageEditor("collage_editor"),         // arg: "path1,path2,..." (đã encode riêng từng path)

    // Wallpaper Creator (M6)
    WallpaperHome("wallpaper_home"),
    WallpaperEditor("wallpaper_editor"),     // arg: templateId (0 = canvas trống)

    // Cài đặt
    Setting("setting");

    companion object {
        const val ARG_URI = "uri"
        const val ARG_IS_VIDEO = "isVideo"
        const val ARG_VIDEO_URI = "videoUri"
        const val ARG_PREVIEW_WIDTH_DP = "previewWidthDp"
        const val ARG_SOURCE_PATH = "sourcePath"
        const val ARG_PATHS = "paths"
        const val ARG_TEMPLATE_ID = "templateId"

        fun importPhotoResult(path: String, isVideo: Boolean) =
            "${ImportPhotoResult.route}/${android.net.Uri.encode(path)}/$isVideo"

        fun videoResult(videoUri: String, previewWidthDp: Float) =
            "${VideoResult.route}/${android.net.Uri.encode(videoUri)}/$previewWidthDp"

        fun editPhotoEditor(sourcePath: String) =
            "${EditPhotoEditor.route}/${android.net.Uri.encode(sourcePath)}"

        /** Nối nhiều path bằng "|" rồi encode cả cụm — tránh xung đột với dấu "/" trong path thật. */
        fun collageEditor(paths: List<String>) =
            "${CollageEditor.route}/${android.net.Uri.encode(paths.joinToString("|"))}"

        fun wallpaperEditor(templateId: Int) = "${WallpaperEditor.route}/$templateId"
    }
}
