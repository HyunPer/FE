package com.pleos.fe.phone.check

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 화면 디자인 전 임시: 폰 앱 ↔ BE 연동 확인 화면.
 * 가입 → 페어링 → 프로필 → 시트 슬롯 → 예약 → (차량 앱에서 탑승) → 탑승 기록까지 실제 서버로 눌러 본다.
 */
@Composable
fun ConnectionCheckScreen(viewModel: ConnectionCheckViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("서버 연동 확인", style = MaterialTheme.typography.headlineSmall)
            Text("서버 ${viewModel.baseUrl}", style = MaterialTheme.typography.bodySmall)
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        item { AccountSection(state, viewModel) }
        if (state.me != null) {
            item { VehicleSection(state, viewModel) }
            item { ProfileSection(state, viewModel) }
            item { ReservationSection(state, viewModel) }
        }
        item {
            Section("로그") {
                if (state.log.isEmpty()) Text("버튼을 누르면 결과가 여기에 쌓입니다", style = MaterialTheme.typography.bodySmall)
                state.log.forEach { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun AccountSection(state: ConnectionCheckState, vm: ConnectionCheckViewModel) {
    var nickname by rememberSaveable { mutableStateOf("") }
    Section("1. 계정") {
        val me = state.me
        if (me == null) {
            OutlinedTextField(nickname, { nickname = it }, label = { Text("닉네임 (예: 엄마)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ vm.signUp(nickname.trim()) }, enabled = nickname.isNotBlank()) { Text("가입") }
                if (vm.isSignedIn) OutlinedButton(vm::refreshAll) { Text("저장된 계정 다시 불러오기") }
            }
        } else {
            Text("${me.nickname} 님으로 로그인됨")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(vm::refreshAll) { Text("새로고침") }
                OutlinedButton(vm::signOut) { Text("로그아웃") }
            }
        }
    }
}

@Composable
private fun VehicleSection(state: ConnectionCheckState, vm: ConnectionCheckViewModel) {
    var code by rememberSaveable { mutableStateOf("") }
    var carNickname by rememberSaveable { mutableStateOf("") }
    Section("2. 차량 연결") {
        Text("차량 앱 화면의 연결 코드를 입력하세요", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(code, { code = it }, label = { Text("연결 코드") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(carNickname, { carNickname = it }, label = { Text("차 별명 (첫 연결 때만, 선택)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button({ vm.pair(code, carNickname) }, enabled = code.isNotBlank()) { Text("페어링") }
        if (state.vehicles.isNotEmpty()) HorizontalDivider()
        state.vehicles.forEach { car ->
            Choice(car.id == state.selectedVehicleId, "${car.nickname ?: car.model} · ${car.myRole}") { vm.selectVehicle(car.id) }
        }
    }
}

@Composable
private fun ProfileSection(state: ConnectionCheckState, vm: ConnectionCheckViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var temperature by rememberSaveable { mutableStateOf("22") }
    var slot by rememberSaveable { mutableStateOf("1") }
    Section("3. 내 프로필") {
        state.profiles.forEach { p ->
            Choice(p.id == state.selectedProfileId, "${p.name} · ${p.climate.temperature}℃ · 풍량 ${p.climate.fanSpeed}") { vm.selectProfile(p.id) }
        }
        if (state.profiles.isEmpty()) Text("아직 프로필이 없습니다", style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it }, label = { Text("새 프로필 이름") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(
                temperature, { temperature = it }, label = { Text("온도") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.5f),
            )
        }
        val temp = temperature.toDoubleOrNull()
        Button({ vm.createProfile(name.trim(), temp ?: 22.0) }, enabled = name.isNotBlank() && temp != null) { Text("프로필 만들기") }
        Text(
            if (state.themes.isEmpty()) "테마가 없어 기본 화면으로 만들어집니다" else "테마 \"${state.themes.first().skin.name}\"가 붙습니다",
            style = MaterialTheme.typography.bodySmall,
        )
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                slot, { slot = it }, label = { Text("시트 슬롯") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
            )
            OutlinedButton({ slot.toIntOrNull()?.let(vm::saveSeatSlot) }, enabled = slot.toIntOrNull() != null) { Text("선택 차량에 저장") }
        }
    }
}

@Composable
private fun ReservationSection(state: ConnectionCheckState, vm: ConnectionCheckViewModel) {
    Section("4. 다음 운전자 예약 · 탑승 기록") {
        val reservation = state.reservation
        Text(
            if (reservation == null) "대기 중인 예약 없음"
            else "예약: ${reservation.driverNickname} · ${reservation.profileName} (${reservation.status})",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(vm::reserve, enabled = state.selectedVehicle != null && state.selectedProfile != null) { Text("선택 프로필로 예약") }
            OutlinedButton(vm::cancelReservation, enabled = reservation != null) { Text("취소") }
        }
        Text("예약 후 차량 앱에서 \"지금 적용\"을 누르고 아래를 새로고침하세요", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(vm::refreshVehicle) { Text("새로고침") }
        HorizontalDivider()
        if (state.history.isEmpty()) Text("탑승 기록 없음", style = MaterialTheme.typography.bodySmall)
        state.history.forEach { h ->
            Text("${h.appliedAt.take(16).replace('T', ' ')}  ${h.driverNickname} · ${h.profileName} · ${if (h.source.name == "RESERVATION") "예약" else "1탭"}")
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Choice(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick)
        Text(label)
    }
}
