package com.pleos.fe.api.vehicle

/**
 * 테마의 앰비언트 색(#RRGGBB)을 Vehicle SDK setAmbientLightBrightnessColor가 받는 32비트 ARGB로 바꾼다(알파 0xFF 고정).
 */
object AmbientColor {

    /**
     * Pleos 에뮬레이터 Virtual Console이 보여주는 앰비언트 색. 실차는 임의 RGB를 받는지 확인되지 않아,
     * 원래 색 설정이 실패하면 이 중 가장 가까운 색으로 다시 시도한다(정확한 RGB 값은 문서에 없어 근사치).
     */
    val EMULATOR_PALETTE = linkedMapOf(
        "Cyan" to "#00FFFF",
        "Blue" to "#0000FF",
        "Purple" to "#8000FF",
        "White" to "#FFFFFF",
    )

    fun argb(hex: String): Int {
        val rgb = hex.removePrefix("#").toLong(16).toInt() and 0xFFFFFF
        return (0xFF shl 24) or rgb
    }

    /**
     * 가장 가까운 팔레트 색 이름. 조명은 RGB 거리보다 색상(hue)이 체감에 가까워서,
     * 채도가 낮은 색은 무채색 후보(White)로, 나머지는 색상 각도가 가장 가까운 유채색으로 고른다.
     */
    fun nearest(hex: String, palette: Map<String, String> = EMULATOR_PALETTE): String {
        val (hue, saturation) = hueSaturation(hex)
        val (neutral, chromatic) = palette.entries.partition { hueSaturation(it.value).second < NEUTRAL_SATURATION }
        val pool = if (saturation < NEUTRAL_SATURATION && neutral.isNotEmpty()) neutral else chromatic.ifEmpty { neutral }
        return pool.minBy { hueDistance(hue, hueSaturation(it.value).first) }.key
    }

    private const val NEUTRAL_SATURATION = 0.25

    private fun hueDistance(a: Double, b: Double): Double {
        val d = kotlin.math.abs(a - b) % 360.0
        return if (d > 180.0) 360.0 - d else d
    }

    /** HSV의 색상(0–360)과 채도(0–1). */
    private fun hueSaturation(hex: String): Pair<Double, Double> {
        val v = hex.removePrefix("#").toInt(16)
        val r = (v shr 16 and 0xFF) / 255.0
        val g = (v shr 8 and 0xFF) / 255.0
        val b = (v and 0xFF) / 255.0
        val max = maxOf(r, g, b)
        val delta = max - minOf(r, g, b)
        val saturation = if (max == 0.0) 0.0 else delta / max
        val hue = when {
            delta == 0.0 -> 0.0
            max == r -> 60.0 * (((g - b) / delta) % 6.0)
            max == g -> 60.0 * ((b - r) / delta + 2.0)
            else -> 60.0 * ((r - g) / delta + 4.0)
        }
        return (if (hue < 0) hue + 360.0 else hue) to saturation
    }
}
