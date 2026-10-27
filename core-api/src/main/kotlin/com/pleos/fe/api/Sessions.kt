package com.pleos.fe.api

import com.pleos.fe.api.model.SignUpRequest
import com.pleos.fe.api.model.UserResponse
import com.pleos.fe.api.model.VehicleRegisterRequest
import com.pleos.fe.api.model.VehicleSelfResponse

/** 폰 앱 계정. 첫 실행 때 닉네임으로 가입하고 토큰을 보관한다. */
class UserSession(private val api: PhoneApi, private val tokens: TokenStore) {

    val isSignedIn: Boolean get() = tokens.get() != null

    suspend fun signUp(nickname: String): ApiResult<UserResponse> = apiCall {
        val response = api.signUp(SignUpRequest(nickname))
        tokens.set(response.accessToken)
        response.user
    }

    /** 401이면 토큰이 무효다. 토큰을 지우고 가입 화면으로 보낸다. */
    suspend fun me(): ApiResult<UserResponse> {
        val result = apiCall { api.me() }
        val error = (result as? ApiResult.Failure)?.error
        if (error is ApiError.Http && error.isUnauthorized) tokens.set(null)
        return result
    }

    fun signOut() = tokens.set(null)
}

/** 차량 앱의 차량 등록. 처음이면 등록해서 토큰을 저장하고, 페어링 코드를 화면에 띄울 정보를 돌려준다. */
class VehicleSession(private val api: VehicleApi, private val tokens: TokenStore) {

    val isRegistered: Boolean get() = tokens.get() != null

    suspend fun ensureRegistered(model: String): ApiResult<VehicleSelfResponse> = apiCall {
        if (tokens.get() == null) {
            tokens.set(api.register(VehicleRegisterRequest(model)).vehicleToken)
        }
        api.me()
    }
}
