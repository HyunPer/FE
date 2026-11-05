package com.pleos.fe.api.model

import kotlinx.serialization.Serializable

// 서버(HyunPer/BE) DTO와 같은 모양. 서버 Swagger(/v3/api-docs/phone, /v3/api-docs/vehicle)가 기준이다.
// 시각(createdAt 등)은 ISO-8601 문자열로 받는다.

// ── 사용자 ──

@Serializable
data class SignUpRequest(val nickname: String)

@Serializable
data class UserResponse(val id: String, val nickname: String, val createdAt: String)

@Serializable
data class SignUpResponse(val user: UserResponse, val accessToken: String)

@Serializable
data class UpdateUserRequest(val nickname: String)

// ── 차량·가족 (폰) ──

@Serializable
enum class VehicleRole { OWNER, MEMBER }

@Serializable
data class PairRequest(val pairingCode: String, val nickname: String? = null)

@Serializable
data class MyVehicleResponse(
    val id: String,
    val model: String,
    val nickname: String? = null,
    val myRole: VehicleRole,
    val joinedAt: String,
)

@Serializable
data class VehicleMemberResponse(val userId: String, val nickname: String, val role: VehicleRole, val joinedAt: String)

@Serializable
data class VehicleDetailResponse(
    val id: String,
    val model: String,
    val nickname: String? = null,
    val myRole: VehicleRole,
    val members: List<VehicleMemberResponse>,
    /** 가족 초대용 현재 연결 코드(멤버에게만 보임). OWNER가 재발급하면 바뀐다. */
    val pairingCode: String = "",
)

@Serializable
data class VehicleUpdateRequest(val nickname: String? = null)

@Serializable
data class PairingCodeResponse(val pairingCode: String)

@Serializable
data class TransferOwnerRequest(val userId: String)

// ── 차량 앱 ──

@Serializable
data class VehicleRegisterRequest(val model: String)

@Serializable
data class VehicleRegisterResponse(val vehicleId: String, val vehicleToken: String, val pairingCode: String)

@Serializable
data class VehicleSelfResponse(
    val id: String,
    val model: String,
    val nickname: String? = null,
    val pairingCode: String,
    val memberCount: Int,
)

/** 서버 오류 응답(RFC 9457). */
/** 서버 오류 응답(RFC 9457). */
@Serializable
data class ProblemDetail(
    val status: Int? = null,
    val title: String? = null,
    val detail: String? = null,
    val instance: String? = null,
)
