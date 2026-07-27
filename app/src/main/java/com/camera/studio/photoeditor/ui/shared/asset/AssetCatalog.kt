package com.camera.studio.photoeditor.ui.shared.asset

import android.content.Context

/**
 * Liệt kê asset gốc (frame/sticker/wallpaper-template/template-gallery) trực tiếp từ
 * `assets/images/...` bằng `AssetManager.list()` — không hard-code số lượng file/category vì mỗi
 * bộ có số lượng khác nhau (19-23 file, xem docs/08_ASSET_MAPPING.md).
 */
object AssetCatalog {

    const val FRAME_CATEGORIES = "cute,floral,party,travel,vintage"
    const val STICKER_CATEGORIES = "cute,kawaii,minimal,party,vintage"
    const val WALLPAPER_TEMPLATE_CATEGORIES = "birthday,halloween,nature,spring,summer"

    private fun listFiles(context: Context, path: String): List<String> =
        runCatching { context.assets.list(path)?.toList() ?: emptyList() }.getOrDefault(emptyList())
            .filter { it.endsWith(".png", true) || it.endsWith(".webp", true) || it.endsWith(".jpg", true) }
            .sortedWith(compareBy({ it.length }, { it }))

    /** Trả về danh sách asset path (dùng với Coil `"file:///android_asset/$path"`) theo category. */
    fun framesInCategory(context: Context, category: String): List<String> =
        listFiles(context, "images/frame/$category").map { "images/frame/$category/$it" }

    fun stickersInCategory(context: Context, category: String): List<String> =
        listFiles(context, "images/stickers/$category").map { "images/stickers/$category/$it" }

    fun wallpaperTemplatesInCategory(context: Context, category: String): List<String> =
        listFiles(context, "images/templates/$category").map { "images/templates/$category/$it" }

    /** 20 template `templates_v2/template_N/{background,frame}.webp` — dùng cho Templates gallery (Collage/Wallpaper). */
    fun templateGalleryIds(context: Context): List<Int> =
        runCatching { context.assets.list("images/templates_v2")?.toList() ?: emptyList() }.getOrDefault(emptyList())
            .filter { it.startsWith("template_") && !it.endsWith(".webp", true) }
            .mapNotNull { it.removePrefix("template_").toIntOrNull() }
            .sorted()

    fun templatePreviewPath(id: Int) = "images/templates_v2/template_$id.webp"
    fun templateBackgroundPath(id: Int) = "images/templates_v2/template_$id/background.webp"
    fun templateFramePath(id: Int) = "images/templates_v2/template_$id/frame.webp"

    fun assetUri(path: String) = "file:///android_asset/$path"
}
