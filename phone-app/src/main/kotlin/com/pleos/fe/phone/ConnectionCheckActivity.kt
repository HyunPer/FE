package com.pleos.fe.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pleos.fe.phone.check.ConnectionCheckScreen
import com.pleos.fe.phone.check.ConnectionCheckViewModel

/** 화면 디자인 전 임시 진입 화면. ViewModel은 PhoneContainer에서 api·session을 받는다(실제 화면도 같은 방식). */
class ConnectionCheckActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    ConnectionCheckScreen(viewModel { ConnectionCheckViewModel(phoneContainer) })
                }
            }
        }
    }
}
