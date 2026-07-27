package com.camera.studio.photoeditor.ui.shared.filter

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Component Filter dùng chung Camera/Edit Photo/Collage Maker — category tabs cuộn ngang + hàng
 * thumbnail tròn preview trực tiếp trên [previewBitmap] (khung hình hiện tại), khớp
 * `takephoto_filter.jpg`/`edit_image_filter.jpg`. [previewBitmap] null → thumbnail dùng màu nền trơn.
 */
@Composable
fun FilterPicker(
    selected: FilterOption?,
    onSelect: (FilterOption) -> Unit,
    previewBitmap: Bitmap?,
    modifier: Modifier = Modifier,
) {
    var categoryIndex by remember { mutableStateOf(0) }
    val category = FilterCategories[categoryIndex]

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            FilterCategories.forEachIndexed { i, cat ->
                val active = i == categoryIndex
                Text(
                    cat.name,
                    color = if (active) Color(0xFFF5C518) else Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { categoryIndex = i },
                )
            }
        }
        // fillMaxWidth() + spacedBy(alignment=CenterHorizontally): trước đây LazyRow không set
        // fillMaxWidth nên co lại đúng bằng độ rộng nội dung, bị Column cha căn về mép trái, để hở
        // khoảng trống bên phải (rõ nhất khi category có ít filter, luôn đủ chỗ không cần cuộn) —
        // giờ ép full width rồi căn giữa cả nhóm, vẫn cuộn được nếu 1 category có nhiều filter hơn.
        LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
            items(category.filters) { filter ->
                val isSelected = filter.name == selected?.name
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A2E))
                            .then(
                                if (isSelected) Modifier.background(Color(0xFFF5C518).copy(alpha = 0.15f)) else Modifier,
                            )
                            .clickable { onSelect(filter) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (filter.matrix == null && previewBitmap == null) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = Color.White)
                        } else if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                colorFilter = filter.matrix?.let { ColorFilter.colorMatrix(ComposeColorMatrix(it)) },
                                modifier = Modifier.size(58.dp).clip(CircleShape),
                            )
                        } else {
                            Box(Modifier.size(58.dp).background(Color(0xFF3A3A40), CircleShape))
                        }
                    }
                    Text(filter.name, color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}
