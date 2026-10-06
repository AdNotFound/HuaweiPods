package moe.chenxy.huaweipods.pods

import moe.chenxy.huaweipods.config.ConfigManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectControlModeTest {
    @Test
    fun `policy routes hook and direct without overlap`() {
        assertTrue(ControlModePolicy.shouldHookHandleConnection(ConfigManager.CONTROL_MODE_HOOK))
        assertFalse(ControlModePolicy.shouldHookHandleConnection(ConfigManager.CONTROL_MODE_DIRECT))
        assertTrue(ControlModePolicy.shouldAppUseDirectTransport(ConfigManager.CONTROL_MODE_DIRECT))
        assertFalse(ControlModePolicy.shouldAppUseDirectTransport(ConfigManager.CONTROL_MODE_HOOK))
    }

    @Test
    fun `unknown control mode falls back to hook`() {
        assertTrue(ControlModePolicy.shouldHookHandleConnection(99))
        assertFalse(ControlModePolicy.shouldAppUseDirectTransport(99))
        assertTrue(ControlModePolicy.isHookMode(99))
    }

    @Test
    fun `direct manager rejects unsupported route`() {
        assertFalse(DirectPodConnectionManager.canHandleRoute(HuaweiDeviceRoute.UNSUPPORTED))
        assertNull(DirectPodConnectionManager.buildBatteryQuery(HuaweiDeviceRoute.UNSUPPORTED))
        assertNull(
            DirectPodConnectionManager.buildAncPacket(
                HuaweiDeviceRoute.UNSUPPORTED,
                NoiseControlMode.NOISE_CANCELLATION,
            ),
        )
    }

    @Test
    fun `direct manager builds packets for supported route`() {
        assertTrue(DirectPodConnectionManager.canHandleRoute(HuaweiDeviceRoute.HUAWEI_FREEBUDS6I))
        assertNotNull(DirectPodConnectionManager.buildBatteryQuery(HuaweiDeviceRoute.HUAWEI_FREEBUDS6I))
        assertNotNull(
            DirectPodConnectionManager.buildAncPacket(
                HuaweiDeviceRoute.HUAWEI_FREEBUDS6I,
                NoiseControlMode.OFF,
            ),
        )
        assertNull(
            DirectPodConnectionManager.buildAncPacket(
                HuaweiDeviceRoute.HUAWEI_FREEBUDS6I,
                NoiseControlMode.UNKNOWN,
            ),
        )
    }
}
