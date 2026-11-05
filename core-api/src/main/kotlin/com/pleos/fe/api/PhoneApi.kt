package com.pleos.fe.api

import com.pleos.fe.api.model.MyVehicleResponse
import com.pleos.fe.api.model.PairRequest
import com.pleos.fe.api.model.PairingCodeResponse
import com.pleos.fe.api.model.SignUpRequest
import com.pleos.fe.api.model.SignUpResponse
import com.pleos.fe.api.model.TransferOwnerRequest
import com.pleos.fe.api.model.UpdateUserRequest
import com.pleos.fe.api.model.UserResponse
import com.pleos.fe.api.model.VehicleDetailResponse
import com.pleos.fe.api.model.VehicleUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import com.pleos.fe.api.model.CheckInRequest
import com.pleos.fe.api.model.RiderConditionDto
import com.pleos.fe.api.model.TripResponse
import retrofit2.http.PUT
import retrofit2.http.Query
/**
 * 폰 앱 API (`/api/v1`, 사용자 토큰). 경로는 앞에 `/`를 붙이지 않는다(기본 주소 뒤에 이어 붙음).
 * 본문 없는 성공(204)은 Response<Unit>으로 받아 [requireSuccess]로 확인한다.
 */
interface PhoneApi {

    // 사용자
    @POST("api/v1/users")
    suspend fun signUp(@Body request: SignUpRequest): SignUpResponse

    @GET("api/v1/users/me")
    suspend fun me(): UserResponse

    @PATCH("api/v1/users/me")
    suspend fun updateMe(@Body request: UpdateUserRequest): UserResponse

    // 차량·가족
    @POST("api/v1/vehicles/pair")
    suspend fun pair(@Body request: PairRequest): MyVehicleResponse

    @GET("api/v1/vehicles")
    suspend fun vehicles(): List<MyVehicleResponse>

    @GET("api/v1/vehicles/{vehicleId}")
    suspend fun vehicle(@Path("vehicleId") vehicleId: String): VehicleDetailResponse

    /** OWNER만(MEMBER는 403). */
    @PATCH("api/v1/vehicles/{vehicleId}")
    suspend fun renameVehicle(@Path("vehicleId") vehicleId: String, @Body request: VehicleUpdateRequest): VehicleDetailResponse

    /** OWNER만. 이전 코드는 바로 무효. */
    @POST("api/v1/vehicles/{vehicleId}/pairing-code")
    suspend fun regeneratePairingCode(@Path("vehicleId") vehicleId: String): PairingCodeResponse

    /** OWNER만. 나는 MEMBER가 된다. */
    @POST("api/v1/vehicles/{vehicleId}/owner")
    suspend fun transferOwner(@Path("vehicleId") vehicleId: String, @Body request: TransferOwnerRequest): VehicleDetailResponse

    /** 다른 멤버가 있으면 OWNER는 먼저 넘겨야 한다(409). */
    @DELETE("api/v1/vehicles/{vehicleId}/members/me")
    suspend fun leaveVehicle(@Path("vehicleId") vehicleId: String): Response<Unit>

    /** OWNER만. */
    @DELETE("api/v1/vehicles/{vehicleId}/members/{userId}")
    suspend fun removeMember(@Path("vehicleId") vehicleId: String, @Path("userId") userId: String): Response<Unit>

    @GET("api/v1/vehicles/{vehicleId}/trips")
    suspend fun trips(@Path("vehicleId") vehicleId: String, @Query("limit") limit: Int = 20): List<TripResponse>
    @PUT("api/v1/trips/{tripId}/check-in")
    suspend fun checkIn(@Path("tripId") tripId: String, @Body request: CheckInRequest): TripResponse
    @GET("api/v1/users/me/condition")
    suspend fun myCondition(): RiderConditionDto
    @PUT("api/v1/users/me/condition")
    suspend fun updateMyCondition(@Body request: RiderConditionDto): RiderConditionDto
}
