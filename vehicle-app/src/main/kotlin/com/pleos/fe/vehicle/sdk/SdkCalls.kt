package com.pleos.fe.vehicle.sdk

import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** 운전석(LHD 기준). 프로필은 운전자 개인 설정이라 운전석에만 적용한다. */
val DRIVER = SeatArea(SeatValue.ROW_1_LEFT)

/**
 * 조회·Capability 호출: SDK가 onComplete로 값을 준다. 시간 안에 오지 않으면 예외.
 * SDK 콜백은 IPC라 메인 스레드를 막지 않도록 suspend로 감싼다.
 */
suspend fun <T> awaitValue(
    what: String,
    timeoutMs: Long = 3_000,
    call: (onComplete: (T) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
): T = withTimeoutOrNull(timeoutMs) {
    suspendCancellableCoroutine<Result<T>> { cont ->
        call(
            { value -> if (cont.isActive) cont.resume(Result.success(value)) },
            { e -> if (cont.isActive) cont.resume(Result.failure(e)) },
        )
    }
}?.getOrThrow() ?: throw IllegalStateException("$what: 차량 응답 없음")

enum class SetResult { CONFIRMED, SENT }

/**
 * 설정(set) 호출. 문서상 set은 실패할 때만 onFailed가 확실히 오고 onComplete는 보장되지 않는다.
 * 그래서 시간 안에 onComplete가 오면 CONFIRMED, 아무 응답이 없으면 SENT(요청은 보냄), onFailed면 예외로 본다.
 */
suspend fun awaitSet(
    timeoutMs: Long = 1_500,
    call: (onComplete: () -> Unit, onFailed: (Exception) -> Unit) -> Unit,
): SetResult {
    val result = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine<Result<SetResult>> { cont ->
            call(
                { if (cont.isActive) cont.resume(Result.success(SetResult.CONFIRMED)) },
                { e -> if (cont.isActive) cont.resume(Result.failure(e)) },
            )
        }
    } ?: return SetResult.SENT
    return result.getOrThrow()
}

/** 차량 범위에 맞춘다. 범위를 모르면 원래 값. */
fun <T : Comparable<T>> T.clampTo(min: T?, max: T?): T =
    if (min == null || max == null) this else coerceIn(min, max)
