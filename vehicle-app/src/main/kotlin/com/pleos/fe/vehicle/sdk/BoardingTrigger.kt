package com.pleos.fe.vehicle.sdk

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 탑승 감지 트리거. 시동 시 앱 자동 실행은 서드파티에 허용되지 않으므로, 앱이 떠 있는 동안
 * 운전석 착석(registerSeatOccupancy) + P단(registerGearSelection)을 감지해 한 번만 2단계 적용을 시작한다.
 * 운전석이 비면 다시 대기한다. Virtual Console에서 착석·기어를 바꿔 시연할 수 있다.
 */
class BoardingTrigger(
    private val vehicle: Vehicle,
    private val scope: CoroutineScope,
    private val onBoarding: suspend () -> Unit,
) {
    private var occupied = false
    private var gear: VehicleGear? = null
    private var fired = false
    private var running: Job? = null

    private val occupancyListener = object : SeatOccupancyListener {
        override fun onSeatOccupancyUpdated(occupied: Boolean?) {
            this@BoardingTrigger.occupied = occupied == true
            if (!this@BoardingTrigger.occupied) fired = false
            evaluate()
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "착석 감지 실패: ${e.message}")
        }
    }

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            this@BoardingTrigger.gear = gear
            evaluate()
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "기어 감지 실패: ${e.message}")
        }
    }

    fun start() {
        vehicle.getSeat().registerSeatOccupancy(DRIVER, occupancyListener)
        vehicle.getGear().registerGearSelection(gearListener)
    }

    fun stop() {
        vehicle.getSeat().unregisterSeatOccupancy(DRIVER, occupancyListener)
        vehicle.getGear().unregisterGearSelection(gearListener)
    }

    /** 시트·공조는 주행 중 제한될 수 있어(ERROR_DRIVING_RESTRICTED) 정차(P단 또는 기어 미확인) 중에만 시작한다. */
    private fun evaluate() {
        val parked = gear == null || gear == VehicleGear.GEAR_PARK
        if (occupied && parked && !fired && running?.isActive != true) {
            fired = true
            running = scope.launch { onBoarding() }
        }
    }

    private companion object {
        const val TAG = "BoardingTrigger"
    }
}
