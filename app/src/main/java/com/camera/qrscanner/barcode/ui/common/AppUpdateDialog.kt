package com.camera.qrscanner.barcode.ui.common

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.camera.qrscanner.barcode.R
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.nlbn.ads.util.AppOpenManager

private const val TAG = "AppUpdate"

/**
 * Kiểm tra có bản cập nhật trên Play không (Play Core App Update API). Nếu có → hiện dialog.
 * Đặt ở màn Home; check 1 lần mỗi lần vào màn.
 */
@Composable
fun CheckForAppUpdateWithDialog() {
    val context = LocalContext.current
    var showUpdateDialog by remember { mutableStateOf(false) }
    val hasChecked = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (hasChecked.value) return@LaunchedEffect

        val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                showUpdateDialog = true
            }
            hasChecked.value = true
        }.addOnFailureListener { e ->
            Log.w(TAG, "Failed to check for updates: ${e.message}")
            hasChecked.value = true
        }
    }

    if (showUpdateDialog) {
        AppUpdateDialog(
            onDismiss = { showUpdateDialog = false },
        )
    }
}

@Composable
fun AppUpdateDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                // Tiêu đề
                Text(
                    text = stringResource(id = R.string.update_isReady_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mô tả
                Text(
                    text = stringResource(id = R.string.newVersion_isReady_mss),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Nút "Update" → mở Play Store trang app hiện tại
                Button(
                    onClick = {
                        // Chặn resume-ad khi rời app sang Play Store (SDK com.nlbn.ads)
                        AppOpenManager.getInstance().disableAdResumeByClickAction()
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                            setPackage("com.android.vending")
                        }
                        runCatching { context.startActivity(intent) }.onFailure {
                            // Fallback: mở bằng trình duyệt nếu chưa cài Play Store
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"),
                                ),
                            )
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        text = stringResource(id = R.string.update_isReady_btn_update),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tùy chọn "Later"
                Text(
                    text = stringResource(id = R.string.update_isReady_btn_late),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(8.dp),
                )
            }
        }
    }
}
