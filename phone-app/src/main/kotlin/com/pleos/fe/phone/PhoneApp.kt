package com.pleos.fe.phone

import android.app.Application
import android.content.Context
import com.pleos.fe.api.PhoneApi
import com.pleos.fe.api.PleosApi
import com.pleos.fe.api.UserSession
import com.pleos.fe.api.assetUrl
import com.pleos.fe.core.android.SecureTokenStore

class PhoneApp : Application() {

    lateinit var container: PhoneContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = PhoneContainer(this)
    }
}

/**
 * 폰 앱의 서버 연결 묶음. 화면(ViewModel)은 여기서 api·session을 받아 쓴다.
 * 흐름: session.isSignedIn이 false면 가입 화면 → signUp(닉네임) → 이후 api 호출에 토큰 자동 첨부.
 */
class PhoneContainer(context: Context) {
    val baseUrl: String = BuildConfig.API_BASE_URL
    private val tokens = SecureTokenStore(context, "phone_session")
    val api: PhoneApi = PleosApi.phone(baseUrl, tokens)
    val session = UserSession(api, tokens)

    /** ThemeSkinDto의 /files/... 경로를 이미지 로더용 전체 주소로. */
    fun imageUrl(path: String) = assetUrl(baseUrl, path)
}

val Context.phoneContainer: PhoneContainer get() = (applicationContext as PhoneApp).container
