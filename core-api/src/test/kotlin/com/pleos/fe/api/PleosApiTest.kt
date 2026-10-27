package com.pleos.fe.api

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PleosApiTest {

    private val server = MockWebServer()
    private val tokens = InMemoryTokenStore()

    @BeforeTest
    fun start() = server.start()

    @AfterTest
    fun stop() = server.close()

    private fun phone() = PleosApi.phone(server.url("/").toString(), tokens)
    private fun vehicle() = PleosApi.vehicle(server.url("/").toString(), tokens)

    private fun json(body: String, code: Int = 200) =
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `가입하면 토큰을 저장하고 다음 요청부터 Bearer 헤더를 붙인다`() = runTest {
        server.enqueue(json("""{"user":{"id":"u1","nickname":"엄마","createdAt":"2026-10-27T00:00:00Z"},"accessToken":"tok-1"}""", 201))
        server.enqueue(json("""{"id":"u1","nickname":"엄마","createdAt":"2026-10-27T00:00:00Z"}"""))
        val session = UserSession(phone(), tokens)

        val user = session.signUp("엄마").getOrThrow()
        session.me().getOrThrow()

        assertEquals("엄마", user.nickname)
        assertNull(server.takeRequest().headers["Authorization"], "가입 요청에는 토큰이 없다")
        val me = server.takeRequest()
        assertEquals("/api/v1/users/me", me.url.encodedPath)
        assertEquals("Bearer tok-1", me.headers["Authorization"])
    }

    @Test
    fun `서버 ProblemDetail의 한국어 설명을 ApiError로 전달한다`() = runTest {
        server.enqueue(
            MockResponse.Builder().code(400).addHeader("Content-Type", "application/problem+json")
                .body("""{"status":400,"title":"Bad Request","detail":"글자색과 배경색 대비가 부족합니다"}""").build(),
        )

        val result = apiCall { phone().themes() }

        val error = assertIs<ApiError.Http>((result as ApiResult.Failure).error)
        assertEquals(400, error.status)
        assertEquals("글자색과 배경색 대비가 부족합니다", error.detail)
    }

    @Test
    fun `401이면 토큰을 지운다`() = runTest {
        tokens.set("expired")
        server.enqueue(json("""{"status":401,"detail":"유효하지 않은 토큰입니다"}""", 401))

        UserSession(phone(), tokens).me()

        assertNull(tokens.get())
    }

    @Test
    fun `204는 null로, 연결 실패는 Network 오류로`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        assertNull(apiCall { vehicle().nextDriver().bodyOrNull() }.getOrThrow())

        server.close()
        val result = apiCall { vehicle().me() }
        assertIs<ApiError.Network>((result as ApiResult.Failure).error)
    }

    @Test
    fun `차량은 처음 한 번만 등록하고 토큰을 재사용한다`() = runTest {
        server.enqueue(json("""{"vehicleId":"v1","vehicleToken":"vt-1","pairingCode":"742017"}""", 201))
        server.enqueue(json("""{"id":"v1","model":"IONIQ 5","pairingCode":"742017","memberCount":0}"""))
        server.enqueue(json("""{"id":"v1","model":"IONIQ 5","pairingCode":"742017","memberCount":1}"""))
        val session = VehicleSession(vehicle(), tokens)

        assertEquals("742017", session.ensureRegistered("IONIQ 5").getOrThrow().pairingCode)
        assertEquals(1, session.ensureRegistered("IONIQ 5").getOrThrow().memberCount)

        assertEquals("/vehicle-api/v1/vehicles", server.takeRequest().url.encodedPath)
        assertEquals("Bearer vt-1", server.takeRequest().headers["Authorization"])
        assertEquals("/vehicle-api/v1/me", server.takeRequest().url.encodedPath, "두 번째는 등록 없이 바로 조회")
    }

    @Test
    fun `이미지 주소를 기본 주소와 합친다`() {
        assertEquals("http://10.0.2.2:8090/files/abc", assetUrl("http://10.0.2.2:8090/", "/files/abc"))
        assertEquals("http://10.0.2.2:8090/files/abc", assetUrl("http://10.0.2.2:8090", "files/abc"))
    }
}
