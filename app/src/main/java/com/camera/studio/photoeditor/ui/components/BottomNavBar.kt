package com.camera.studio.photoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camera.studio.photoeditor.R
import com.camera.studio.photoeditor.ui.theme.Primary

enum class BottomNavTab { HOME, SETTING }

/** Thanh điều hướng dưới: Home / Camera(giữa) / Setting — dùng chung Home & Setting (RN: 3 icon cố định). */
@Composable
fun BottomNavBar(current: BottomNavTab, onHome: () -> Unit, onCamera: () -> Unit, onSetting: () -> Unit) {
    // Bỏ ripple mặc định (hộp xám phủ cả Column khi nhấn) cho 2 tab Home/Setting — chỉ dùng màu cam
    // (tint đổi theo `current`) làm phản hồi trạng thái, không cần thêm hiệu ứng nhấn nào khác.
    val homeInteraction = remember { MutableInteractionSource() }
    val settingInteraction = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(interactionSource = homeInteraction, indication = null, onClick = onHome),
        ) {
            Icon(Icons.Default.Home, contentDescription = null, tint = if (current == BottomNavTab.HOME) Primary else Color.Gray)
            Text(stringResource(R.string.home), color = if (current == BottomNavTab.HOME) Primary else Color.Gray, fontSize = 11.sp)
        }
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Primary)
                .clickable(onClick = onCamera),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(interactionSource = settingInteraction, indication = null, onClick = onSetting),
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = if (current == BottomNavTab.SETTING) Primary else Color.Gray)
            Text(stringResource(R.string.settings_title), color = if (current == BottomNavTab.SETTING) Primary else Color.Gray, fontSize = 11.sp)
        }
    }
}
