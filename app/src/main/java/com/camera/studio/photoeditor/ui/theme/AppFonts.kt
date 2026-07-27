package com.camera.studio.photoeditor.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.camera.studio.photoeditor.R

/**
 * Bộ font gốc của app tham chiếu (11 họ, `old_assets/.../fonts/`) — danh sách hiển thị trong Font
 * picker của Text tool (Edit Photo/Collage Maker/Wallpaper Creator, xem
 * docs/07_SHARED_COMPONENTS.md §Text). `displayName` là nhãn hiển thị trên UI.
 */
data class AppFontOption(val displayName: String, val family: FontFamily)

val AppFontOptions: List<AppFontOption> = listOf(
    AppFontOption(
        "Inter",
        FontFamily(
            Font(R.font.inter_regular, FontWeight.Normal),
            Font(R.font.inter_medium, FontWeight.Medium),
            Font(R.font.inter_semi_bold, FontWeight.SemiBold),
            Font(R.font.inter_bold, FontWeight.Bold),
        ),
    ),
    AppFontOption("Numans", FontFamily(Font(R.font.numans_regular, FontWeight.Normal))),
    AppFontOption(
        "Nunito",
        FontFamily(
            Font(R.font.nunito_regular, FontWeight.Normal),
            Font(R.font.nunito_medium, FontWeight.Medium),
            Font(R.font.nunito_semi_bold, FontWeight.SemiBold),
            Font(R.font.nunito_bold, FontWeight.Bold),
        ),
    ),
    AppFontOption(
        "Open Sans",
        FontFamily(
            Font(R.font.open_sans_regular, FontWeight.Normal),
            Font(R.font.open_sans_medium, FontWeight.Medium),
            Font(R.font.open_sans_semi_bold, FontWeight.SemiBold),
            Font(R.font.open_sans_bold, FontWeight.Bold),
        ),
    ),
    AppFontOption(
        "Orbitron",
        FontFamily(
            Font(R.font.orbitron_regular, FontWeight.Normal),
            Font(R.font.orbitron_medium, FontWeight.Medium),
            Font(R.font.orbitron_semi_bold, FontWeight.SemiBold),
            Font(R.font.orbitron_bold, FontWeight.Bold),
        ),
    ),
    AppFontOption("Orelega One", FontFamily(Font(R.font.orelega_one_regular, FontWeight.Normal))),
    AppFontOption("Parisienne", FontFamily(Font(R.font.parisienne_regular, FontWeight.Normal))),
    AppFontOption("Peddana", FontFamily(Font(R.font.peddana_regular, FontWeight.Normal))),
    AppFontOption("Poetsen One", FontFamily(Font(R.font.poetsen_one_regular, FontWeight.Normal))),
    AppFontOption("Rammetto One", FontFamily(Font(R.font.rammetto_one_regular, FontWeight.Normal))),
    AppFontOption(
        "Readex Pro",
        FontFamily(
            Font(R.font.readex_pro_regular, FontWeight.Normal),
            Font(R.font.readex_pro_medium, FontWeight.Medium),
            Font(R.font.readex_pro_semi_bold, FontWeight.SemiBold),
            Font(R.font.readex_pro_bold, FontWeight.Bold),
        ),
    ),
)
