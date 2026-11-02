package com.pleos.fe.vehicle.sdk

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import android.speech.tts.TextToSpeech as AndroidTts

/**
 * 웰컴 인사말 음성.
 * Pleos TextToSpeech를 ON_DEVICE 모드로 쓴다(차량 엔진, registerApp·Client Secret 불필요).
 * Pleos 음성 서비스(CAAS)가 없는 기기(일반 에뮬레이터)면 안드로이드 기본 TTS로 대신한다.
 * 엔진은 처음 말할 때 한 번만 준비하고, 앱이 살아 있는 동안 재사용한다.
 */
class WelcomeVoice(private val context: Context) {

    private val lock = Mutex()
    private var pleos: TextToSpeech? = null
    private var pleosUnavailable = false
    private var platform: AndroidTts? = null

    /** 말하기를 요청하고 어느 엔진을 썼는지 돌려준다. 둘 다 없으면 예외. 재생 완료는 기다리지 않는다. */
    suspend fun speak(text: String): String = lock.withLock {
        val engine = pleos ?: if (pleosUnavailable) null else preparePleos()
        if (engine != null) {
            // 첫 음절이 잘리지 않게 오디오 경로가 열릴 틈을 둔다
            delay(PREROLL_MS)
            engine.speak(text, true)
            return@withLock "Pleos TTS"
        }
        val fallback = platform ?: preparePlatform() ?: error("쓸 수 있는 TTS 엔진이 없습니다")
        fallback.speak(text, AndroidTts.QUEUE_FLUSH, null, UTTERANCE_ID)
        "안드로이드 기본 TTS"
    }

    fun release() {
        pleos?.release()
        platform?.shutdown()
        pleos = null
        platform = null
    }

    private suspend fun preparePleos(): TextToSpeech? {
        val tts = runCatching { TextToSpeech(context, Mode.ON_DEVICE) }
            .onFailure { Log.w(TAG, "Pleos TTS 생성 실패: ${it.message}") }
            .getOrNull()
        val ready = tts != null && withTimeoutOrNull(INIT_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                tts.initialize(object : OnInitializeListener {
                    override fun onSuccess() {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onFailure(errorType: ErrorType) {
                        Log.w(TAG, "Pleos TTS 초기화 실패: $errorType")
                        if (cont.isActive) cont.resume(false)
                    }
                })
            }
        } == true
        if (!ready) {
            tts?.release()
            pleosUnavailable = true
            return null
        }
        tts.setLocale(Locale.KOREAN)
        tts.addEventListener(LogListener)
        pleos = tts
        return tts
    }

    private suspend fun preparePlatform(): AndroidTts? = withTimeoutOrNull(INIT_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            lateinit var tts: AndroidTts
            tts = AndroidTts(context) { status ->
                if (!cont.isActive) return@AndroidTts
                if (status == AndroidTts.SUCCESS) {
                    tts.language = Locale.KOREAN
                    cont.resume(tts)
                } else {
                    tts.shutdown()
                    cont.resume(null)
                }
            }
        }
    }?.also { platform = it }

    private object LogListener : EventListener {
        override fun onStart() = Unit
        override fun onReady() = Unit
        override fun onDone() = Unit
        override fun onStop() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onError(errorType: ErrorType) {
            Log.w(TAG, "Pleos TTS 재생 오류: $errorType")
        }
    }

    private companion object {
        const val TAG = "WelcomeVoice"
        const val PREROLL_MS = 200L
        const val INIT_TIMEOUT_MS = 5_000L
        const val UTTERANCE_ID = "welcome"
    }
}
