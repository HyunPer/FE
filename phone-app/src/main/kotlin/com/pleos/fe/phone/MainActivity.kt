package com.pleos.fe.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pleos.fe.phone.shwimpyo.ShwimpyoApp
import com.pleos.fe.phone.shwimpyo.ShwimpyoViewModel

/** 쉼표 폰 앱 진입 화면. ViewModel은 PhoneContainer에서 api·session을 받는다. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShwimpyoApp(viewModel { ShwimpyoViewModel(phoneContainer) }) }
    }
}
