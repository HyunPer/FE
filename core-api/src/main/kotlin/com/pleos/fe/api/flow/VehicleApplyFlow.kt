package com.pleos.fe.api.flow

import com.pleos.fe.api.ApiResult
import com.pleos.fe.api.VehicleApi
import com.pleos.fe.api.apiCall
import com.pleos.fe.api.bodyOrNull
import com.pleos.fe.api.model.ApplicableProfileDto
import com.pleos.fe.api.model.DriverProfilesResponse
import com.pleos.fe.api.model.VehicleSettingsDto
import com.pleos.fe.api.model.WelcomeResponse
import kotlinx.coroutines.CancellationException

/**
 * 1단계: 차량 설정을 Pleos Vehicle SDK로 적용한다. 실패하면 예외를 던진다.
 * - seatPosition.memorySlot이 있으면 selectSeatMemory, 없고 위치값이 있으면 moveSeat* + 위치 구독 후 STOP
 * - seat.heatingLevel/ventilationLevel → setHvacSeatTemperature/Ventilation, steeringWheelHeating → setHvacSteeringWheelHeat
 * - climate → setHvacTemperature, setHvacFanSpeed
 */
fun interface VehicleControl {
    suspend fun apply(settings: VehicleSettingsDto)
}

/**
 * 2단계: 차량 설정이 끝난 뒤의 연출. 화면 스킨 교체 → 앰비언트 라이트(Vehicle SDK Light) → 사운드·TTS 인사 순.
 */
fun interface WelcomeRenderer {
    suspend fun render(welcome: WelcomeResponse)
}

sealed interface BoardingOutcome {
    /** 차량 설정과 웰컴까지 모두 적용됨. */
    data class Welcomed(val welcome: WelcomeResponse) : BoardingOutcome

    /** 예약이 없다. 1탭 선택 화면에 drivers를 보여주고, 고르면 [VehicleApplyFlow.applySelected]. */
    data class ChooseDriver(val drivers: List<DriverProfilesResponse>) : BoardingOutcome

    /** 어느 단계에서 멈췄는지. REPORT에서 실패하면 차량 설정은 이미 적용된 상태라 재시도만 하면 된다(서버가 같은 응답). */
    data class Failed(val step: Step, val cause: Throwable) : BoardingOutcome

    enum class Step { FETCH, VEHICLE_SETTINGS, REPORT, WELCOME }
}

/**
 * 탑승 시 2단계 적용. 서버 API가 순서를 강제한다: 차량 설정 보고를 해야 웰컴 묶음을 받는다.
 * 트리거(앱 열림·착석/도어 이벤트·음성 버튼)는 차량 앱이 정하고, 여기서는 [onBoarding]만 부른다.
 */
class VehicleApplyFlow(
    private val api: VehicleApi,
    private val control: VehicleControl,
    private val renderer: WelcomeRenderer,
) {

    suspend fun onBoarding(): BoardingOutcome {
        val next = when (val result = apiCall { api.nextDriver().bodyOrNull() }) {
            is ApiResult.Failure -> return BoardingOutcome.Failed(BoardingOutcome.Step.FETCH, result.error)
            is ApiResult.Success -> result.value
        }
        if (next == null) {
            return when (val drivers = apiCall { api.driverProfiles() }) {
                is ApiResult.Success -> BoardingOutcome.ChooseDriver(drivers.value)
                is ApiResult.Failure -> BoardingOutcome.Failed(BoardingOutcome.Step.FETCH, drivers.error)
            }
        }
        return applyThenWelcome(next.vehicleSettings) { api.reportApplied(next.reservationId) }
    }

    /** 1탭 선택 화면에서 고른 프로필. */
    suspend fun applySelected(profile: ApplicableProfileDto): BoardingOutcome =
        applyThenWelcome(profile.vehicleSettings) { api.reportSelectedApplied(profile.profileId) }

    private suspend fun applyThenWelcome(
        settings: VehicleSettingsDto,
        report: suspend () -> WelcomeResponse,
    ): BoardingOutcome {
        step(BoardingOutcome.Step.VEHICLE_SETTINGS) { control.apply(settings) }?.let { return it }

        val welcome = when (val result = apiCall { report() }) {
            is ApiResult.Failure -> return BoardingOutcome.Failed(BoardingOutcome.Step.REPORT, result.error)
            is ApiResult.Success -> result.value
        }

        step(BoardingOutcome.Step.WELCOME) { renderer.render(welcome) }?.let { return it }
        return BoardingOutcome.Welcomed(welcome)
    }

    /** SDK·화면 쪽 실패를 Failed로 바꾼다. 성공하면 null. */
    private suspend fun step(step: BoardingOutcome.Step, block: suspend () -> Unit): BoardingOutcome.Failed? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        BoardingOutcome.Failed(step, e)
    }
}
