package com.pleos.fe.api.care

import com.pleos.fe.api.model.MotionSensitivity

/**
 * 멀미 위험도 계산에 넣는 신호. 모두 차량 상태 조회(Vehicle SDK)와 내비 정보(NaviHelper)에서 온다.
 * - 앞으로의 길: NaviHelper TBT(급커브·U턴·회전교차로), 주의 구간(과속방지턱·내리막·좁은 길)
 * - 지금의 흔들림: 브레이크 밟는 빈도, 핸들 각도 변화량
 */
data class MotionSignals(
    /** 2km 안에 남은 급커브·U턴·회전교차로 수. */
    val sharpTurnsAhead: Int = 0,
    /** 2km 안에 남은 과속방지턱·내리막·좁은 길 수. */
    val bumpsAhead: Int = 0,
    /** 최근 3분 동안 1분에 브레이크를 밟은 횟수. 가다 서다의 강도. */
    val brakesPerMinute: Double = 0.0,
    /** 최근 1분 동안 핸들 각도 변화량의 합(도). 구불구불한 정도. */
    val steeringSwingDeg: Double = 0.0,
    val drivingMinutes: Int = 0,
    /** 함께 탄 사람 중 가장 멀미를 잘 타는 사람 기준. */
    val sensitivity: MotionSensitivity = MotionSensitivity.MEDIUM,
)

enum class RiskLevel { LOW, CAUTION, HIGH }

/** 위험도를 올린 항목 하나. [points]는 민감도 배율을 곱하기 전 점수, [max]는 그 항목의 상한. */
data class RiskFactor(val label: String, val points: Int, val max: Int)

data class MotionRisk(
    val score: Int,
    val level: RiskLevel,
    val reasons: List<String>,
    /** [reasons]와 같은 순서. 화면에서 항목별 막대로 보여 준다. */
    val factors: List<RiskFactor> = emptyList(),
)

/**
 * 멀미 위험도(0–100). 의학적 진단이 아니라 "미리 대비하자"는 신호다.
 * 항목별 상한을 둬서 한 가지 신호만으로 위험이 튀지 않게 하고, 민감도로 마지막에 배율을 준다.
 */
object MotionRiskModel {

    fun evaluate(s: MotionSignals): MotionRisk {
        val factors = mutableListOf<RiskFactor>()
        var raw = 0.0

        val turns = s.sharpTurnsAhead.coerceIn(0, 5)
        if (turns > 0) {
            raw += turns * 8.0 // 최대 40
            factors += RiskFactor("2km 안 급커브 ${s.sharpTurnsAhead}곳", turns * 8, 40)
        }
        val bumps = s.bumpsAhead.coerceIn(0, 5)
        if (bumps > 0) {
            raw += bumps * 4.0 // 최대 20
            factors += RiskFactor("턱·내리막 ${s.bumpsAhead}곳", bumps * 4, 20)
        }
        val stopAndGo = ((s.brakesPerMinute - STOP_AND_GO_BASE) * 6.0).coerceIn(0.0, 20.0)
        if (stopAndGo > 0) {
            raw += stopAndGo
            factors += RiskFactor("가다 서다가 잦아요", stopAndGo.toInt(), 20)
        }
        val swing = (s.steeringSwingDeg / SWING_FULL_DEG * 20.0).coerceIn(0.0, 20.0)
        if (swing >= 8.0) factors += RiskFactor("구불구불한 길이에요", swing.toInt(), 20)
        raw += swing
        val long = ((s.drivingMinutes - LONG_TRIP_MIN) / 3.0).coerceIn(0.0, 10.0)
        if (long > 0) {
            raw += long
            factors += RiskFactor("${s.drivingMinutes}분째 이동 중", long.toInt(), 10)
        }

        val score = (raw * s.sensitivity.factor).toInt().coerceIn(0, 100)
        val level = when {
            score >= HIGH_FROM -> RiskLevel.HIGH
            score >= CAUTION_FROM -> RiskLevel.CAUTION
            else -> RiskLevel.LOW
        }
        return MotionRisk(score, level, factors.map { it.label }, factors)
    }

    private val MotionSensitivity.factor: Double
        get() = when (this) {
            MotionSensitivity.LOW -> 0.7
            MotionSensitivity.MEDIUM -> 1.0
            MotionSensitivity.HIGH -> 1.3
        }

    private const val STOP_AND_GO_BASE = 2.0
    private const val SWING_FULL_DEG = 400.0
    private const val LONG_TRIP_MIN = 30
    const val CAUTION_FROM = 35
    const val HIGH_FROM = 65
}

/** 최근 [windowMs] 동안의 값만 더한다(브레이크 횟수, 핸들 각도 변화량). 시각은 밀리초. */
class RollingWindow(private val windowMs: Long) {
    private val events = ArrayDeque<Pair<Long, Double>>()

    fun add(at: Long, value: Double = 1.0) {
        events.addLast(at to value)
    }

    fun sum(now: Long): Double {
        while (events.isNotEmpty() && events.first().first < now - windowMs) events.removeFirst()
        return events.sumOf { it.second }
    }

    fun perMinute(now: Long): Double = sum(now) * 60_000.0 / windowMs

    fun clear() = events.clear()
}