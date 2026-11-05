package com.pleos.fe.api

import com.pleos.fe.api.model.VehicleRegisterRequest
import com.pleos.fe.api.model.VehicleRegisterResponse
import com.pleos.fe.api.model.VehicleSelfResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.pleos.fe.api.model.RiderResponse
import com.pleos.fe.api.model.TripResponse
import com.pleos.fe.api.model.TripSummaryRequest
import retrofit2.http.Path
/** 차량 앱 API (`/vehicle-api/v1`, 차량 토큰). */
interface VehicleApi {

    /** 첫 실행 1회. 토큰 불필요. */
    @POST("vehicle-api/v1/vehicles")
    suspend fun register(@Body request: VehicleRegisterRequest): VehicleRegisterResponse

    @GET("vehicle-api/v1/me")
    suspend fun me(): VehicleSelfResponse

    // 쉼표: 트립

    /** 탑승 감지 때. 끝내지 못한 이전 트립은 서버가 닫는다. */
    @POST("vehicle-api/v1/trips")
    suspend fun startTrip(): TripResponse

    /** 도착 때 주행 요약과 함께. 재시도해도 같은 응답. */
    @POST("vehicle-api/v1/trips/{tripId}/finish")
    suspend fun finishTrip(@Path("tripId") tripId: String, @Body summary: TripSummaryRequest): TripResponse

    /** 이 차 가족의 컨디션(멀미 민감도·쉬는 방식). */
    @GET("vehicle-api/v1/me/riders")
    suspend fun riders(): List<RiderResponse>
}
