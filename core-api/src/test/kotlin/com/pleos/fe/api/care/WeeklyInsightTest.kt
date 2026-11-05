package com.pleos.fe.api.care

import com.pleos.fe.api.model.Mood
import com.pleos.fe.api.model.TripResponse
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WeeklyInsightTest {

    private val seoul = ZoneId.of("Asia/Seoul")

    // 2026-11-05(목) 12:00 KST 기준. 이번 주는 11-02(월) 0시부터.
    private val now = Instant.parse("2026-11-05T03:00:00Z")

    private fun trip(startUtc: String, minutes: Int, congestion: Int = 0, risk: Int = 10, mood: Mood? = null, ended: Boolean = true) =
        TripResponse(startUtc, "v1", startUtc, if (ended) startUtc else null, minutes, congestion, 0, risk, mood)

    @Test
    fun `이번 주 이동만 모으고, 힘들었던 시간대를 짚는다`() {
        val trips = listOf(
            trip("2026-11-04T09:30:00Z", 40, congestion = 15, mood = Mood.TIRED), // 수 18:30 KST 저녁
            trip("2026-11-03T10:00:00Z", 50, congestion = 20, mood = Mood.STRESSED), // 화 19:00 KST 저녁
            trip("2026-11-02T23:00:00Z", 30, mood = Mood.CALM), // 화 08:00 KST 아침
            trip("2026-11-01T10:00:00Z", 90, congestion = 40, mood = Mood.TIRED), // 지난주 일요일: 빠짐
            trip("2026-11-05T02:00:00Z", 20, ended = false), // 아직 안 끝남: 빠짐
        )

        val insight = WeeklyInsights.of(trips, now, seoul)

        assertEquals(3, insight.tripCount)
        assertEquals(40, insight.averageMinutes)
        assertEquals(35, insight.congestionMinutes)
        assertEquals(2, insight.hardTrips)
        assertEquals("저녁 이동 뒤에 피곤·답답했던 날이 많았어요(2번)", insight.pattern)
    }

    @Test
    fun `기분 기록이 없으면 가장 막힌 요일, 그것도 없으면 멀미 위험을 말한다`() {
        val congested = listOf(
            trip("2026-11-03T00:00:00Z", 40, congestion = 25), // 화
            trip("2026-11-04T00:00:00Z", 40, congestion = 5), // 수
        )
        assertEquals("가장 많이 막힌 날은 화요일이에요(25분)", WeeklyInsights.of(congested, now, seoul).pattern)

        val risky = listOf(trip("2026-11-03T00:00:00Z", 60, risk = 80), trip("2026-11-04T00:00:00Z", 60, risk = 70))
        assertEquals("멀미 위험이 높았던 이동이 2번 있었어요", WeeklyInsights.of(risky, now, seoul).pattern)
    }

    @Test
    fun `이번 주 이동이 없으면 0과 패턴 없음`() {
        val insight = WeeklyInsights.of(emptyList(), now, seoul)
        assertEquals(0, insight.tripCount)
        assertEquals(0, insight.averageMinutes)
        assertNull(insight.pattern)
    }
}
