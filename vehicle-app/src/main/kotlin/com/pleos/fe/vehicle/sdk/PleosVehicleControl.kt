package com.pleos.fe.vehicle.sdk

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.Seat
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.control.MoveControl
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import ai.pleos.playground.vehicle.listener.SeatBackrestAnglePositionListener
import ai.pleos.playground.vehicle.listener.SeatForeAftPositionListener
import com.pleos.fe.api.flow.VehicleControl
import com.pleos.fe.api.model.SeatPositionDto
import com.pleos.fe.api.model.VehicleSettingsDto
import com.pleos.fe.api.vehicle.MoveDirection
import com.pleos.fe.api.vehicle.SeatAxis
import com.pleos.fe.api.vehicle.SeatAxisMover
import com.pleos.fe.api.vehicle.SeatMoveOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

/** 항목별 적용 결과. QA 화면과 로그에 그대로 보여준다. */
data class ApplyItem(val name: String, val ok: Boolean, val detail: String)

/**
 * 1단계 차량 설정을 Pleos Vehicle SDK로 운전석에 적용한다.
 * 항목마다 따로 적용하고 결과를 모은다. 차종마다 지원 기능이 달라 일부 실패는 정상이며,
 * 하나도 적용하지 못했을 때만 예외를 던져 2단계(적용 완료 보고)로 넘어가지 않게 한다.
 */
class PleosVehicleControl(
    private val vehicle: Vehicle,
    private val seatMover: SeatAxisMover = SeatAxisMover(),
) : VehicleControl {

    private val lastReport = MutableStateFlow<List<ApplyItem>>(emptyList())
    val report: StateFlow<List<ApplyItem>> = lastReport.asStateFlow()

    override suspend fun apply(settings: VehicleSettingsDto) {
        val items = buildList {
            add(item("공조 온도") { applyTemperature(settings.climate.temperature) })
            add(item("풍량") { applyFanSpeed(settings.climate.fanSpeed) })
            add(item("시트 열선") { applySeatHeating(settings.seat.heatingLevel) })
            add(item("시트 통풍") { applySeatVentilation(settings.seat.ventilationLevel) })
            add(item("핸들 열선") { applySteeringHeat(settings.seat.steeringWheelHeating) })
            add(item("시트 자세") { applySeatPosition(settings.seatPosition) })
        }
        lastReport.value = items
        if (items.none { it.ok }) {
            throw IllegalStateException("차량 설정을 하나도 적용하지 못했습니다: " + items.joinToString { "${it.name}(${it.detail})" })
        }
    }

    private suspend fun applyTemperature(celsius: Double): String {
        val hvac = vehicle.getHVAC()
        val bounds = awaitValue<Map<SeatArea, AreaBounds<Float>>>("온도 범위") { ok, fail ->
            hvac.checkHvacTemperatureCapabilities({ _: CapabilityConfig, map: Map<SeatArea, AreaBounds<Float>> -> ok(map) }, fail)
        }.let { it[DRIVER] ?: it.values.firstOrNull() }
        val value = celsius.toFloat().clampTo(bounds?.minValue, bounds?.maxValue)
        val result = awaitSet { ok, fail -> hvac.setHvacTemperature(DRIVER, value, TemperatureUnit.CELSIUS, { _, _ -> ok() }, fail) }
        return describe(result, "$value°C" + if (value.toDouble() != celsius) " (차량 범위로 조정)" else "")
    }

    private suspend fun applyFanSpeed(speed: Int): String {
        val hvac = vehicle.getHVAC()
        val bounds = awaitValue<Map<SeatArea, AreaBounds<Int>>>("풍량 범위") { ok, fail ->
            hvac.checkHvacFanSpeedCapabilities({ _: CapabilityConfig, map: Map<SeatArea, AreaBounds<Int>> -> ok(map) }, fail)
        }.let { it[DRIVER] ?: it.values.firstOrNull() }
        val value = speed.clampTo(bounds?.minValue, bounds?.maxValue)
        return describe(awaitSet { ok, fail -> hvac.setHvacFanSpeed(DRIVER, value, { _, _ -> ok() }, fail) }, "$value")
    }

    private suspend fun applySeatHeating(level: Int): String {
        val seat = vehicle.getSeat()
        return describe(awaitSet { ok, fail -> seat.setHvacSeatTemperature(DRIVER, level, { _, _ -> ok() }, fail) }, "${level}단계")
    }

    private suspend fun applySeatVentilation(level: Int): String {
        val seat = vehicle.getSeat()
        return describe(awaitSet { ok, fail -> seat.setHvacSeatVentilation(DRIVER, level, { _, _ -> ok() }, fail) }, "${level}단계")
    }

    /** 프로필은 켬/끔만 있다. 켜면 차량이 지원하는 최저 단계(1 이상)로 켠다. */
    private suspend fun applySteeringHeat(on: Boolean): String {
        val wheel = vehicle.getSteeringWheel()
        val level = if (!on) 0 else {
            val bounds = awaitValue<AreaBounds<Int>>("핸들 열선 범위") { ok, fail ->
                wheel.checkHvacSteeringWheelHeatCapabilities({ _: CapabilityConfig, b: AreaBounds<Int> -> ok(b) }, fail)
            }
            maxOf(1, bounds.minValue)
        }
        return describe(awaitSet { ok, fail -> wheel.setHvacSteeringWheelHeat(level, { ok() }, fail) }, if (on) "켬($level)" else "끔")
    }

    private suspend fun applySeatPosition(position: SeatPositionDto?): String {
        if (position == null) return "이 차량에 저장된 자세 없음 → 그대로 둠"
        val seat = vehicle.getSeat()
        position.memorySlot?.let { slot ->
            // selectSeatMemory는 실패 콜백만 있다. 시간 안에 실패가 없으면 요청이 들어간 것으로 본다.
            val result = awaitSet { _, fail -> seat.selectSeatMemory(DRIVER, slot) { e -> fail(e) } }
            return describe(result, "시트 메모리 ${slot}번 호출")
        }
        val parts = mutableListOf<String>()
        position.backrestPosition?.let { parts += "등받이 " + outcomeText(seatMover.moveTo(backrestAxis(seat), it)) }
        position.foreAftPosition?.let { parts += "앞뒤 " + outcomeText(seatMover.moveTo(foreAftAxis(seat), it)) }
        return parts.joinToString(" · ")
    }

    private fun outcomeText(o: SeatMoveOutcome) = when (o) {
        SeatMoveOutcome.AlreadyThere -> "이미 위치"
        is SeatMoveOutcome.Reached -> "도착(${o.position})"
        is SeatMoveOutcome.TimedOut -> throw IllegalStateException("시간 초과로 멈춤(${o.lastPosition})")
        SeatMoveOutcome.Unknown -> throw IllegalStateException("현재 위치를 읽을 수 없음")
    }

    private fun foreAftAxis(seat: Seat) = object : SeatAxis {
        override suspend fun current(): Int? = awaitValue("앞뒤 위치") { ok, fail -> seat.getCurrentSeatForeAftPosition(DRIVER, ok, fail) }
        override fun move(direction: MoveDirection) = seat.moveSeatForeAft(DRIVER, direction.toSdk(), { _, _ -> }, {})
        override fun stop() = seat.moveSeatForeAft(DRIVER, MoveControl.STOP, { _, _ -> }, {})
        override val positions = callbackFlow {
            val listener = object : SeatForeAftPositionListener {
                override fun onSeatForeAftPositionUpdated(position: Int?) { position?.let { trySend(it) } }
                override fun onFailed(e: Exception) { close(e) }
            }
            seat.registerSeatForeAftPosition(DRIVER, listener)
            awaitClose { seat.unregisterSeatForeAftPosition(DRIVER, listener) }
        }
    }

    private fun backrestAxis(seat: Seat) = object : SeatAxis {
        override suspend fun current(): Int? = awaitValue("등받이 위치") { ok, fail -> seat.getCurrentSeatBackrestAnglePosition(DRIVER, ok, fail) }
        override fun move(direction: MoveDirection) = seat.moveSeatBackrestAngle(DRIVER, direction.toSdk(), { _, _ -> }, {})
        override fun stop() = seat.moveSeatBackrestAngle(DRIVER, MoveControl.STOP, { _, _ -> }, {})
        override val positions = callbackFlow {
            val listener = object : SeatBackrestAnglePositionListener {
                override fun onSeatBackrestAnglePositionUpdated(position: Int?) { position?.let { trySend(it) } }
                override fun onFailed(e: Exception) { close(e) }
            }
            seat.registerSeatBackrestAnglePosition(DRIVER, listener)
            awaitClose { seat.unregisterSeatBackrestAnglePosition(DRIVER, listener) }
        }
    }

    private fun MoveDirection.toSdk() = if (this == MoveDirection.FORWARD) MoveControl.FORWARD else MoveControl.BACKWARD

    private fun describe(result: SetResult, value: String) =
        if (result == SetResult.CONFIRMED) value else "$value (요청 보냄)"

    private suspend fun item(name: String, block: suspend () -> String): ApplyItem = try {
        ApplyItem(name, true, block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ApplyItem(name, false, e.message ?: e.javaClass.simpleName)
    }
}
