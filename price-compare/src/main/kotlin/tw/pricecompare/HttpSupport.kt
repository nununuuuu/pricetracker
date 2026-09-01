package tw.pricecompare

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

internal val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .callTimeout(20, TimeUnit.SECONDS)
    .build()

internal suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String? = withContext(Dispatchers.IO) {
    runCatching {
        val request = Request.Builder().url(url).apply { headers.forEach { (key, value) -> header(key, value) } }.build()
        httpClient.newCall(request).execute().use { response -> response.body?.string()?.takeIf { response.isSuccessful } }
    }.getOrNull()
}

internal suspend fun post(url: String, json: String, headers: Map<String, String> = emptyMap()): String? = withContext(Dispatchers.IO) {
    runCatching {
        val request = Request.Builder().url(url)
            .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .apply { headers.forEach { (key, value) -> header(key, value) } }.build()
        httpClient.newCall(request).execute().use { response -> response.body?.string()?.takeIf { response.isSuccessful } }
    }.getOrNull()
}

internal fun encoded(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

