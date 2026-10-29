package com.pleos.fe.vehicle

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.pleos.fe.api.ApiResult
import com.pleos.fe.api.flow.BoardingOutcome
import com.pleos.fe.vehicle.sdk.BoardingTrigger
import com.pleos.fe.vehicle.sdk.PleosVehicleControl
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 화면 디자인이 나오기 전 에뮬레이터 QA용 임시 화면.
 * 차량 등록·페어링 코드, 탑승 2단계 적용(버튼 / 착석+P단 자동), 항목별 SDK 적용 결과를 로그로 보여준다.
 * Virtual Console(http://127.0.0.1:8088, adb forward tcp:8088 tcp:8088)에서 착석·기어·공조 값을 바꿔 확인한다.
 */
class VehicleQaActivity : Activity() {

    private val scope = MainScope()
    private val container by lazy { vehicleContainer }
    private lateinit var status: TextView
    private lateinit var log: TextView
    private var trigger: BoardingTrigger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { textSize = 22f; setTypeface(typeface, Typeface.BOLD) }
        log = TextView(this).apply { textSize = 16f; setTextIsSelectable(true) }

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(button("차량 등록 / 코드 보기") { register() })
            addView(button("탑승 (2단계 적용)") { board() })
            addView(button("착석+P단 자동 감지 켜기") { enableTrigger() })
            addView(button("로그 지우기") { log.text = "" })
        }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 40, 48, 40)
                addView(status)
                addView(buttons)
                addView(ScrollView(this@VehicleQaActivity).apply { addView(log) }, LinearLayout.LayoutParams(-1, 0, 1f))
            },
        )
        status.text = if (container.sdk != null) "Vehicle SDK 연결됨 · 서버 ${container.baseUrl}" else "Vehicle SDK 없음(로그만) · 서버 ${container.baseUrl}"
        register()
    }

    override fun onDestroy() {
        trigger?.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        minHeight = 96
        setOnClickListener { onClick() }
    }

    private fun register() = scope.launch {
        when (val r = container.session.ensureRegistered(model = "Pleos Connect")) {
            is ApiResult.Success -> line("페어링 코드 ${r.value.pairingCode} · 멤버 ${r.value.memberCount}명 · ${r.value.nickname ?: "이름 없음"}")
            is ApiResult.Failure -> line("차량 등록 실패: ${r.error.message}")
        }
    }

    private fun enableTrigger() {
        if (trigger != null) return line("자동 감지는 이미 켜져 있습니다")
        trigger = container.boardingTrigger(scope) { board().join() }?.also { it.start() }
        line(if (trigger != null) "자동 감지 켬: 운전석 착석 + P단이면 적용을 시작합니다" else "Vehicle SDK가 없어 자동 감지를 쓸 수 없습니다")
    }

    private fun board() = scope.launch {
        line("탑승 처리 시작")
        when (val o = container.applyFlow.onBoarding()) {
            is BoardingOutcome.Welcomed -> {
                reportVehicleItems()
                container.welcomeRenderer.ambientResult.value?.let { line("${mark(it.ok)} ${it.name}: ${it.detail}") }
                line("웰컴: 테마 \"${o.welcome.skin.name}\" · 위젯 ${o.welcome.widgets.joinToString()}")
                line("TTS 인사: \"${o.welcome.greeting}\"")
            }
            is BoardingOutcome.ChooseDriver -> {
                line("예약 없음 → 1탭 선택 화면 대상: " + o.drivers.joinToString { d -> "${d.nickname}(${d.profiles.joinToString { it.name }})" })
                o.drivers.flatMap { it.profiles }.firstOrNull()?.let { first ->
                    line("QA: 첫 프로필 \"${first.name}\"로 적용해 봅니다")
                    when (val s = container.applyFlow.applySelected(first)) {
                        is BoardingOutcome.Welcomed -> { reportVehicleItems(); line("웰컴: \"${s.welcome.greeting}\"") }
                        is BoardingOutcome.Failed -> failed(s)
                        else -> Unit
                    }
                }
            }
            is BoardingOutcome.Failed -> { reportVehicleItems(); failed(o) }
        }
    }

    private fun reportVehicleItems() {
        (container.vehicleControl as? PleosVehicleControl)?.report?.value?.forEach { line("${mark(it.ok)} ${it.name}: ${it.detail}") }
    }

    private fun failed(o: BoardingOutcome.Failed) = line("✗ ${o.step} 단계에서 멈춤: ${o.cause.message}")

    private fun mark(ok: Boolean) = if (ok) "✓" else "✗"

    private fun line(text: String) {
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        log.append("[$time] $text\n")
        (log.parent as? View)?.post { (log.parent as? ScrollView)?.fullScroll(View.FOCUS_DOWN) }
    }
}
