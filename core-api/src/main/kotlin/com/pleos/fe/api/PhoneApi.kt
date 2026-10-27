package com.pleos.fe.api

import com.pleos.fe.api.model.AssetResponse
import com.pleos.fe.api.model.ImportThemeRequest
import com.pleos.fe.api.model.MyVehicleResponse
import com.pleos.fe.api.model.PairRequest
import com.pleos.fe.api.model.PairingCodeResponse
import com.pleos.fe.api.model.ProfileRequest
import com.pleos.fe.api.model.ProfileResponse
import com.pleos.fe.api.model.ReservationResponse
import com.pleos.fe.api.model.ReserveRequest
import com.pleos.fe.api.model.SeatPositionDto
import com.pleos.fe.api.model.SeatPositionRequest
import com.pleos.fe.api.model.SeatPositionResponse
import com.pleos.fe.api.model.ShareResponse
import com.pleos.fe.api.model.SharedThemeResponse
import com.pleos.fe.api.model.SignUpRequest
import com.pleos.fe.api.model.SignUpResponse
import com.pleos.fe.api.model.ThemeRequest
import com.pleos.fe.api.model.ThemeResponse
import com.pleos.fe.api.model.TransferOwnerRequest
import com.pleos.fe.api.model.UpdateUserRequest
import com.pleos.fe.api.model.UserResponse
import com.pleos.fe.api.model.VehicleDetailResponse
import com.pleos.fe.api.model.VehicleUpdateRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * 폰 앱 API (`/api/v1`, 사용자 토큰). 경로는 앞에 `/`를 붙이지 않는다(기본 주소 뒤에 이어 붙음).
 * 204가 올 수 있는 호출은 Response<T>로 받아 [bodyOrNull]로 꺼낸다.
 */
interface PhoneApi {

    // 사용자
    @POST("api/v1/users")
    suspend fun signUp(@Body request: SignUpRequest): SignUpResponse

    @GET("api/v1/users/me")
    suspend fun me(): UserResponse

    @PATCH("api/v1/users/me")
    suspend fun updateMe(@Body request: UpdateUserRequest): UserResponse

    // 프로필
    @GET("api/v1/profiles")
    suspend fun profiles(): List<ProfileResponse>

    @GET("api/v1/profiles/{id}")
    suspend fun profile(@Path("id") id: String): ProfileResponse

    @POST("api/v1/profiles")
    suspend fun createProfile(@Body request: ProfileRequest): ProfileResponse

    /** 전체 교체. 프로필 전체를 보낸다. */
    @PUT("api/v1/profiles/{id}")
    suspend fun updateProfile(@Path("id") id: String, @Body request: ProfileRequest): ProfileResponse

    @DELETE("api/v1/profiles/{id}")
    suspend fun deleteProfile(@Path("id") id: String): Response<Unit>

    // 차량별 시트 자세
    @GET("api/v1/profiles/{id}/seat-positions")
    suspend fun seatPositions(@Path("id") profileId: String): List<SeatPositionResponse>

    @PUT("api/v1/profiles/{id}/seat-positions/{vehicleId}")
    suspend fun saveSeatPosition(
        @Path("id") profileId: String,
        @Path("vehicleId") vehicleId: String,
        @Body request: SeatPositionRequest,
    ): SeatPositionDto

    @DELETE("api/v1/profiles/{id}/seat-positions/{vehicleId}")
    suspend fun deleteSeatPosition(@Path("id") profileId: String, @Path("vehicleId") vehicleId: String): Response<Unit>

    // 차량·예약
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

    /** 다른 멤버가 있으면 OWNER는 먼저 넘겨야 한다(409). 내 대기 예약·이 차 시트 자세가 정리된다. */
    @DELETE("api/v1/vehicles/{vehicleId}/members/me")
    suspend fun leaveVehicle(@Path("vehicleId") vehicleId: String): Response<Unit>

    /** OWNER만. */
    @DELETE("api/v1/vehicles/{vehicleId}/members/{userId}")
    suspend fun removeMember(@Path("vehicleId") vehicleId: String, @Path("userId") userId: String): Response<Unit>

    @PUT("api/v1/vehicles/{vehicleId}/next-driver")
    suspend fun reserve(@Path("vehicleId") vehicleId: String, @Body request: ReserveRequest): ReservationResponse

    /** 대기 예약이 없으면 204 → bodyOrNull()이 null. */
    @GET("api/v1/vehicles/{vehicleId}/next-driver")
    suspend fun currentReservation(@Path("vehicleId") vehicleId: String): Response<ReservationResponse>

    /** 예약한 사람이나 OWNER만(다른 MEMBER는 403). */
    @DELETE("api/v1/vehicles/{vehicleId}/next-driver")
    suspend fun cancelReservation(@Path("vehicleId") vehicleId: String): Response<Unit>

    // 이미지·테마
    /** multipart 필드 이름은 "file". [imagePart]로 만든다. */
    @Multipart
    @POST("api/v1/assets")
    suspend fun uploadAsset(@Part file: MultipartBody.Part): AssetResponse

    @GET("api/v1/themes")
    suspend fun themes(): List<ThemeResponse>

    @GET("api/v1/themes/{id}")
    suspend fun theme(@Path("id") id: String): ThemeResponse

    @POST("api/v1/themes")
    suspend fun createTheme(@Body request: ThemeRequest): ThemeResponse

    @PUT("api/v1/themes/{id}")
    suspend fun updateTheme(@Path("id") id: String, @Body request: ThemeRequest): ThemeResponse

    @DELETE("api/v1/themes/{id}")
    suspend fun deleteTheme(@Path("id") id: String): Response<Unit>

    @POST("api/v1/themes/{id}/share")
    suspend fun shareTheme(@Path("id") id: String): ShareResponse

    @DELETE("api/v1/themes/{id}/share")
    suspend fun unshareTheme(@Path("id") id: String): Response<Unit>

    @GET("api/v1/themes/shared/{shareCode}")
    suspend fun previewSharedTheme(@Path("shareCode") shareCode: String): SharedThemeResponse

    @POST("api/v1/themes/import")
    suspend fun importTheme(@Body request: ImportThemeRequest): ThemeResponse

    @GET("api/v1/themes/market")
    suspend fun themeMarket(): List<SharedThemeResponse>
}
