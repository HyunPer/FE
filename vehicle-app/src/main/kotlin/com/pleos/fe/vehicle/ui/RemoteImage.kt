package com.pleos.fe.vehicle.ui

import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 서버 이미지(테마 배경·캐릭터) 표시. 이미지 몇 장만 쓰므로 로더 라이브러리 없이 내려받아 메모리에 캐시한다.
 * 실패하면 아무것도 그리지 않는다(테마 색 배경은 그대로).
 */
@Composable
fun RemoteImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
) {
    val bitmap by produceState(ImageCache.get(url), url) {
        if (value == null) value = withContext(Dispatchers.IO) { ImageCache.load(url) }
    }
    bitmap?.let { Image(it, contentDescription, modifier, contentScale = contentScale) }
}

private object ImageCache {
    private val cache = LruCache<String, ImageBitmap>(16)

    fun get(url: String): ImageBitmap? = cache.get(url)

    fun load(url: String): ImageBitmap? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        try {
            connection.inputStream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        } finally {
            connection.disconnect()
        }
    }.onFailure { Log.w("RemoteImage", "이미지 불러오기 실패 $url: ${it.message}") }
        .getOrNull()
        ?.also { cache.put(url, it) }
}
