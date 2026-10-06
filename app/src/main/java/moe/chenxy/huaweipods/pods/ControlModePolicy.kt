package moe.chenxy.huaweipods.pods

import moe.chenxy.huaweipods.config.ConfigManager

/**
 * 控制方式开关的纯逻辑策略。
 *
 * HOOK 模式：沿用现有 LSPosed 链路，App 只收发广播，真正的 RFCOMM 建连在
 * `com.android.bluetooth` / `com.xiaomi.bluetooth` 被 Hook 进程内完成。
 * DIRECT 模式：App 进程直接用标准 SPP UUID 建连（最小可用：电量 + 降噪控制），
 * Hook 端必须让出控制通道，避免双方同时持有 RFCOMM。
 */
object ControlModePolicy {
    fun isDirectMode(controlMode: Int): Boolean =
        controlMode == ConfigManager.CONTROL_MODE_DIRECT

    fun isHookMode(controlMode: Int): Boolean = !isDirectMode(controlMode)

    /** Hook 端是否应该处理本次连接/写入；直连模式下必须让出。 */
    fun shouldHookHandleConnection(controlMode: Int): Boolean = isHookMode(controlMode)

    /** App 端是否应该走本地直连而不是发广播给系统蓝牙进程。 */
    fun shouldAppUseDirectTransport(controlMode: Int): Boolean = isDirectMode(controlMode)

    fun normalized(controlMode: Int): Int = with(ConfigManager) {
        controlMode.normalizedControlMode()
    }
}
