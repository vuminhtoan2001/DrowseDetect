package com.camera.studio.photoeditor.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.brian.base_application.language.LanguageActivity
import com.camera.studio.photoeditor.R
import com.camera.studio.photoeditor.core.MainActivity
import com.camera.studio.photoeditor.platform.IapLauncher
import com.camera.studio.photoeditor.ui.components.AppHeader
import com.camera.studio.photoeditor.ui.components.NativeAdPlaceholder

private val SettingsAccent = Color(0xFFF5C518)

/**
 * Settings — Language/Share/Rate/Premium. Language (M8) mở đúng màn `LanguageActivity` của
 * base-application (KHÔNG dùng màn Settings hệ thống Android nữa) — cùng 1 màn với lúc chọn ngôn
 * ngữ lần đầu (Splash→Language), đổi xong tự áp cho MainActivity qua `notifyLanguageSaved`. Premium
 * mở paywall thật qua [IapLauncher].
 */
@Composable
fun SettingScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    val versionName = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrDefault("")
    var showRateDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppHeader(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
            centerTitle = false,
            actions = {
                Text("v-$versionName", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
            },
        )

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingRow(Icons.Default.Language, stringResource(R.string.language)) {
                (context as? FragmentActivity)?.let { LanguageActivity.start(it, MainActivity::class.java) }
            }
            SettingRow(Icons.Default.Share, stringResource(R.string.share_app)) {
                val text = "$appName\nhttps://play.google.com/store/apps/details?id=${context.packageName}"
                context.startActivity(
                    Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, null),
                )
            }
            SettingRow(Icons.Default.Star, stringResource(R.string.rate_app)) { showRateDialog = true }
            SettingRow(Icons.Default.WorkspacePremium, stringResource(R.string.premium_package)) {
                IapLauncher.open(context)
            }

            NativeAdPlaceholder("native_settings")
        }
    }

    if (showRateDialog) {
        RateAppDialog(
            appName = appName,
            onDismiss = { showRateDialog = false },
            onSend = { stars ->
                showRateDialog = false
                if (stars >= 4) {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${context.packageName}")),
                        )
                    }
                }
            },
        )
    }
}

/** Button to, có box-shadow rõ để phân biệt từng dòng thay vì list phẳng như trước. */
@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(SettingsAccent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, contentDescription = null, tint = SettingsAccent, modifier = Modifier.size(22.dp)) }
                Text(title, modifier = Modifier.padding(start = 16.dp), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
