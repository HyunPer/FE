package com.pleos.fe.api

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 토큰 보관소. 인터셉터가 요청마다 동기로 읽으므로 get()은 메모리 값을 바로 돌려줘야 한다.
 * 안드로이드에서는 core-android의 SecureTokenStore(Keystore 암호화 + DataStore)를 쓴다.
 */
interface TokenStore {
    fun get(): String?
    fun set(token: String?)
}

class InMemoryTokenStore(private var token: String? = null) : TokenStore {
    override fun get(): String? = token
    override fun set(token: String?) {
        this.token = token
    }
}

/** 서버 연결 진입점. 폰 앱은 [phone], 차량 앱은 [vehicle]만 쓴다. */
object PleosApi {

    /** 서버가 필드를 늘려도 앱이 깨지지 않게 모르는 키는 무시한다. PUT 전체 교체를 위해 기본값도 보낸다. */
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun phone(baseUrl: String, tokens: TokenStore, configure: OkHttpClient.Builder.() -> Unit = {}): PhoneApi =
        retrofit(baseUrl, tokens, configure).create(PhoneApi::class.java)

    fun vehicle(baseUrl: String, tokens: TokenStore, configure: OkHttpClient.Builder.() -> Unit = {}): VehicleApi =
        retrofit(baseUrl, tokens, configure).create(VehicleApi::class.java)

    private fun retrofit(baseUrl: String, tokens: TokenStore, configure: OkHttpClient.Builder.() -> Unit): Retrofit {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(BearerInterceptor(tokens))
            .apply(configure)
            .build()
        return Retrofit.Builder()
            .baseUrl(normalize(baseUrl))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private fun normalize(baseUrl: String) = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
}

/** 토큰이 있으면 Authorization: Bearer 헤더를 붙인다. 토큰은 URL에 넣지 않는다(Pleos 보안 가이드). */
private class BearerInterceptor(private val tokens: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = chain.proceed(
        tokens.get()?.let { chain.request().newBuilder().header("Authorization", "Bearer $it").build() }
            ?: chain.request(),
    )
}

/** 서버가 주는 이미지 경로(/files/{id})를 기본 주소와 합친다. 이미지 로더(Coil 등)에 그대로 넘긴다. */
fun assetUrl(baseUrl: String, path: String): String = baseUrl.trimEnd('/') + "/" + path.trimStart('/')

/** 테마 이미지 업로드용 multipart 조각. 형식 판정은 서버가 파일 내용으로 한다. */
fun imagePart(fileName: String, bytes: ByteArray, contentType: String = "image/png"): MultipartBody.Part =
    MultipartBody.Part.createFormData("file", fileName, bytes.toRequestBody(contentType.toMediaType()))
