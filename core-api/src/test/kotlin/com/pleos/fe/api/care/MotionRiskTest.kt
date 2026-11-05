package com.pleos.fe.api.care

import com.pleos.fe.api.model.MotionSensitivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MotionRiskTest {

    @Test
    fun `곧은 길을 막힘 없이 가면 낮음`() {
        val risk = MotionRiskModel.evaluate(MotionSignals(drivingMinutes = 15))
        assertEquals(RiskLevel.LOW, risk.level)
        assertTrue(risk.reasons.isEmpty())
    }

    @Test
    fun `급커브가 이어지는 산길은 높음, 이유를 함께 준다`() {
        val risk = MotionRiskModel.evaluate(
            MotionSignals(sharpTurnsAhead = 4, bumpsAhead = 2, steeringSwingDeg = 380.0, drivingMinutes = 50),
        )
        assertEquals(RiskLevel.HIGH, risk.level)
        assertTrue("2km 안 급커브 4곳" in risk.reasons)
        assertTrue("구불구불한 길이에요" in risk.reasons)
    }

    @Test
    fun `같은 길이라도 멀미를 잘 타는 사람이 타면 더 일찍 주의`() {
        val road = MotionSignals(sharpTurnsAhead = 2, brakesPerMinute = 4.0)
        assertEquals(RiskLevel.LOW, MotionRiskModel.evaluate(road.copy(sensitivity = MotionSensitivity.LOW)).level)
        assertEquals(RiskLevel.CAUTION, MotionRiskModel.evaluate(road.copy(sensitivity = MotionSensitivity.HIGH)).level)
    }

    @Test
    fun `한 가지 신호만으로는 100을 넘지 않는다`() {
        val risk = MotionRiskModel.evaluate(MotionSignals(sharpTurnsAhead = 50, sensitivity = MotionSensitivity.HIGH))
        assertEquals(52, risk.score) // 급커브 상한 40 × 1.3
    }

    @Test
    fun `최근 구간 밖의 값은 빠진다`() {
        val window = RollingWindow(windowMs = 60_000)
        window.add(at = 0)
        window.add(at = 30_000)
        window.add(at = 70_000)
        assertEquals(2.0, window.sum(now = 80_000))
        assertEquals(2.0, window.perMinute(now = 80_000))
    }
}
