package com.pleos.fe.api

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
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
            // 무료 호스팅 서버는 쉬다가 첫 요청에 깨어나는 데 1분쯤 걸려서 넉넉히 기다린다.
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(75, TimeUnit.SECONDS)
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
