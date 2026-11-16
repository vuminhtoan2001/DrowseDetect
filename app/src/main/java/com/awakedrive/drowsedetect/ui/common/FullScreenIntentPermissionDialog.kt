package com.awakedrive.drowsedetect.ui.common

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nlbn.ads.util.AppOpenManager
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.ui.theme.Theme
import com.awakedrive.drowsedetect.ui.theme.ThemeSecond

private const val TAG = "FullScreenIntentPerm"

/**
 * "Mỗi phiên chỉ hỏi 1 lần" (docs/update-sdk-language/FULL_SCREEN_INTENT_PERMISSION.md §3.1).
 *
 * KHÔNG dùng biến static thường: Android không kill process khi user bấm Home/Back thoát app, biến
 * static sống theo process nên vào lại app vẫn giữ `true` → không hỏi lại. Reset ở
 * `ProcessLifecycleOwner.onStop` (toàn app rời foreground; không tính chuyển Activity / xoay màn).
 */
private object FullScreenIntentPromptState {
    var askedThisSession = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                Log.d(TAG, "ProcessLifecycleOwner.onStop → reset askedThisSession")
                askedThisSession = false
            }
        })
    }
}

/**
 * Android < 14: `USE_FULL_SCREEN_INTENT` là quyền thường (AAR đã khai) → coi như luôn có — KHÔNG có
 * nghĩa đã xin được, mà là không cần xin. Android 14+: user phải tự bật trong Settings.
 */
private fun isFullScreenIntentGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val granted = nm.canUseFullScreenIntent()
    Log.d(TAG, "SDK_INT=${Build.VERSION.SDK_INT} canUseFullScreenIntent=$granted")
    return granted
}

private fun openFullScreenIntentSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    // Rời app sang Settings do app chủ động → bỏ qua ads-resume lần quay lại kế tiếp (§4)
    AppOpenManager.getInstance().disableAdResumeByClickAction()
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    }.onSuccess {
        Log.d(TAG, "Opened full-screen intent settings")
    }.onFailure { e ->
        Log.w(TAG, "Failed to open full-screen intent settings: ${e.message}")
    }
}

/**
 * Ở Home: chưa có quyền full-screen intent thì hiện dialog mời bật. Mỗi phiên foreground hỏi tối đa
 * 1 lần; chạy độc lập với [CheckForAppUpdateWithDialog].
 */
@Composable
fun CheckFullScreenIntentPermission() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d(TAG, "check: askedThisSession=${FullScreenIntentPromptState.askedThisSession}")
        if (FullScreenIntentPromptState.askedThisSession) return@LaunchedEffect
        FullScreenIntentPromptState.askedThisSession = true
        if (!isFullScreenIntentGranted(context)) showDialog = true
        Log.d(TAG, "showDialog=$showDialog")
    }

    if (showDialog) {
        FullScreenIntentPermissionDialog(
            onGrant = {
                Log.d(TAG, "User tapped Allow")
                showDialog = false
                openFullScreenIntentSettings(context)
            },
            onDismiss = {
                Log.d(TAG, "User tapped Not now / dismissed")
                showDialog = false
            },
        )
    }
}

// Bảng màu popup: màu chủ đạo của app (Theme → ThemeSecond) cho nút CTA, bóng và hào quang; nền thẻ trắng → xanh nhạt.
private val AccentStart = Theme
private val AccentEnd = ThemeSecond
private val CardTop = Color(0xFFFFFFFF)
private val CardBottom = Color(0xFFE9F1FF)
private val TitleColor = Color(0xFF14142B)
private val BodyColor = Color(0xFF6B6B80)

@Composable
private fun FullScreenIntentPermissionDialog(
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        val cardShape = RoundedCornerShape(28.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(24.dp, cardShape, ambientColor = AccentStart, spotColor = AccentStart)
                .clip(cardShape)
                .background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
                .border(1.dp, Color.White.copy(alpha = 0.8f), cardShape),
        ) {
            // Nút đóng góc phải
            Icon(
                Icons.Filled.Close,
                contentDescription = null,
                tint = BodyColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onDismiss() }
                    .padding(6.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Hero icon + hào quang tỏa sáng phía sau
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(132.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(AccentEnd.copy(alpha = 0.28f), Color.Transparent),
                                ),
                                CircleShape,
                            ),
                    )
                    Image(
                        painter = painterResource(id = R.drawable.notification_bell),
                        contentDescription = null,
                        modifier = Modifier.size(84.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.full_screen_intent_permission_title),
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = TitleColor,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(id = R.string.full_screen_intent_permission_message),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    color = BodyColor,
                )

                Spacer(Modifier.height(24.dp))

                // CTA gradient + dấu tích
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.horizontalGradient(listOf(AccentStart, AccentEnd)))
                        .clickable { onGrant() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.full_screen_intent_permission_grant),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.size(8.dp))
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(id = R.string.full_screen_intent_permission_later),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = BodyColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}
