package com.pleos.fe.api.flow

import com.pleos.fe.api.InMemoryTokenStore
import com.pleos.fe.api.PleosApi
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class VehicleApplyFlowTest {

    private val server = MockWebServer()
    private val events = mutableListOf<String>()

    private val nextDriver = """
        {"reservationId":"r1","driverUserId":"u1","driverNickname":"자녀","profileId":"p1","profileName":"밤 드라이브",
         "vehicleSettings":{"seat":{"heatingLevel":2,"ventilationLevel":0,"steeringWheelHeating":true},
                            "climate":{"temperature":19.0,"fanSpeed":4},
                            "seatPosition":{"memorySlot":2,"updatedAt":"2026-10-27T00:00:00Z"}}}
    """.trimIndent()

    private val welcome = """
        {"profileId":"p1","driverNickname":"자녀","widgets":["music"],
         "skin":{"name":"고양이 밤","colors":{"background":"#0D0B1E","surface":"#1C1840","primary":"#7C4DFF","onPrimary":"#FFFFFF","text":"#F5F3FF"},
                 "buttonStyle":"PILL","widgetStyle":"GLASS","fontStyle":"ROUNDED","ambientColor":"#7C4DFF"},
         "ambientLight":{"color":"#7C4DFF","brightness":70},"greeting":"자녀님, 밤 드라이브 설정을 적용했어요"}
    """.trimIndent()

    @BeforeTest
    fun start() = server.start()

    @AfterTest
    fun stop() = server.close()

    private fun json(body: String) =
        MockResponse.Builder().code(200).addHeader("Content-Type", "application/json").body(body).build()

    private fun flow(control: VehicleControl = VehicleControl { events += "control:${it.seatPosition?.memorySlot}" }) =
        VehicleApplyFlow(
            api = PleosApi.vehicle(server.url("/").toString(), InMemoryTokenStore("vt")),
            control = control,
            renderer = WelcomeRenderer { events += "render:${it.skin.name}" },
        )

    @Test
    fun `차량 설정을 먼저 적용하고 보고한 뒤에야 웰컴을 그린다`() = runTest {
        server.enqueue(json(nextDriver))
        server.enqueue(json(welcome))

        val outcome = flow().onBoarding()

        val welcomed = assertIs<BoardingOutcome.Welcomed>(outcome)
        assertEquals(70, welcomed.welcome.ambientLight.brightness)
        assertEquals(listOf("control:2", "render:고양이 밤"), events)
        server.takeRequest()
        assertEquals("/vehicle-api/v1/me/next-driver/r1/applied", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `예약이 없으면 1탭 선택용 프로필 목록을 돌려준다`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        server.enqueue(json("""[{"userId":"u1","nickname":"엄마","profiles":[{"profileId":"p9","name":"출근","vehicleSettings":{"seat":{},"climate":{}}}]}]"""))

        val outcome = assertIs<BoardingOutcome.ChooseDriver>(flow().onBoarding())

        assertEquals("출근", outcome.drivers.single().profiles.single().name)
        assertEquals(emptyList(), events, "아무것도 적용하지 않는다")
    }

    @Test
    fun `차량 설정이 실패하면 보고하지 않고 멈춘다`() = runTest {
        server.enqueue(json(nextDriver))

        val outcome = flow(control = { error("ERROR_DRIVING_RESTRICTED") }).onBoarding()

        val failed = assertIs<BoardingOutcome.Failed>(outcome)
        assertEquals(BoardingOutcome.Step.VEHICLE_SETTINGS, failed.step)
        assertEquals(1, server.requestCount, "applied 보고를 보내지 않았다")
    }
}
