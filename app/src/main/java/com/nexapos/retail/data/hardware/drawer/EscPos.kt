package com.nexapos.retail.data.hardware.drawer

/**
 * Which connector pin of the printer's drawer (RJ11) port carries the pulse.
 * Pin 2 is the de-facto default; some drawers/printers are wired to pin 5.
 */
enum class DrawerPin(val code: Byte, val label: String) {
    PIN_2(0x00, "Pin 2 (default)"),
    PIN_5(0x01, "Pin 5"),
    ;

    companion object {
        fun from(id: String?): DrawerPin = entries.firstOrNull { it.name == id } ?: PIN_2
    }
}

/** Pure ESC/POS byte builders — no Android imports so they unit-test on the JVM. */
object EscPos {
    private const val MAX_UNIT = 255
    private const val DEFAULT_ON_UNITS = 25 // × 2 ms = 50 ms pulse
    private const val DEFAULT_OFF_UNITS = 250 // × 2 ms = 500 ms recovery

    /**
     * `ESC p m t1 t2` — drawer kick-out pulse. Times are in 2 ms units.
     * Defaults produce `1B 70 00 19 FA` for [DrawerPin.PIN_2].
     */
    fun drawerPulse(
        pin: DrawerPin,
        onUnits: Int = DEFAULT_ON_UNITS,
        offUnits: Int = DEFAULT_OFF_UNITS,
    ): ByteArray =
        byteArrayOf(
            0x1B,
            0x70,
            pin.code,
            onUnits.coerceIn(0, MAX_UNIT).toByte(),
            offUnits.coerceIn(0, MAX_UNIT).toByte(),
        )
}
