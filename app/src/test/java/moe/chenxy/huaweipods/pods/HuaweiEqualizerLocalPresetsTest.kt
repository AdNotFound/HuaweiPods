package moe.chenxy.huaweipods.pods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HuaweiEqualizerLocalPresetsTest {
    private val flat = List(HuaweiEqualizerCodec.BAND_COUNT) { 0 }
    private val vShape = listOf(6, 4, 2, 0, -2, -2, 0, 2, 4, 6)

    @Test
    fun `blank or oversized names are rejected`() {
        assertEquals(null, HuaweiEqualizerLocalPresets.normalizeName("   "))
        assertEquals(null, HuaweiEqualizerLocalPresets.normalizeName("a".repeat(33)))
        assertEquals("bass", HuaweiEqualizerLocalPresets.normalizeName("  bass  "))
    }

    @Test
    fun `gains must be ten in-range values`() {
        assertTrue(HuaweiEqualizerLocalPresets.isValidGains(flat))
        assertFalse(HuaweiEqualizerLocalPresets.isValidGains(listOf(0, 0, 0)))
        assertFalse(HuaweiEqualizerLocalPresets.isValidGains(List(10) { 61 }))
    }

    @Test
    fun `upsert overwrites same name and rejects invalid input`() {
        val saved = HuaweiEqualizerLocalPresets.upsert(emptyList(), "bass", flat)
        assertEquals(1, saved.size)
        val overwritten = HuaweiEqualizerLocalPresets.upsert(saved, "bass", vShape)
        assertEquals(listOf("bass" to vShape), overwritten.map { it.name to it.gains })
        assertEquals(saved, HuaweiEqualizerLocalPresets.upsert(saved, "   ", vShape))
        assertEquals(saved, HuaweiEqualizerLocalPresets.upsert(saved, "rock", listOf(1, 2, 3)))
    }

    @Test
    fun `remove drops only the named preset`() {
        val presets = HuaweiEqualizerLocalPresets.upsert(
            HuaweiEqualizerLocalPresets.upsert(emptyList(), "a", flat),
            "b",
            vShape,
        )
        assertEquals(listOf("b"), HuaweiEqualizerLocalPresets.remove(presets, "a").map { it.name })
        assertEquals(2, HuaweiEqualizerLocalPresets.remove(presets, "missing").size)
    }

    @Test
    fun `encode and decode round-trip names with special characters`() {
        val presets = listOf(
            LocalEqualizerPreset(name = "my bass:低音 🎧", gains = vShape),
            LocalEqualizerPreset(name = "flat", gains = flat),
        )
        assertEquals(presets, HuaweiEqualizerLocalPresets.decode(HuaweiEqualizerLocalPresets.encode(presets)))
    }

    @Test
    fun `decode skips corrupt lines`() {
        val raw = HuaweiEqualizerLocalPresets.encode(listOf(LocalEqualizerPreset("ok", flat))) +
            "\nbroken-line\nbad:1,2,3"
        val decoded = HuaweiEqualizerLocalPresets.decode(raw)
        assertEquals(listOf("ok"), decoded.map { it.name })
    }

    @Test
    fun `preset count is capped`() {
        var presets = emptyList<LocalEqualizerPreset>()
        repeat(HuaweiEqualizerLocalPresets.MAX_PRESETS + 5) { index ->
            presets = HuaweiEqualizerLocalPresets.upsert(presets, "p$index", flat)
        }
        assertEquals(HuaweiEqualizerLocalPresets.MAX_PRESETS, presets.size)
        assertEquals("p5", presets.first().name)
    }
}
