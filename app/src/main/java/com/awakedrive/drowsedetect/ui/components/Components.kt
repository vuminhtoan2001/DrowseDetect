package com.awakedrive.drowsedetect.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awakedrive.drowsedetect.service.MonitorStateHolder
import com.awakedrive.drowsedetect.ui.theme.BrandBlue
import com.awakedrive.drowsedetect.ui.theme.BrandCyan
import com.awakedrive.drowsedetect.ui.theme.Navy600
import com.awakedrive.drowsedetect.ui.theme.Navy700
import com.awakedrive.drowsedetect.ui.theme.Navy800
import com.awakedrive.drowsedetect.ui.theme.Navy900
import com.awakedrive.drowsedetect.ui.theme.StateRed

/** Nền toàn màn: gradient navy, có thể pha màu trạng thái (xanh/vàng/đỏ) ở phía trên. */
@Composable
fun ScreenBackground(tint: Color = BrandBlue, content: @Composable BoxScope.() -> Unit) {
    // Text của Material3 mặc định dùng LocalContentColor (đen) nên phải đặt trắng cho nền tối.
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(tint.copy(alpha = 0.28f).compositeOver(Navy900), Navy900, Navy800)),
            ),
            content = content,
        )
    }
}

private fun Color.compositeOver(bg: Color): Color {
    val a = alpha
    return Color(red * a + bg.red * (1 - a), green * a + bg.green * (1 - a), blue * a + bg.blue * (1 - a), 1f)
}

/** Thẻ kính mờ viền xanh dùng cho mọi khối nội dung. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BrandBlue.copy(alpha = 0.35f),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Navy700.copy(alpha = 0.85f))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    colors: List<Color> = listOf(BrandBlue, BrandCyan),
) {
    Row(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(Brush.horizontalGradient(colors))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            Box(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Color.White) {
    Row(
        modifier
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .border(BorderStroke(1.5.dp, color.copy(alpha = 0.6f)), RoundedCornerShape(27.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.7f)), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, textAlign = TextAlign.Center)
    }
}

/** Ô icon tròn dùng cho hàng thông tin. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** Ô số liệu nhỏ: nhãn + giá trị lớn. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Navy600.copy(alpha = 0.5f))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Khung hiển thị camera trước (chỉ để người dùng căn vị trí, không vẽ gì lên hình).
 * Gắn SurfaceProvider khi đang hiển thị, gỡ khi rời màn; service vẫn phân tích ở nền.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderColor: Color = Color.Transparent,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val preview by MonitorStateHolder.preview.collectAsStateWithLifecycle()
    val view = remember { PreviewView(context).apply {
            // TextureView để clip bo tròn (khung hiệu chuẩn hình tròn) hoạt động đúng
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        } }
    DisposableEffect(preview, view) {
        preview?.setSurfaceProvider(view.surfaceProvider)
        onDispose { preview?.setSurfaceProvider(null) }
    }
    Box(modifier.clip(shape).background(Navy800).border(BorderStroke(3.dp, borderColor), shape)) {
        AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
        overlay()
    }
}

/** Chấm đỏ "đang giám sát" góc trên trái khung camera. */
@Composable
fun RecBadge(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x99000000))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(StateRed))
        Box(Modifier.size(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}
