package com.awakedrive.drowsedetect.ui.setting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awakedrive.drowsedetect.BuildConfig
import com.awakedrive.drowsedetect.R
import com.awakedrive.drowsedetect.ui.common.AppTopBar
import com.awakedrive.drowsedetect.ui.language.MyLanguageActivity
import com.awakedrive.drowsedetect.ui.theme.PremiumGold
import com.awakedrive.drowsedetect.ui.theme.SettingIconTint
import com.google.android.play.core.review.ReviewManagerFactory

@Composable
fun SettingScreen() {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(
            title = stringResource(R.string.SETTINGS),
            actions = {
                IconButton(onClick = {
                    // Mở thẳng màn IAP (mua gói) do SDK cung cấp.
                    com.brian.base_iap.utils.NativeCodecSnowFlakeCortexAI.nativeAiStartIapActivity(context)
                }) {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = "premium", tint = PremiumGold)
                }
            },
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingRow(
                Icons.Filled.Language,
                stringResource(R.string.LANGUAGE_OPTIONS),
                onClick = {
                    // Màn Language custom của app (LANGUAGE_SCREEN_CUSTOM.md §4.2).
                    context.startActivity(
                        Intent(context, MyLanguageActivity::class.java)
                            .putExtra(MyLanguageActivity.EXTRA_FROM_SETTINGS, true),
                    )
                },
            )
            SettingRow(Icons.Filled.Share, stringResource(R.string.layout_share_btn_label), onClick = { shareApp(context) })
            SettingRow(Icons.Filled.Star, stringResource(R.string.rateApp), onClick = { requestReview(context) })

            Text(
                "Version ${BuildConfig.VERSION_CODE} - ${BuildConfig.VERSION_NAME}",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    sub: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = SettingIconTint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) {
                Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        trailing?.invoke()
    }
}

private fun shareApp(context: Context) {
    val text = "${context.getString(R.string.app_name)} - ${context.getString(R.string.share_message)}\n" +
        "android: https://play.google.com/store/apps/details?id=${context.packageName}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(Intent.createChooser(intent, null))
    } catch (_: android.content.ActivityNotFoundException) {
    }
}

private fun requestReview(context: Context) {
    val activity = context as? Activity ?: return
    val manager = ReviewManagerFactory.create(context)
    manager.requestReviewFlow().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            manager.launchReviewFlow(activity, task.result)
        } else {
            try {
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")),
                )
            } catch (_: Exception) {
            }
        }
    }
}
