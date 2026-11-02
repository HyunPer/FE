package com.pleos.fe.vehicle.voice

import android.content.Context
import android.content.Intent
import android.util.Log
import com.pleos.fe.api.ApiResult
import com.pleos.fe.api.apiCall
import com.pleos.fe.api.flow.DriverMatcher
import com.pleos.fe.vehicle.VehicleContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Gleo Interface(음성 비서) 호출 처리. App Schema 초안은 docs/gleo/schema.json(심사·승인 전).
 * - Gleo는 function 이름을 action으로 startActivity를 부르고(component "activity"), 파라미터는 extra ACTION_PARAM(JSON 문자열).
 * - 앱은 7초 안에 APP_ACTION_RESPONSE 브로드캐스트로 결과를 알린다. 차량 설정 적용은 더 걸릴 수 있어 "시작함"으로 먼저 답한다.
 * - 화면 흐름은 터치와 똑같이 VehicleController를 거친다(차량 설정 → 보고 → 웰컴 순서 유지).
 */
class GleoActions(
    private val context: Context,
    private val container: VehicleContainer,
    private val scope: CoroutineScope,
) {

    /** Gleo가 보낸 인텐트면 처리하고 true. */
    fun handle(intent: Intent?): Boolean {
        val function = intent?.action?.takeIf { it in FUNCTIONS } ?: return false
        val params = runCatching { JSONObject(intent.getStringExtra(EXTRA_PARAM) ?: "{}") }.getOrElse { JSONObject() }
        Log.i(TAG, "Gleo 호출 $function $params")
        when (function) {
            APPLY_NEXT_DRIVER -> {
                container.controller.board()
                respond(function, STATUS_SUCCESS, "Applying the reserved driver's profile to this car now.")
            }
            SHOW_DRIVER_PROFILES -> {
                container.controller.pickDriver()
                respond(function, STATUS_SUCCESS, "Showing the family profiles on the screen.")
            }
            APPLY_DRIVER_PROFILE -> scope.launch { applyByName(params) }
        }
        return true
    }

    private suspend fun applyByName(params: JSONObject) {
        val driverName = params.optString("driverName")
        val profileName = params.optString("profileName").ifBlank { null }
        val drivers = (apiCall { container.api.driverProfiles() } as? ApiResult.Success)?.value
            ?: return respond(APPLY_DRIVER_PROFILE, STATUS_FAIL, "Could not load the family profiles.")
        val match = DriverMatcher.find(drivers, driverName, profileName)
            ?: return respond(APPLY_DRIVER_PROFILE, STATUS_FAIL, "No matching family profile was found.")
        container.controller.choose(match.driverNickname, match.profile)
        respond(APPLY_DRIVER_PROFILE, STATUS_SUCCESS, "Applying the selected profile to this car now.")
    }

    // Callback: functionName·status(success/asking/fail)·message(영어)·mode(voice/beep/silent)·data(JSON 배열 문자열)
    private fun respond(function: String, status: String, message: String) {
        val response = JSONObject()
            .put("functionName", function)
            .put("status", status)
            .put("message", message)
            .put("mode", "voice")
            .put("data", "[]")
        context.sendBroadcast(
            Intent(ACTION_RESPONSE)
                .putExtra("packageName", context.packageName)
                .putExtra("response", response.toString()),
        )
    }

    companion object {
        const val APPLY_NEXT_DRIVER = "APPLY_NEXT_DRIVER"
        const val APPLY_DRIVER_PROFILE = "APPLY_DRIVER_PROFILE"
        const val SHOW_DRIVER_PROFILES = "SHOW_DRIVER_PROFILES"
        private val FUNCTIONS = setOf(APPLY_NEXT_DRIVER, APPLY_DRIVER_PROFILE, SHOW_DRIVER_PROFILES)

        private const val TAG = "GleoActions"
        private const val EXTRA_PARAM = "ACTION_PARAM"
        private const val ACTION_RESPONSE = "ai.pleos.playground.intent.action.APP_ACTION_RESPONSE"
        private const val STATUS_SUCCESS = "success"
        private const val STATUS_FAIL = "fail"
    }
}
