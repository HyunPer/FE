package com.pleos.fe.api

import com.pleos.fe.api.care.MotionRiskModel
import com.pleos.fe.api.care.MotionSignals
import com.pleos.fe.api.model.CheckInRequest
import com.pleos.fe.api.model.Mood
import com.pleos.fe.api.model.MotionSensitivity
import com.pleos.fe.api.model.PairRequest
import com.pleos.fe.api.model.RestStyle
import com.pleos.fe.api.model.RiderConditionDto
import com.pleos.fe.api.model.TripSummaryRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 실제 백엔드를 상대로 쉼표 시연 흐름을 끝까지 돌린다. 서버를 띄우고 주소를 넘길 때만 실행된다.
 * PLEOS_E2E_BASE_URL=http://localhost:8090/ ./gradlew :core-api:test --tests '*EndToEnd*'
 */
class EndToEndTest {

    private val baseUrl = System.getenv("PLEOS_E2E_BASE_URL").orEmpty()

    @Test
    fun `가입·차량 연결부터 트립 종료와 도착 체크인까지`() = runTest {
        assumeTrue(baseUrl.isNotBlank(), "PLEOS_E2E_BASE_URL이 없어 건너뜀")

        val phoneTokens = InMemoryTokenStore()
        val vehicleTokens = InMemoryTokenStore()
        val phone = PleosApi.phone(baseUrl, phoneTokens)
        val vehicleApi = PleosApi.vehicle(baseUrl, vehicleTokens)

        // 차량 앱 첫 실행 → 폰에서 연결 코드로 페어링
        val car = VehicleSession(vehicleApi, vehicleTokens).ensureRegistered("CASPER Electric").getOrThrow()
        UserSession(phone, phoneTokens).signUp("대학생 자녀").getOrThrow()
        val vehicle = phone.pair(PairRequest(car.pairingCode, "우리집 캐스퍼"))

        // 폰: 멀미를 잘 탄다고 저장 → 차량 앱이 가족 컨디션으로 받는다
        phone.updateMyCondition(RiderConditionDto(MotionSensitivity.HIGH, RestStyle.FRESH_AIR))
        val sensitivity = vehicleApi.riders().maxOf { it.condition.motionSensitivity }
        assertEquals(MotionSensitivity.HIGH, sensitivity)

        // 차량 앱: 탑승 → 산길에서 위험도 계산 → 도착 때 요약 보고
        val trip = vehicleApi.startTrip()
        val peak = MotionRiskModel.evaluate(MotionSignals(sharpTurnsAhead = 3, steeringSwingDeg = 300.0, sensitivity = sensitivity)).score
        vehicleApi.finishTrip(trip.id, TripSummaryRequest(drivingMinutes = 44, congestionMinutes = 18, sharpTurns = 6, peakRisk = peak))

        // 폰: 마지막 이동을 보고 도착 체크인
        val last = phone.trips(vehicle.id).first()
        assertEquals(44, last.drivingMinutes)
        assertEquals(peak, last.peakRisk)
        phone.checkIn(last.id, CheckInRequest(Mood.QUEASY, "뒷자리에서 속이 안 좋았음"))
        assertEquals(Mood.QUEASY, phone.trips(vehicle.id).first().myMood)
    }
}
