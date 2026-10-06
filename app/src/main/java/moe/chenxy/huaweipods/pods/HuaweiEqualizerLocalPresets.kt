package moe.chenxy.huaweipods.pods

import android.content.SharedPreferences
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * App 本地保存的命名均衡器预设（按耳机地址隔离）。
 *
 * 只解决“命名保存 / 列表选择 / 删除”这一层：写入耳机仍走原有通道
 * （通用路由经 [HuaweiEqualizerController.setCustom]，FreeClip2 经 SmartAudio 桥）。
 * 持久化用纯文本行格式，避免引入新的序列化依赖，保证单元可测。
 */
object HuaweiEqualizerLocalPresets {
    const val MAX_PRESETS = 20

    private const val KEY_SUFFIX = "eq_local_presets"

    fun prefsKey(address: String): String =
        "huawei_${address.uppercase().ifBlank { "unknown" }}_$KEY_SUFFIX"

    fun normalizeName(value: String): String? =
        HuaweiEqualizerPresetPolicy.normalizeName(value)

    fun isValidGains(gains: List<Int>): Boolean =
        gains.size == HuaweiEqualizerCodec.BAND_COUNT &&
            gains.all { it in HuaweiEqualizerCodec.GAIN_RANGE }

    /**
     * 同名覆盖（大小写敏感），超过上限时保留最近写入的 [MAX_PRESETS] 个。
     * 名字或增益非法时返回原列表。
     */
    fun upsert(
        presets: List<LocalEqualizerPreset>,
        name: String,
        gains: List<Int>,
    ): List<LocalEqualizerPreset> {
        val normalized = normalizeName(name) ?: return presets
        if (!isValidGains(gains)) return presets
        return ((presets.filterNot { it.name == normalized }) +
            LocalEqualizerPreset(name = normalized, gains = gains.toList()))
            .takeLast(MAX_PRESETS)
    }

    fun remove(presets: List<LocalEqualizerPreset>, name: String): List<LocalEqualizerPreset> =
        presets.filterNot { it.name == name }

    fun encode(presets: List<LocalEqualizerPreset>): String = presets.joinToString("\n") { preset ->
        val encodedName = URLEncoder.encode(preset.name, StandardCharsets.UTF_8.name())
        "$encodedName:${preset.gains.joinToString(",")}"
    }

    fun decode(raw: String?): List<LocalEqualizerPreset> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val separator = line.indexOf(':')
            if (separator <= 0) return@mapNotNull null
            val name = runCatching {
                URLDecoder.decode(line.substring(0, separator), StandardCharsets.UTF_8.name())
            }.getOrNull()?.trim().orEmpty()
            val gains = line.substring(separator + 1).split(',')
                .map { it.toIntOrNull() ?: return@mapNotNull null }
            if (normalizeName(name) == null || !isValidGains(gains)) return@mapNotNull null
            LocalEqualizerPreset(name = name, gains = gains)
        }.toList().takeLast(MAX_PRESETS)
    }

    fun load(prefs: SharedPreferences, address: String): List<LocalEqualizerPreset> =
        decode(prefs.getString(prefsKey(address), null))

    fun save(prefs: SharedPreferences, address: String, presets: List<LocalEqualizerPreset>) {
        prefs.edit().putString(prefsKey(address), encode(presets)).apply()
    }
}

data class LocalEqualizerPreset(
    val name: String,
    val gains: List<Int>,
)
