package com.camera.studio.photoeditor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.camera.studio.photoeditor.R
import com.camera.studio.photoeditor.ui.theme.Primary

/**
 * Port `components/LayoutUI/RateAppSheet.js` — chọn sao rồi bấm Send. >=4 sao → mở Store; dưới 4
 * sao → chỉ đóng (KHÔNG chê bai công khai lên Store, tránh review xấu — pattern "smart rating").
 */
@Composable
fun RateAppDialog(appName: String, onDismiss: () -> Unit, onSend: (stars: Int) -> Unit) {
    var star by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${stringResource(R.string.rate_header)} $appName?", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
                (1..5).forEach { i ->
                    Icon(
                        if (star >= i) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (star >= i) Color(0xFFF4C63C) else Color.Gray,
                        modifier = Modifier.padding(horizontal = 4.dp).clickable { star = i },
                    )
                }
            }
        },
        confirmButton = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary)
                    .clickable(enabled = star > 0) { onSend(star) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.send), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
    )
}
