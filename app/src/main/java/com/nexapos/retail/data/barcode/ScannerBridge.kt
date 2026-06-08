package com.nexapos.retail.data.barcode

import android.view.KeyEvent
import com.nexapos.retail.data.profile.ScannerInput

/**
 * Adapts hardware [KeyEvent]s into the pure [BarcodeAssembler] and decides what to
 * swallow. Held by MainActivity; [emit] publishes a completed barcode, [terminator]
 * supplies the user's configured terminator key(s).
 *
 * Returns true only to SWALLOW an event from the rest of the app:
 *  - machine-fast burst characters (so they don't leak into a focused field),
 *  - the terminator (down + up) of a recognised scan.
 * Everything else returns false and reaches Compose / text fields unchanged.
 */
class ScannerBridge(
    private val emit: (String) -> Unit,
    private val terminator: () -> ScannerInput.Terminator,
) {
    private val assembler = BarcodeAssembler()
    private var swallowNextUp = false

    fun feed(event: KeyEvent): Boolean =
        when (event.action) {
            KeyEvent.ACTION_UP -> {
                val swallow = swallowNextUp && isTerminator(event.keyCode)
                if (swallow) swallowNextUp = false
                swallow
            }
            KeyEvent.ACTION_DOWN ->
                if (isTerminator(event.keyCode)) {
                    val code = assembler.finish(event.eventTime)
                    if (code != null) {
                        emit(code)
                        swallowNextUp = true
                        true
                    } else {
                        false
                    }
                } else {
                    val ch = event.unicodeChar
                    if (ch == 0) false else assembler.feed(ch.toChar(), event.eventTime)
                }
            else -> false
        }

    private fun isTerminator(keyCode: Int): Boolean {
        val enter = keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
        val tab = keyCode == KeyEvent.KEYCODE_TAB
        return when (terminator()) {
            ScannerInput.Terminator.ENTER -> enter
            ScannerInput.Terminator.TAB -> tab
            ScannerInput.Terminator.BOTH -> enter || tab
        }
    }
}
