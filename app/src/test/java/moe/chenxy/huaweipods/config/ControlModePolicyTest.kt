package moe.chenxy.huaweipods.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlModePolicyTest {
    @Test
    fun `hook is default and unknown values fall back to hook`() {
        assertEquals(ConfigManager.CONTROL_MODE_HOOK, with(ConfigManager) { 99.normalizedControlMode() })
        assertEquals(ConfigManager.CONTROL_MODE_HOOK, with(ConfigManager) { (-1).normalizedControlMode() })
        assertEquals(ConfigManager.CONTROL_MODE_DIRECT, with(ConfigManager) { ConfigManager.CONTROL_MODE_DIRECT.normalizedControlMode() })
    }

    @Test
    fun `direct mode flag round-trips`() {
        assertTrue(with(ConfigManager) { ConfigManager.CONTROL_MODE_DIRECT.normalizedControlMode() } == ConfigManager.CONTROL_MODE_DIRECT)
        assertFalse(with(ConfigManager) { ConfigManager.CONTROL_MODE_HOOK.normalizedControlMode() } == ConfigManager.CONTROL_MODE_DIRECT)
    }
}
