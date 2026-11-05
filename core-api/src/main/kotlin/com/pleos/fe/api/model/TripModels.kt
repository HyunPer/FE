package com.pleos.fe.api.model

import kotlinx.serialization.Serializable

// ── 쉼표: 트립·체크인·탑승자 컨디션 (서버 trip 패키지와 같은 모양) ──

@Serializable
enum class Mood { CALM, OK, TIRED, STRESSED, QUEASY }

@Serializable
enum class MotionSensitivity { LOW, MEDIUM, HIGH }

@Serializable
enum class RestStyle { QUIET, BREATHING, MUSIC, FRESH_AIR }

/** 차량 앱이 도착 때 보내는 주행 요약. */
@Serializable
data class TripSummaryRequest(
    val drivingMinutes: Int,
    val congestionMinutes: Int,
    val sharpTurns: Int,
    val peakRisk: Int,
)

@Serializable
data class TripResponse(
    val id: String,
    val vehicleId: String,
    val startedAt: String,
    val endedAt: String? = null,
    val drivingMinutes: Int,
    val congestionMinutes: Int,
    val sharpTurns: Int,
    val peakRisk: Int,
    val myMood: Mood? = null,
    val myNote: String? = null,
)

@Serializable
data class CheckInRequest(val mood: Mood, val note: String? = null)

@Serializable
data class RiderConditionDto(
    val motionSensitivity: MotionSensitivity = MotionSensitivity.MEDIUM,
    val restStyle: RestStyle = RestStyle.QUIET,
)

@Serializable
data class RiderResponse(val userId: String, val nickname: String, val condition: RiderConditionDto)