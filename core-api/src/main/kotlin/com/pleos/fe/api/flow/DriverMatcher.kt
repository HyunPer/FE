package com.pleos.fe.api.flow

import com.pleos.fe.api.model.ApplicableProfileDto
import com.pleos.fe.api.model.DriverProfilesResponse

/**
 * 음성 명령("엄마 출근 프로필로")의 이름을 차량 멤버·프로필에 맞춘다.
 * 음성 인식 결과는 띄어쓰기·호칭이 흔들리므로 공백을 빼고 같거나 포함하면 같은 사람으로 본다.
 */
object DriverMatcher {

    data class Match(val driverNickname: String, val profile: ApplicableProfileDto)

    /** 프로필 이름이 없으면 그 사람의 첫 프로필. 사람이 여럿 걸리면 정확히 같은 이름을 우선한다. */
    fun find(drivers: List<DriverProfilesResponse>, driverName: String, profileName: String? = null): Match? {
        val wanted = driverName.normalized()
        if (wanted.isEmpty()) return null
        val candidates = drivers.filter { it.profiles.isNotEmpty() && it.nickname.normalized().let { n -> n == wanted || n in wanted || wanted in n } }
        val driver = candidates.firstOrNull { it.nickname.normalized() == wanted } ?: candidates.firstOrNull() ?: return null

        val profile = profileName?.normalized()?.takeIf { it.isNotEmpty() }?.let { p ->
            driver.profiles.firstOrNull { it.name.normalized() == p }
                ?: driver.profiles.firstOrNull { it.name.normalized().let { n -> p in n || n in p } }
                ?: return null
        } ?: driver.profiles.first()
        return Match(driver.nickname, profile)
    }

    private fun String.normalized() = filterNot { it.isWhitespace() }.lowercase()
}
