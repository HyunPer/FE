package com.pleos.fe.api

import com.pleos.fe.api.model.ProblemDetail
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** 화면이 분기하기 쉬운 결과 타입. 예외를 화면까지 던지지 않는다. */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

/**
 * - Http: 서버가 ProblemDetail로 응답한 오류. [detail]은 서버가 준 한국어 설명이라 그대로 보여줘도 된다.
 * - Network: 연결 끊김·시간 초과. 주행 중 통신 끊김을 고려해 재시도 버튼을 보여준다.
 */
sealed class ApiError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Http(val status: Int, val detail: String?) : ApiError(detail ?: "HTTP $status") {
        val isUnauthorized get() = status == 401
        val isNotFound get() = status == 404
        val isConflict get() = status == 409
    }

    class Network(cause: IOException) : ApiError("네트워크에 연결할 수 없습니다", cause)

    class Unexpected(cause: Throwable) : ApiError(cause.message ?: "알 수 없는 오류", cause)
}

/** 서버 호출을 감싸 [ApiResult]로 바꾼다. 코루틴 취소는 그대로 전파한다. */
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    ApiResult.Failure(e.toApiError())
} catch (e: IOException) {
    ApiResult.Failure(ApiError.Network(e))
} catch (e: Exception) {
    ApiResult.Failure(ApiError.Unexpected(e))
}

/** 204면 null, 2xx면 본문, 그 외는 HttpException(→ apiCall이 ApiError.Http로 바꿈). */
fun <T> Response<T>.bodyOrNull(): T? = when {
    code() == 204 -> null
    isSuccessful -> body()
    else -> throw HttpException(this)
}

/** 본문 없는 성공(204)을 확인만 한다. */
fun Response<Unit>.requireSuccess() {
    if (!isSuccessful) throw HttpException(this)
}

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.value

fun <T> ApiResult<T>.getOrThrow(): T = when (this) {
    is ApiResult.Success -> value
    is ApiResult.Failure -> throw error
}

private fun HttpException.toApiError(): ApiError.Http {
    val detail = runCatching {
        response()?.errorBody()?.string()?.let { PleosApi.json.decodeFromString<ProblemDetail>(it).detail }
    }.getOrNull()
    return ApiError.Http(code(), detail)
}
