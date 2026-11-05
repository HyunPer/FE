package com.pleos.fe.api.care

import com.pleos.fe.api.model.Mood
import com.pleos.fe.api.model.TripResponse
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** 이번 주(월요일 0시부터) 이동 요약과, 눈에 띄는 패턴 한 줄. */
data class WeeklyInsight(
    val tripCount: Int,
    val averageMinutes: Int,
    val congestionMinutes: Int,
    /** 피곤·답답·속이 안 좋았다고 남긴 이동 수. */
    val hardTrips: Int,
    /** 눈에 띄는 패턴이 없으면 null. 해석은 하지 않고 사실만 말한다. */
    val pattern: String?,
)

/**
 * 폰 기록 탭의 "이번 주" 요약. 진단이 아니라 본인이 남긴 기록을 모아 보여주는 것이다.
 * 패턴은 우선순위대로 하나만: 힘들었던 시간대 → 가장 막혔던 요일 → 멀미 위험이 높았던 이동.
 */
object WeeklyInsights {

    fun of(trips: List<TripResponse>, now: Instant, zone: ZoneId): WeeklyInsight {
        val weekStart = ZonedDateTime.ofInstant(now, zone)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toLocalDate().atStartOfDay(zone)
        val week = trips.mapNotNull { trip ->
            if (trip.endedAt == null) return@mapNotNull null
            val start = runCatching { Instant.parse(trip.startedAt).atZone(zone) }.getOrNull() ?: return@mapNotNull null
            if (start.isBefore(weekStart) || start.toInstant().isAfter(now)) null else trip to start
        }
        val hard = week.filter { (trip, _) -> trip.myMood in HARD_MOODS }
        return WeeklyInsight(
            tripCount = week.size,
            averageMinutes = if (week.isEmpty()) 0 else week.sumOf { it.first.drivingMinutes } / week.size,
            congestionMinutes = week.sumOf { it.first.congestionMinutes },
            hardTrips = hard.size,
            pattern = hardTimePattern(hard.map { it.second })
                ?: congestedDayPattern(week)
                ?: riskyTripPattern(week.map { it.first }),
        )
    }

    private fun hardTimePattern(starts: List<ZonedDateTime>): String? {
        val (band, count) = starts.groupingBy { TimeBand.of(it.hour) }.eachCount().maxByOrNull { it.value } ?: return null
        return if (count >= 2) "${band.label} 이동 뒤에 피곤·답답했던 날이 많았어요(${count}번)" else null
    }

    private fun congestedDayPattern(week: List<Pair<TripResponse, ZonedDateTime>>): String? {
        val (day, minutes) = week.groupBy { it.second.dayOfWeek }
            .mapValues { (_, list) -> list.sumOf { it.first.congestionMinutes } }
            .maxByOrNull { it.value } ?: return null
        return if (minutes >= 10) "가장 많이 막힌 날은 ${day.korean}이에요(${minutes}분)" else null
    }

    private fun riskyTripPattern(trips: List<TripResponse>): String? {
        val risky = trips.count { it.peakRisk >= MotionRiskModel.HIGH_FROM }
        return if (risky >= 2) "멀미 위험이 높았던 이동이 ${risky}번 있었어요" else null
    }

    private val HARD_MOODS = setOf(Mood.TIRED, Mood.STRESSED, Mood.QUEASY)

    private enum class TimeBand(val label: String) {
        DAWN("새벽"), MORNING("아침"), DAY("낮"), EVENING("저녁"), NIGHT("밤");

        companion object {
            fun of(hour: Int) = when (hour) {
                in 0..5 -> DAWN
                in 6..9 -> MORNING
                in 10..16 -> DAY
                in 17..20 -> EVENING
                else -> NIGHT
            }
        }
    }

    private val DayOfWeek.korean: String
        get() = when (this) {
            DayOfWeek.MONDAY -> "월요일"
            DayOfWeek.TUESDAY -> "화요일"
            DayOfWeek.WEDNESDAY -> "수요일"
            DayOfWeek.THURSDAY -> "목요일"
            DayOfWeek.FRIDAY -> "금요일"
            DayOfWeek.SATURDAY -> "토요일"
            DayOfWeek.SUNDAY -> "일요일"
        }
}
