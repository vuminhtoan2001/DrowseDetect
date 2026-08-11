package com.camera.qrscanner.barcode.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.camera.qrscanner.barcode.ui.common.CheckForAppUpdateWithDialog

/**
 * Placeholder Home — thay bằng UI/logic tính năng thật của app khi build từ khung sườn này.
 * Giữ nguyên [CheckForAppUpdateWithDialog] — CHỈ gọi 1 lần ở đây (màn gốc), không copy sang màn khác.
 */
@Composable
fun HomeScreen() {
    CheckForAppUpdateWithDialog()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Text("Home — TODO: build màn tính năng thật ở đây", style = MaterialTheme.typography.bodyMedium)
    }
}
