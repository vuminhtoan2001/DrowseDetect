package com.awakedrive.drowsedetect.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import com.awakedrive.drowsedetect.R

/** Hint "chạm vào đây" (RN assets/images/hand_click.gif) — animate bằng Coil. */
@Composable
fun HandClickHint(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val loader = remember {
        ImageLoader.Builder(context)
            .components { add(ImageDecoderDecoder.Factory()) }
            .build()
    }
    AsyncImage(
        model = R.drawable.hand_click,
        imageLoader = loader,
        contentDescription = null,
        modifier = modifier,
    )
}
