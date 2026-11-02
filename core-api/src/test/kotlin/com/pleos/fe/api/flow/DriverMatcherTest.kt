package com.pleos.fe.api.flow

import com.pleos.fe.api.model.ApplicableProfileDto
import com.pleos.fe.api.model.ClimateDto
import com.pleos.fe.api.model.DriverProfilesResponse
import com.pleos.fe.api.model.SeatDto
import com.pleos.fe.api.model.VehicleSettingsDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DriverMatcherTest {

    private fun profile(id: String, name: String) = ApplicableProfileDto(id, name, VehicleSettingsDto(SeatDto(), ClimateDto()))

    private val drivers = listOf(
        DriverProfilesResponse("u1", "엄마", listOf(profile("p1", "출근"), profile("p2", "주말 드라이브"))),
        DriverProfilesResponse("u2", "대학생 자녀", listOf(profile("p3", "밤"))),
        DriverProfilesResponse("u3", "손님", emptyList()),
    )

    @Test
    fun `이름만 말하면 그 사람의 첫 프로필`() {
        assertEquals("p1", DriverMatcher.find(drivers, "엄마")?.profile?.profileId)
    }

    @Test
    fun `띄어쓰기와 일부만 말해도 찾는다`() {
        val match = DriverMatcher.find(drivers, "대학생자녀")
        assertEquals("대학생 자녀", match?.driverNickname)
        assertEquals("p2", DriverMatcher.find(drivers, "엄마", "주말드라이브")?.profile?.profileId)
        assertEquals("p2", DriverMatcher.find(drivers, "엄마", "주말")?.profile?.profileId)
    }

    @Test
    fun `없는 사람, 없는 프로필, 프로필 없는 사람은 null`() {
        assertNull(DriverMatcher.find(drivers, "아빠"))
        assertNull(DriverMatcher.find(drivers, "엄마", "퇴근"))
        assertNull(DriverMatcher.find(drivers, "손님"))
        assertNull(DriverMatcher.find(drivers, " "))
    }
}
