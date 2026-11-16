package com.awakedrive.drowsedetect.ui.language

import java.util.Locale

/** 1 dòng trong danh sách chọn ngôn ngữ. [code] là BCP-47 tag truyền cho LocaleHelper/LanguageRouter. */
data class LanguageItem(
    val code: String,
    val nativeName: String,
    val flagEmoji: String,
)

/**
 * Danh sách ngôn ngữ hỗ trợ (docs/update-sdk-language/LANGUAGE_SCREEN_CUSTOM.md §5.3) — PHẢI khớp chính xác các thư mục `values-xx` có trong
 * `app/src/main/res` (hiện tại: ar, de, es, fa, fr, hi, id, it, iw, ja, km, ko, nl, pt, ru, th, tr,
 * uk, vi, zh-rCN + en mặc định). Thêm/bớt ở đây thì phải thêm/bớt tương ứng thư mục
 * `values-<code>/strings.xml`, nếu không app hiện ngôn ngữ trong danh sách chọn nhưng UI vẫn hiển thị
 * tiếng Anh (fallback) do thiếu bản dịch.
 */
object SupportedLanguages {
    val list = listOf(
        LanguageItem("en", "English", "🇺🇸"),
        LanguageItem("ar", "العربية", "🇸🇦"),
        LanguageItem("de", "Deutsch", "🇩🇪"),
        LanguageItem("es", "Español", "🇪🇸"),
        LanguageItem("fa", "فارسی", "🇮🇷"),
        LanguageItem("fr", "Français", "🇫🇷"),
        LanguageItem("hi", "हिन्दी", "🇮🇳"),
        LanguageItem("id", "Bahasa Indonesia", "🇮🇩"),
        LanguageItem("it", "Italiano", "🇮🇹"),
        LanguageItem("iw", "עברית", "🇮🇱"),
        LanguageItem("ja", "日本語", "🇯🇵"),
        LanguageItem("km", "ភាសាខ្មែរ", "🇰🇭"),
        LanguageItem("ko", "한국어", "🇰🇷"),
        LanguageItem("nl", "Nederlands", "🇳🇱"),
        LanguageItem("pt", "Português", "🇵🇹"),
        LanguageItem("ru", "Русский", "🇷🇺"),
        LanguageItem("th", "ภาษาไทย", "🇹🇭"),
        LanguageItem("tr", "Türkçe", "🇹🇷"),
        LanguageItem("uk", "Українська", "🇺🇦"),
        LanguageItem("vi", "Tiếng Việt", "🇻🇳"),
        LanguageItem("zh-CN", "简体中文", "🇨🇳"),
    )
}

/**
 * Ngôn ngữ máy → 1 code trong [SupportedLanguages.list], dùng cho auto-select (§7.1 doc). Xử lý quirk
 * `Locale.getDefault().language` trả `"in"`/`"iw"` cho Indonesia/Hebrew tuỳ phiên bản JVM. Không khớp
 * gì trong danh sách → fallback `"en"`.
 */
fun detectDeviceLanguageCode(): String {
    val locale = Locale.getDefault()
    val lang = when (locale.language) {
        "in" -> "id"
        "he" -> "iw"
        else -> locale.language
    }
    val tag = if (lang == "zh") "zh-CN" else lang
    return SupportedLanguages.list.firstOrNull { it.code.equals(tag, ignoreCase = true) }?.code
        ?: SupportedLanguages.list.firstOrNull { it.code.equals(lang, ignoreCase = true) }?.code
        ?: "en"
}
