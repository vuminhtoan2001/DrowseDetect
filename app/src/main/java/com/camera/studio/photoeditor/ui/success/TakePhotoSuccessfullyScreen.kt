package com.camera.studio.photoeditor.ui.success

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camera.studio.photoeditor.R
import com.camera.studio.photoeditor.ui.components.NativeAdPlaceholder
import com.camera.studio.photoeditor.ui.theme.Primary

/** Port `screens/TakePhotoSuccessfully.js`. */
@Composable
fun TakePhotoSuccessfullyScreen(onBackHome: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.successfully),
            contentDescription = null,
            modifier = Modifier.size(160.dp),
        )
        Text(
            stringResource(R.string.success_title),
            color = Color(0xFF2E7D32),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            stringResource(R.string.success_desc),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )

        NativeAdPlaceholder("native_takepho_sucess")

        Text(
            stringResource(R.string.back_home),
            color = Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp).clickable(onClick = onBackHome),
        )
    }
}
