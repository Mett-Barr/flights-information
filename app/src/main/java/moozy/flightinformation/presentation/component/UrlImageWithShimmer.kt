package moozy.flightinformation.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import moozy.flightinformation.presentation.component.modifier.shimmerEffect


@Composable
fun UrlImageWithShimmer(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Box(modifier = modifier) {
        if (url.isNullOrEmpty()) {
            // 空 URL → 直接顯示淺灰背景
            Box(modifier = Modifier
                .matchParentSize()
                .background(Color.LightGray))
        } else {
            var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    imageState = state
                }
            )

            when (imageState) {
                is AsyncImagePainter.State.Loading -> {
                    // loading 時顯示 shimmer 背景
                    Box(modifier = Modifier.matchParentSize().shimmerEffect())
                }
                is AsyncImagePainter.State.Error -> {
                    // 錯誤時顯示淺灰背景 + 錯誤提示
                    Box(modifier = Modifier
                        .matchParentSize()
                        .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("圖片載入錯誤")
                    }
                }
                is AsyncImagePainter.State.Success -> {
                    // 成功時什麼都不做，圖片已被 AsyncImage 顯示
                }
                else -> {
                    // 初始或其他狀態也用 shimmer
                    Box(modifier = Modifier.matchParentSize().shimmerEffect())
                }
            }
        }
    }
}