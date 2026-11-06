package com.pleos.fe.vehicle.care

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pleos.fe.api.care.RiskLevel
import com.pleos.fe.api.model.MotionSensitivity
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** 트립 모니터: 신호 → 위험도 → 도착 요약. SDK·내비 없이 시연용 신호와 가짜 시계로 확인한다. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class TripMonitorTest {

    private var now = 0L

    @Test
    fun `앞의 급커브와 구불길 신호로 위험도가 오르고, 요약에 최고치가 남는다`() = runTest {
        val monitor = TripMonitor(null, null, backgroundScope) { now }
        monitor.sensitivity = MotionSensitivity.HIGH
        monitor.start()
        assertEquals(RiskLevel.LOW, monitor.risk.value.level)

        now = 10 * 60_000L
        monitor.simulate(turnsAhead = 4, bumps = 2)
        assertEquals(RiskLevel.CAUTION, monitor.risk.value.level) // (32 + 8) × 1.3 = 52
        monitor.simulate(steeringDeg = 420.0)
        assertEquals(RiskLevel.HIGH, monitor.risk.value.level) // (32 + 8 + 20) × 1.3 = 78

        now = 44 * 60_000L
        monitor.resetSimulation()
        val summary = monitor.stop()
        assertEquals(44, summary.drivingMinutes)
        assertEquals(78, summary.peakRisk)
    }

    @Test
    fun `가다 서다가 이어진 동안만 막힘 시간으로 센다`() = runTest {
        val monitor = TripMonitor(null, null, backgroundScope) { now }
        monitor.start()

        now = 60_000L
        monitor.simulate(brakePresses = 18) // 최근 3분 동안 1분에 6번
        now = 240_000L
        monitor.simulate() // 아직 3분 안: 계속 막힘
        now = 300_000L
        monitor.simulate() // 3분이 지나 브레이크 기록이 빠짐: 막힘 아님

        val summary = monitor.stop()
        assertEquals(4, summary.congestionMinutes)
        assertEquals(5, summary.drivingMinutes)
    }

    @Test
    fun `휴게소 거리는 알려 주고, 초기화하면 지운다`() = runTest {
        val monitor = TripMonitor(null, null, backgroundScope) { now }
        monitor.start()
        monitor.simulate(restAreaM = 12_000.0)
        assertEquals(12_000.0, monitor.restAreaAheadM.value!!, 0.0)
        monitor.resetSimulation()
        assertNull(monitor.restAreaAheadM.value)
    }

    @Test
    fun `주행 중에는 회전 안내를 10초마다 내비에 묻는다`() = runTest {
        var requests = 0
        val monitor = TripMonitor(null, null, backgroundScope, requestTbt = { requests++ }) { now }
        monitor.start()
        runCurrent()
        assertEquals(1, requests) // 시작하자마자 한 번

        advanceTimeBy(20_001)
        assertEquals(3, requests) // 10초, 20초

        monitor.stop()
        advanceTimeBy(30_000)
        assertEquals(3, requests) // 끝나면 더 묻지 않는다
    }
}
