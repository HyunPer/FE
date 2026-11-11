package com.pleos.fe.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pleos.fe.phone.shwimpyo.DemoPrefill
import com.pleos.fe.phone.shwimpyo.ShwimpyoApp
import com.pleos.fe.phone.shwimpyo.ShwimpyoViewModel
import java.net.URLDecoder

/** 쉼표 폰 앱 진입 화면. ViewModel은 PhoneContainer에서 api·session을 받는다. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val demo = if (BuildConfig.DEBUG) demoPrefill() else DemoPrefill()
        setContent { ShwimpyoApp(viewModel { ShwimpyoViewModel(phoneContainer) }, demo) }
    }

    /** 시연 녹화용: `am start ... --es demo_nickname %EC%97%84%EB%A7%88`처럼 URL 인코딩한 글자를 받는다. */
    private fun demoPrefill() = DemoPrefill(
        nickname = intent.getStringExtra("demo_nickname").decoded(),
        note = intent.getStringExtra("demo_note").decoded(),
    )

    private fun String?.decoded(): String = this?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }.orEmpty()
}
