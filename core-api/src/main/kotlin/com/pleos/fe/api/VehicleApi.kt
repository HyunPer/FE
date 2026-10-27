package com.pleos.fe.api

import com.pleos.fe.api.model.DriverProfilesResponse
import com.pleos.fe.api.model.NextDriverResponse
import com.pleos.fe.api.model.SeatPositionDto
import com.pleos.fe.api.model.SeatPositionRequest
import com.pleos.fe.api.model.VehicleRegisterRequest
import com.pleos.fe.api.model.VehicleRegisterResponse
import com.pleos.fe.api.model.VehicleSelfResponse
import com.pleos.fe.api.model.WelcomeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** 차량 앱 API (`/vehicle-api/v1`, 차량 토큰). */
interface VehicleApi {

    /** 첫 실행 1회. 토큰 불필요. */
    @POST("vehicle-api/v1/vehicles")
    suspend fun register(@Body request: VehicleRegisterRequest): VehicleRegisterResponse

    @GET("vehicle-api/v1/me")
    suspend fun me(): VehicleSelfResponse

    /** 1탭 선택 화면용. 차량 설정만 들어 있다. */
    @GET("vehicle-api/v1/me/profiles")
    suspend fun driverProfiles(): List<DriverProfilesResponse>

    /** 1단계. 예약이 없으면 204 → bodyOrNull()이 null. */
    @GET("vehicle-api/v1/me/next-driver")
    suspend fun nextDriver(): Response<NextDriverResponse>

    /** 차량 설정 적용 후 보고 → 2단계 웰컴 묶음. 재시도해도 같은 응답. */
    @POST("vehicle-api/v1/me/next-driver/{reservationId}/applied")
    suspend fun reportApplied(@Path("reservationId") reservationId: String): WelcomeResponse

    /** 1탭으로 고른 프로필의 차량 설정 적용 후 보고 → 2단계 웰컴 묶음. */
    @POST("vehicle-api/v1/me/profiles/{profileId}/applied")
    suspend fun reportSelectedApplied(@Path("profileId") profileId: String): WelcomeResponse

    /** 지금 자세 저장. SDK로 읽은 현재 위치·메모리 슬롯. */
    @PUT("vehicle-api/v1/me/profiles/{profileId}/seat-position")
    suspend fun saveSeatPosition(@Path("profileId") profileId: String, @Body request: SeatPositionRequest): SeatPositionDto
}
