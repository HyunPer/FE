package com.pleos.fe.vehicle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleos.fe.api.model.VehicleSelfResponse
import com.pleos.fe.vehicle.BuildConfig

private val Background = Color(0xFF0C1016)
private val Surface = Color(0xFF1A2029)
private val TextColor = Color(0xFFF2F5F9)
private val SubText = Color(0xFF8D97A6)
private val Accent = Color(0xFF5AA2FF)

/** 차량 앱 화면 진입점. 쉼표 화면은 VehicleScreen에 상태를 더하고 여기서 분기한다. */
@Composable
fun VehicleRoot(controller: VehicleController) {
    val screen by controller.screen.collectAsState()
    Box(Modifier.fillMaxSize().background(Background)) {
        when (val s = screen) {
            VehicleScreen.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center).size(72.dp), color = Accent)
            is VehicleScreen.Waiting -> WaitingScreen(s.self, controller::startTrip)
            is VehicleScreen.Driving -> CareScreen(
                risk = s.risk,
                onArrive = controller::finishTrip,
                restStyle = s.restStyle,
                restAreaAheadM = s.restAreaAheadM,
                parked = s.parked,
                // 시연 패널은 디버그 빌드에서만(제출 릴리스 APK에는 보이지 않는다)
                onDemo = if (BuildConfig.DEBUG) controller::demo else null,
            )
            is VehicleScreen.Arrived -> ArrivalScreen(s.trip, controller::done)
            is VehicleScreen.Failed -> FailedScreen(s.message, s.retry)
        }
    }
}

/** 멤버가 없으면 연결 코드를 크게, 있으면 대기 상태와 작은 코드. 연결 코드는 정차 중 화면이다. */
@Composable
fun WaitingScreen(self: VehicleSelfResponse, onStartTrip: () -> Unit) {
    val paired = self.memberCount > 0
    Row(Modifier.fillMaxSize().padding(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                if (paired) "쉼표가 함께 탈 준비가 됐어요" else "이 차를 가족과 연결하세요",
                color = TextColor,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                if (paired) "가족 ${self.memberCount}명 연결됨" else "폰 앱 → 차량 연결에서 오른쪽 코드를 입력하세요",
                color = SubText,
                fontSize = 24.sp,
            )
            if (paired) {
                // 탑승은 착석+P단으로 자동 감지한다. 에뮬레이터 시연용으로 버튼도 둔다.
                Spacer(Modifier.height(36.dp))
                Button({ onStartTrip() }, Modifier.height(76.dp)) { Text("지금 출발", fontSize = 24.sp) }
            }
        }
        Spacer(Modifier.width(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(if (paired) 8.dp else 14.dp)) {
            self.pairingCode.forEach { c ->
                Box(
                    Modifier
                        .size(if (paired) 52.dp else 88.dp, if (paired) 64.dp else 110.dp)
                        .background(Surface, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(c.toString(), color = Accent, fontSize = if (paired) 30.sp else 52.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FailedScreen(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = TextColor, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(36.dp))
        Button(onRetry, Modifier.height(76.dp)) { Text("다시 시도", fontSize = 24.sp) }
    }
}
