package com.pleos.fe.vehicle.sdk

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.control.LightSwitch
import com.pleos.fe.api.flow.WelcomeRenderer
import com.pleos.fe.api.model.AmbientLightDto
import com.pleos.fe.api.model.WelcomeResponse
import com.pleos.fe.api.vehicle.AmbientColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 2단계 웰컴: 화면 스킨(상태로 내보냄) → 실내 앰비언트 라이트(Vehicle SDK Light).
 * 조명 실패는 웰컴 전체를 막지 않는다(차종에 따라 앰비언트가 없을 수 있음). 결과는 ambientResult로 남긴다.
 */
class PleosWelcomeRenderer(
    private val vehicle: Vehicle?,
    private val screen: WelcomeRenderer,
) : WelcomeRenderer {

    private val lastAmbient = MutableStateFlow<ApplyItem?>(null)
    val ambientResult: StateFlow<ApplyItem?> = lastAmbient.asStateFlow()

    override suspend fun render(welcome: WelcomeResponse) {
        screen.render(welcome)
        lastAmbient.value = try {
            ApplyItem("실내 조명", true, applyAmbient(welcome.ambientLight))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApplyItem("실내 조명", false, e.message ?: e.javaClass.simpleName)
        }
    }

    private suspend fun applyAmbient(light: AmbientLightDto): String {
        val sdk = vehicle ?: return "SDK 없음 → 화면만 적용"
        val api = sdk.getLight()
        val area = awaitValue<List<SeatArea>>("앰비언트 지원 영역") { ok, fail ->
            api.checkAmbientLightBrightnessColorCapabilities({ _: CapabilityConfig, areas: List<SeatArea> -> ok(areas) }, fail)
        }.firstOrNull() ?: DRIVER

        awaitSet { ok, fail -> api.setAmbientLightSwitch(area, LightSwitch.ON, { _, _ -> ok() }, fail) }
        val colorText = try {
            awaitSet { ok, fail -> api.setAmbientLightBrightnessColor(area, AmbientColor.argb(light.color), { _, _ -> ok() }, fail) }
            light.color
        } catch (e: Exception) {
            // 임의 RGB를 받지 않는 차량(에뮬레이터 포함)일 수 있다 → 가장 가까운 지원 색으로 다시
            val name = AmbientColor.nearest(light.color)
            val fallback = AmbientColor.EMULATOR_PALETTE.getValue(name)
            awaitSet { ok, fail -> api.setAmbientLightBrightnessColor(area, AmbientColor.argb(fallback), { _, _ -> ok() }, fail) }
            "${light.color} → $name"
        }

        val bounds = runCatching {
            awaitValue<Map<SeatArea, AreaBounds<Int>>>("밝기 범위") { ok, fail ->
                api.checkAmbientLightBrightnessCapabilities({ _: CapabilityConfig, map: Map<SeatArea, AreaBounds<Int>> -> ok(map) }, fail)
            }.let { it[area] ?: it.values.firstOrNull() }
        }.getOrNull()
        val brightness = light.brightness.clampTo(bounds?.minValue, bounds?.maxValue)
        awaitSet { ok, fail -> api.setAmbientLightBrightness(area, brightness, { _, _ -> ok() }, fail) }
        return "$colorText · 밝기 $brightness"
    }
}
