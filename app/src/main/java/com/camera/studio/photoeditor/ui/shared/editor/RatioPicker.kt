package com.camera.studio.photoeditor.ui.shared.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class RatioOption(val label: String, val widthRatio: Float, val heightRatio: Float) {
    SQUARE("1/1", 1f, 1f),
    R4_3("4/3", 4f, 3f),
    R3_4("3/4", 3f, 4f),
    R16_9("16/9", 16f, 9f),
    R9_16("9/16", 9f, 16f),
    R3_2("3/2", 3f, 2f),
}

/** 6 tỉ lệ dùng chung Collage Maker/Wallpaper — khớp `collage_ratio.jpg`. */
@Composable
fun RatioPicker(selected: RatioOption, onSelect: (RatioOption) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        RatioOption.entries.forEach { opt ->
            val isSelected = opt == selected
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSelect(opt) }) {
                val previewMax = 18f
                val w = if (opt.widthRatio >= opt.heightRatio) previewMax else previewMax * (opt.widthRatio / opt.heightRatio)
                val h = if (opt.heightRatio >= opt.widthRatio) previewMax else previewMax * (opt.heightRatio / opt.widthRatio)
                Box(Modifier.width(w.dp).height(h.dp).background(if (isSelected) Color(0xFFF5C518) else Color.White.copy(alpha = 0.3f)))
                Text(opt.label, color = if (isSelected) Color(0xFFF5C518) else Color.White, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
