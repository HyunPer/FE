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

// ── 프로필 ──

@Serializable
data class DisplayDto(val widgets: List<String> = emptyList())

/** 시트 열선·통풍 단계(0–3)와 핸들 열선. 시트 위치는 차량별 SeatPosition. */
@Serializable
data class SeatDto(
    val heatingLevel: Int = 0,
    val ventilationLevel: Int = 0,
    val steeringWheelHeating: Boolean = false,
)

@Serializable
data class ClimateDto(val temperature: Double = 22.0, val fanSpeed: Int = 3)

/** 앰비언트 밝기. 색은 테마의 ambientColor. */
@Serializable
data class LightDto(val brightness: Int = 50)

@Serializable
data class ProfileRequest(
    val name: String,
    val display: DisplayDto = DisplayDto(),
    val seat: SeatDto = SeatDto(),
    val climate: ClimateDto = ClimateDto(),
    val light: LightDto = LightDto(),
    val welcomeSoundId: String? = null,
    val themeId: String? = null,
)

@Serializable
data class ProfileResponse(
    val id: String,
    val name: String,
    val display: DisplayDto,
    val seat: SeatDto,
    val climate: ClimateDto,
    val light: LightDto,
    val welcomeSoundId: String? = null,
    val themeId: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

/** 셋 중 하나 이상. 위치값은 차량 단위 그대로. */
@Serializable
data class SeatPositionRequest(
    val memorySlot: Int? = null,
    val backrestPosition: Int? = null,
    val foreAftPosition: Int? = null,
)

@Serializable
data class SeatPositionDto(
    val memorySlot: Int? = null,
    val backrestPosition: Int? = null,
    val foreAftPosition: Int? = null,
    val updatedAt: String,
)

@Serializable
data class SeatPositionResponse(
    val vehicleId: String,
    val vehicleNickname: String? = null,
    val vehicleModel: String,
    val position: SeatPositionDto,
)

// ── 차량·예약 (폰) ──

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
)

@Serializable
enum class ReservationStatus { PENDING, APPLIED, CANCELLED }

@Serializable
data class ReserveRequest(val profileId: String)

@Serializable
data class ReservationResponse(
    val id: String,
    val vehicleId: String,
    val driverUserId: String,
    val driverNickname: String,
    val profileId: String,
    val profileName: String,
    val status: ReservationStatus,
    val createdAt: String,
    val appliedAt: String? = null,
)

// ── 테마·이미지 ──

@Serializable
enum class ButtonStyle { ROUNDED, PILL, SQUARE, OUTLINED }

@Serializable
enum class WidgetStyle { FILLED, OUTLINED, GLASS }

@Serializable
enum class FontStyle { DEFAULT, ROUNDED, BOLD }

@Serializable
enum class CharacterPlacement { BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, WELCOME_ONLY }

@Serializable
data class ThemeColorsDto(
    val background: String = "#121212",
    val surface: String = "#1E1E1E",
    val primary: String = "#1E88E5",
    val onPrimary: String = "#FFFFFF",
    val text: String = "#FFFFFF",
)

@Serializable
data class ThemeRequest(
    val name: String,
    val colors: ThemeColorsDto = ThemeColorsDto(),
    val backgroundAssetId: String? = null,
    val characterAssetId: String? = null,
    val characterPlacement: CharacterPlacement = CharacterPlacement.BOTTOM_RIGHT,
    val buttonStyle: ButtonStyle = ButtonStyle.ROUNDED,
    val widgetStyle: WidgetStyle = WidgetStyle.FILLED,
    val fontStyle: FontStyle = FontStyle.DEFAULT,
    val ambientColor: String = "#FFFFFF",
)

@Serializable
data class CharacterDto(val imageUrl: String, val placement: CharacterPlacement)

/** 화면을 그리는 데 필요한 값. 폰 미리보기·마켓·차량 화면이 모두 이걸로 그린다. 이미지 주소는 assetUrl()로 완성한다. */
@Serializable
data class ThemeSkinDto(
    val name: String,
    val colors: ThemeColorsDto,
    val backgroundImageUrl: String? = null,
    val character: CharacterDto? = null,
    val buttonStyle: ButtonStyle,
    val widgetStyle: WidgetStyle,
    val fontStyle: FontStyle,
    val ambientColor: String,
)

@Serializable
data class ThemeResponse(
    val id: String,
    val skin: ThemeSkinDto,
    val backgroundAssetId: String? = null,
    val characterAssetId: String? = null,
    val shareCode: String? = null,
    val imported: Boolean,
    val importCount: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class SharedThemeResponse(
    val shareCode: String,
    val authorNickname: String,
    val importCount: Int,
    val skin: ThemeSkinDto,
    val createdAt: String,
)

@Serializable
data class ShareResponse(val shareCode: String)

@Serializable
data class ImportThemeRequest(val shareCode: String)

@Serializable
data class AssetResponse(val id: String, val url: String, val contentType: String, val sizeBytes: Long, val createdAt: String)

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

/** 1단계: Vehicle SDK로 바로 적용하는 값. seatPosition이 null이면 시트를 움직이지 않는다. */
@Serializable
data class VehicleSettingsDto(
    val seat: SeatDto,
    val climate: ClimateDto,
    val seatPosition: SeatPositionDto? = null,
)

@Serializable
data class ApplicableProfileDto(val profileId: String, val name: String, val vehicleSettings: VehicleSettingsDto)

@Serializable
data class DriverProfilesResponse(val userId: String, val nickname: String, val profiles: List<ApplicableProfileDto>)

@Serializable
data class NextDriverResponse(
    val reservationId: String,
    val driverUserId: String,
    val driverNickname: String,
    val profileId: String,
    val profileName: String,
    val vehicleSettings: VehicleSettingsDto,
)

@Serializable
data class AmbientLightDto(val color: String, val brightness: Int)

/** 2단계: 차량 설정 적용 보고 후 받는 웰컴 묶음. */
@Serializable
data class WelcomeResponse(
    val profileId: String,
    val driverNickname: String,
    val skin: ThemeSkinDto,
    val widgets: List<String>,
    val ambientLight: AmbientLightDto,
    val welcomeSoundId: String? = null,
    val greeting: String,
)

/** 서버 오류 응답(RFC 9457). */
@Serializable
data class ProblemDetail(
    val status: Int? = null,
    val title: String? = null,
    val detail: String? = null,
    val instance: String? = null,
)
