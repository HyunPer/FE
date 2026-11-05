package com.pleos.fe.vehicle.sdk

import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** 운전석(LHD 기준). 탑승 감지(착석)에 쓴다. */
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
