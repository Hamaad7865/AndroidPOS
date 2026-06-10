package com.nexapos.retail.data.hardware.drawer

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class EscPosTest {
    @Test
    fun `pin 2 default pulse is the canonical ESC p sequence`() {
        assertArrayEquals(
            byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()),
            EscPos.drawerPulse(DrawerPin.PIN_2),
        )
    }

    @Test
    fun `pin 5 flips the connector byte only`() {
        assertArrayEquals(
            byteArrayOf(0x1B, 0x70, 0x01, 0x19, 0xFA.toByte()),
            EscPos.drawerPulse(DrawerPin.PIN_5),
        )
    }

    @Test
    fun `pulse durations are clamped to one byte`() {
        val pulse = EscPos.drawerPulse(DrawerPin.PIN_2, onUnits = 9_999, offUnits = -5)
        assertArrayEquals(
            byteArrayOf(0x1B, 0x70, 0x00, 0xFF.toByte(), 0x00),
            pulse,
        )
    }

    @Test
    fun `unknown pin id falls back to pin 2`() {
        org.junit.Assert.assertEquals(DrawerPin.PIN_2, DrawerPin.from("garbage"))
        org.junit.Assert.assertEquals(DrawerPin.PIN_5, DrawerPin.from("PIN_5"))
    }
}
