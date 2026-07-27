package com.camera.studio.photoeditor.ui.shared.filter

/**
 * Bộ Filter dùng chung Camera/Edit Photo/Collage Maker (xem docs/07_SHARED_COMPONENTS.md §Filter).
 * Mỗi filter là 1 ma trận màu 4x5 (định dạng `android.graphics.ColorMatrix`/`androidx.compose.ui
 * .graphics.ColorMatrix` dùng chung 1 FloatArray 20 phần tử) — áp được cả lên preview (qua
 * `View.setLayerType(LAYER_TYPE_HARDWARE, Paint().apply{colorFilter=...})`) lẫn bake vào bitmap cuối
 * (qua `Canvas`+`Paint`), đảm bảo ảnh chụp/lưu khớp đúng những gì thấy ở preview.
 */
data class FilterOption(val name: String, val matrix: FloatArray?) // null = "No filter"

data class FilterCategory(val name: String, val filters: List<FilterOption>)

private fun identity() = null

private fun m(
    rMul: Float = 1f, gMul: Float = 1f, bMul: Float = 1f,
    rAdd: Float = 0f, gAdd: Float = 0f, bAdd: Float = 0f,
    sat: Float = 1f,
): FloatArray {
    // Ma trận bão hoà chuẩn (luminance weights) nhân với scale/offset kênh màu.
    val invSat = 1 - sat
    val lr = 0.2126f * invSat; val lg = 0.7152f * invSat; val lb = 0.0722f * invSat
    val satM = floatArrayOf(
        lr + sat, lg, lb, 0f, 0f,
        lr, lg + sat, lb, 0f, 0f,
        lr, lg, lb + sat, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
    // Nhân thêm scale/offset kênh (áp sau bão hoà).
    return floatArrayOf(
        satM[0] * rMul, satM[1] * rMul, satM[2] * rMul, 0f, rAdd * 255f,
        satM[5] * gMul, satM[6] * gMul, satM[7] * gMul, 0f, gAdd * 255f,
        satM[10] * bMul, satM[11] * bMul, satM[12] * bMul, 0f, bAdd * 255f,
        0f, 0f, 0f, 1f, 0f,
    )
}

private fun grayscale() = m(sat = 0f)

val FilterCategories: List<FilterCategory> = listOf(
    FilterCategory(
        "Popular",
        listOf(
            FilterOption("None", identity()),
            FilterOption("Warm", m(rMul = 1.12f, gMul = 1.02f, bMul = 0.88f, sat = 1.05f)),
            FilterOption("Cool", m(rMul = 0.9f, gMul = 1.0f, bMul = 1.15f, sat = 1.05f)),
            FilterOption("Vivid", m(sat = 1.45f)),
            FilterOption("B&W", grayscale()),
        ),
    ),
    FilterCategory(
        "Adventure",
        listOf(
            FilterOption("None", identity()),
            FilterOption("Forest", m(rMul = 0.95f, gMul = 1.12f, bMul = 0.9f, sat = 1.15f)),
            FilterOption("Desert", m(rMul = 1.15f, gMul = 1.0f, bMul = 0.75f, sat = 1.1f)),
            FilterOption("Arctic", m(rMul = 0.92f, gMul = 1.02f, bMul = 1.18f, sat = 0.9f)),
        ),
    ),
    FilterCategory(
        "Blue Shadow",
        listOf(
            FilterOption("None", identity()),
            FilterOption("Midnight", m(rMul = 0.75f, gMul = 0.85f, bMul = 1.25f, sat = 0.85f)),
            FilterOption("Ocean", m(rMul = 0.8f, gMul = 1.0f, bMul = 1.3f, sat = 1.1f)),
            FilterOption("Steel", m(rMul = 0.88f, gMul = 0.92f, bMul = 1.05f, sat = 0.55f)),
        ),
    ),
    FilterCategory(
        "Retro",
        listOf(
            FilterOption("None", identity()),
            FilterOption("Sepia", m(rMul = 1.15f, gMul = 0.95f, bMul = 0.7f, sat = 0.55f)),
            FilterOption("Faded", m(rMul = 1.02f, gMul = 1.0f, bMul = 0.96f, rAdd = 0.06f, gAdd = 0.05f, bAdd = 0.05f, sat = 0.7f)),
            FilterOption("Vintage", m(rMul = 1.08f, gMul = 0.98f, bMul = 0.82f, rAdd = 0.03f, sat = 0.8f)),
        ),
    ),
)
