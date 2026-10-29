package com.pleos.fe.api.vehicle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SeatAxisMoverTest {

    /** 100ms마다 한 칸씩 움직이는 가짜 시트. forwardIncreases=false면 FORWARD가 값을 줄인다. */
    private class FakeAxis(
        private val scope: CoroutineScope,
        start: Int,
        private val forwardIncreases: Boolean = true,
        private val stuck: Boolean = false,
    ) : SeatAxis {
        var position = start
        val commands = mutableListOf<String>()
        private var job: Job? = null
        override val positions = MutableSharedFlow<Int>(extraBufferCapacity = 64)

        override suspend fun current() = position
        override fun move(direction: MoveDirection) {
            commands += direction.name
            job?.cancel()
            val step = if ((direction == MoveDirection.FORWARD) == forwardIncreases) 1 else -1
            job = scope.launch {
                while (true) {
                    delay(100)
                    if (!stuck) position += step
                    positions.emit(position)
                }
            }
        }
        override fun stop() {
            commands += "STOP"
            job?.cancel()
        }
    }

    @Test
    fun `목표까지 움직이고 멈춘다`() = runTest {
        val axis = FakeAxis(backgroundScope, start = 10)
        val outcome = SeatAxisMover().moveTo(axis, target = 15)

        assertEquals(SeatMoveOutcome.Reached(15), outcome)
        assertEquals(listOf("FORWARD", "STOP"), axis.commands)
    }

    @Test
    fun `FORWARD가 값을 줄이는 차량이면 한 번 뒤집어서 도착한다`() = runTest {
        val axis = FakeAxis(backgroundScope, start = 10, forwardIncreases = false)
        val outcome = SeatAxisMover().moveTo(axis, target = 14)

        assertIs<SeatMoveOutcome.Reached>(outcome)
        assertEquals(14, axis.position)
        assertEquals(listOf("FORWARD", "BACKWARD", "STOP"), axis.commands)
    }

    @Test
    fun `움직이지 않으면 시간 초과 후 반드시 STOP`() = runTest {
        val axis = FakeAxis(backgroundScope, start = 10, stuck = true)
        val outcome = SeatAxisMover(timeoutMs = 2_000).moveTo(axis, target = 20)

        assertIs<SeatMoveOutcome.TimedOut>(outcome)
        assertTrue(axis.commands.last() == "STOP")
    }

    @Test
    fun `이미 목표면 움직이지 않는다`() = runTest {
        val axis = FakeAxis(backgroundScope, start = 7)
        assertEquals(SeatMoveOutcome.AlreadyThere, SeatAxisMover().moveTo(axis, target = 7))
        assertTrue(axis.commands.isEmpty())
    }

    @Test
    fun `앰비언트 색 변환과 가까운 팔레트`() {
        assertEquals(0xFF7C4DFF.toInt(), AmbientColor.argb("#7C4DFF"))
        assertEquals("Purple", AmbientColor.nearest("#7C4DFF"))
        assertEquals("Cyan", AmbientColor.nearest("#00BCD4"))
        assertEquals("White", AmbientColor.nearest("#F5F3FF"))
    }
}
