package moe.chenxy.huaweipods.pods

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import moe.chenxy.huaweipods.config.ConfigManager
import moe.chenxy.huaweipods.utils.miuiStrongToast.data.BatteryParams

/**
 * App 进程直连管理器（最小可用档）。
 *
 * 只复用已经过实机验证的包构造器与 [HuaweiL2capAncController] 标准 SPP 传输，
 * 不复制 [HuaweiHfpController] 的系统通知/超级岛/融合中心逻辑。
 * 调用方必须已经持有 `BLUETOOTH_CONNECT` 运行时权限。
 */
@SuppressLint("MissingPermission")
object DirectPodConnectionManager {
    @Volatile
    private var activeAddress: String? = null

    @Volatile
    private var activeRoute: HuaweiDeviceRoute = HuaweiDeviceRoute.UNSUPPORTED

    fun canHandleRoute(route: HuaweiDeviceRoute): Boolean = route.isSupported

    fun isActiveFor(address: String?): Boolean =
        !address.isNullOrBlank() && activeAddress?.equals(address, ignoreCase = true) == true

    fun activeAddress(): String? = activeAddress

    fun activeRoute(): HuaweiDeviceRoute = activeRoute

    fun buildBatteryQuery(route: HuaweiDeviceRoute): ByteArray? =
        HuaweiAncPackets.batteryQuery(route)

    fun buildAncPacket(
        route: HuaweiDeviceRoute,
        mode: NoiseControlMode,
        subMode: Int? = null,
    ): ByteArray? = HuaweiAncPackets.mode(route, mode, subMode)

    fun connect(
        context: Context,
        device: BluetoothDevice,
        route: HuaweiDeviceRoute,
        onBattery: (BatteryParams) -> Unit = {},
        onComplete: ((Boolean) -> Unit)? = null,
    ) {
        if (!canHandleRoute(route)) {
            onComplete?.invoke(false)
            return
        }
        activeAddress = runCatching { device.address }.getOrNull()
        activeRoute = route
        // 先查一次电量作为连通性验证；失败不代表设备不可用，调用方可再手动刷新。
        HuaweiL2capAncController.requestBattery(
            context = context.applicationContext ?: context,
            device = device,
            route = route,
            onBattery = onBattery,
            onComplete = onComplete,
        )
    }

    fun refreshBattery(
        context: Context,
        device: BluetoothDevice,
        route: HuaweiDeviceRoute,
        onBattery: (BatteryParams) -> Unit,
        onComplete: ((Boolean) -> Unit)? = null,
    ) {
        if (!canHandleRoute(route)) {
            onComplete?.invoke(false)
            return
        }
        HuaweiL2capAncController.requestBattery(
            context = context.applicationContext ?: context,
            device = device,
            route = route,
            onBattery = onBattery,
            onComplete = onComplete,
        )
    }

    fun setAncMode(
        context: Context,
        device: BluetoothDevice,
        route: HuaweiDeviceRoute,
        mode: NoiseControlMode,
        subMode: Int? = null,
        onComplete: ((Boolean) -> Unit)? = null,
    ) {
        if (!canHandleRoute(route) || !route.supportsAnc) {
            onComplete?.invoke(false)
            return
        }
        HuaweiL2capAncController.setAncMode(
            context = context.applicationContext ?: context,
            device = device,
            route = route,
            mode = mode,
            subMode = subMode,
            onComplete = onComplete,
        )
    }

    fun setAncLevel(
        context: Context,
        device: BluetoothDevice,
        route: HuaweiDeviceRoute,
        level: Int,
    ) {
        if (!canHandleRoute(route)) return
        HuaweiL2capAncController.setAncLevel(
            context = context.applicationContext ?: context,
            device = device,
            route = route,
            level = level,
        )
    }

    fun disconnect(device: BluetoothDevice? = null) {
        activeAddress = null
        activeRoute = HuaweiDeviceRoute.UNSUPPORTED
        HuaweiL2capAncController.disconnect(device)
    }

    /** 按地址从已配对设备中解析 [BluetoothDevice]，直连模式下 UI 分发写入时使用。 */
    fun findBondedDevice(context: Context, address: String?): BluetoothDevice? {
        if (address.isNullOrBlank()) return null
        val manager = context.getSystemService(BluetoothManager::class.java) ?: return null
        val adapter = runCatching { manager.adapter }.getOrNull() ?: return null
        return runCatching {
            adapter.getRemoteDevice(address)
        }.getOrNull()
    }

    /** 当前配置是否为直连模式；Hook 与 UI 共用同一开关语义。 */
    fun isDirectMode(): Boolean = ConfigManager.isDirectControlMode()
}
