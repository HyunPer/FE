package com.pleos.fe.api

import com.pleos.fe.api.flow.BoardingOutcome
import com.pleos.fe.api.flow.VehicleApplyFlow
import com.pleos.fe.api.flow.VehicleControl
import com.pleos.fe.api.flow.WelcomeRenderer
import com.pleos.fe.api.model.ButtonStyle
import com.pleos.fe.api.model.ClimateDto
import com.pleos.fe.api.model.PairRequest
import com.pleos.fe.api.model.ProfileRequest
import com.pleos.fe.api.model.ReserveRequest
import com.pleos.fe.api.model.SeatPositionRequest
import com.pleos.fe.api.model.ThemeColorsDto
import com.pleos.fe.api.model.ThemeRequest
import com.pleos.fe.api.model.VehicleSettingsDto
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * 실제 백엔드를 상대로 데모 시나리오를 끝까지 돌린다. 서버를 띄우고 주소를 넘길 때만 실행된다.
 * PLEOS_E2E_BASE_URL=http://localhost:8090/ ./gradlew :core-api:test --tests '*EndToEnd*'
 */
class EndToEndTest {

    private val baseUrl = System.getenv("PLEOS_E2E_BASE_URL").orEmpty()

    @Test
    fun `가입부터 탑승 2단계 적용까지`() = runTest {
        assumeTrue(baseUrl.isNotBlank(), "PLEOS_E2E_BASE_URL이 없어 건너뜀")

        val phoneTokens = InMemoryTokenStore()
        val vehicleTokens = InMemoryTokenStore()
        val phone = PleosApi.phone(baseUrl, phoneTokens)
        val vehicleApi = PleosApi.vehicle(baseUrl, vehicleTokens)

        val car = VehicleSession(vehicleApi, vehicleTokens).ensureRegistered("IONIQ 5").getOrThrow()
        UserSession(phone, phoneTokens).signUp("대학생 자녀").getOrThrow()
        val vehicle = phone.pair(PairRequest(car.pairingCode, "우리집 아이오닉"))

        val theme = phone.createTheme(
            ThemeRequest(
                name = "고양이 밤",
                colors = ThemeColorsDto("#0D0B1E", "#1C1840", "#7C4DFF", "#FFFFFF", "#F5F3FF"),
                buttonStyle = ButtonStyle.PILL,
                ambientColor = "#7C4DFF",
            ),
        )
        val profile = phone.createProfile(ProfileRequest(name = "밤 드라이브", climate = ClimateDto(19.0, 4), themeId = theme.id))
        phone.saveSeatPosition(profile.id, vehicle.id, SeatPositionRequest(memorySlot = 2))
        phone.reserve(vehicle.id, ReserveRequest(profile.id))

        var applied: VehicleSettingsDto? = null
        val outcome = VehicleApplyFlow(
            api = vehicleApi,
            control = VehicleControl { applied = it },
            renderer = WelcomeRenderer { },
        ).onBoarding()

        val welcome = assertIs<BoardingOutcome.Welcomed>(outcome).welcome
        assertEquals(2, applied?.seatPosition?.memorySlot)
        assertEquals(19.0, applied?.climate?.temperature)
        assertEquals("고양이 밤", welcome.skin.name)
        assertEquals(ButtonStyle.PILL, welcome.skin.buttonStyle)
        assertEquals("#7C4DFF", welcome.ambientLight.color)
        assertEquals("대학생 자녀님, 밤 드라이브 설정을 적용했어요", welcome.greeting)
        assertEquals(null, phone.currentReservation(vehicle.id).bodyOrNull(), "적용 후 대기 예약 없음")
    }
}
