package com.pleos.fe.api.vehicle

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.math.sign

enum class MoveDirection { FORWARD, BACKWARD }

/**
 * 시트의 한 축(등받이 각도, 앞뒤 위치 등). Pleos Vehicle SDK의 moveSeat*·getCurrentSeat*Position·registerSeat*Position을 감싼다.
 * 위치 값은 차량 단위 그대로의 정수다.
 */
interface SeatAxis {
    suspend fun current(): Int?
    fun move(direction: MoveDirection)
    fun stop()
    /** 움직이는 동안 들어오는 위치 업데이트. */
    val positions: Flow<Int>
}

sealed interface SeatMoveOutcome {
    data object AlreadyThere : SeatMoveOutcome
    data class Reached(val position: Int) : SeatMoveOutcome
    data class TimedOut(val lastPosition: Int) : SeatMoveOutcome
    /** 현재 위치를 읽지 못함(미지원 차종 등). 움직이지 않았다. */
    data object Unknown : SeatMoveOutcome
}

/**
 * 목표 위치까지 폐루프 이동. SDK는 절대 위치 지정이 없고 FORWARD/STOP/BACKWARD만 있다.
 * FORWARD가 값을 늘리는지 줄이는지 문서에 없어서, "FORWARD = 값 증가"로 가정해 출발하고
 * 첫 업데이트에서 목표와 멀어지면 한 번 방향을 뒤집는다. 목표에 닿거나 지나치면 멈추고, 시간 초과에도 반드시 STOP을 보낸다.
 */
class SeatAxisMover(
    private val timeoutMs: Long = 8_000,
    private val tolerance: Int = 0,
) {
    suspend fun moveTo(axis: SeatAxis, target: Int): SeatMoveOutcome {
        val start = axis.current() ?: return SeatMoveOutcome.Unknown
        if (abs(start - target) <= tolerance) return SeatMoveOutcome.AlreadyThere

        var direction = if (target > start) MoveDirection.FORWARD else MoveDirection.BACKWARD
        var reference = start
        var last = start
        var flipped = false
        return try {
            withTimeout(timeoutMs) {
                axis.move(direction)
                axis.positions.first { position ->
                    val reached = abs(position - target) <= tolerance ||
                        (position - target).sign != (reference - target).sign
                    val movingAway = abs(position - target) > abs(last - target)
                    last = position
                    if (!reached && movingAway && !flipped) {
                        flipped = true
                        reference = position
                        direction = direction.opposite()
                        axis.move(direction)
                    }
                    reached
                }
                SeatMoveOutcome.Reached(last)
            }
        } catch (e: TimeoutCancellationException) {
            SeatMoveOutcome.TimedOut(last)
        } catch (e: CancellationException) {
            throw e
        } finally {
            axis.stop()
        }
    }

    private fun MoveDirection.opposite() = if (this == MoveDirection.FORWARD) MoveDirection.BACKWARD else MoveDirection.FORWARD
}
