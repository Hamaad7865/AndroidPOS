package com.nexapos.retail.data.barcode

/**
 * Pure assembler that turns a stream of typed characters + a terminator into a
 * barcode, distinguishing a hardware scanner's fast burst from human typing.
 *
 * It NEVER swallows characters — every keystroke still reaches the focused field,
 * so normal typing and key auto-repeat are never broken. Detection is purely
 * retrospective: [feed] buffers characters, resetting the buffer whenever the gap
 * since the previous character exceeds [resetGapMs] (so human-speed typing never
 * accumulates); [finish], called on a terminator key, returns the code only if the
 * buffer was built as one fast burst of at least [minLen] characters.
 *
 * No Android types → fully unit-testable.
 */
class BarcodeAssembler(
    private val resetGapMs: Long = RESET_GAP_MS,
    private val minLen: Int = MIN_LEN,
) {
    private val buffer = StringBuilder()
    private var lastMs = 0L

    fun feed(
        ch: Char,
        atMs: Long,
    ) {
        if (atMs - lastMs > resetGapMs) buffer.setLength(0)
        buffer.append(ch)
        lastMs = atMs
    }

    fun finish(atMs: Long): String? {
        val fast = atMs - lastMs <= resetGapMs
        val code = buffer.toString()
        buffer.setLength(0)
        lastMs = 0L
        return code.takeIf { fast && it.length >= minLen }
    }

    companion object {
        // A scanner emits a whole barcode with only a few ms between keystrokes; a
        // human types far slower. 50ms cleanly separates the two: human keystrokes
        // exceed it (so the buffer resets every key and never forms a "scan"),
        // while a scanner's keystrokes fall under it and accumulate.
        const val RESET_GAP_MS = 50L
        const val MIN_LEN = 3
    }
}
