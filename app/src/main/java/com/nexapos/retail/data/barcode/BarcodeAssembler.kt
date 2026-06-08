package com.nexapos.retail.data.barcode

/**
 * Pure assembler that turns a stream of typed characters + a terminator into a
 * barcode, distinguishing a hardware scanner's fast burst from human typing.
 *
 * - [feed] appends a character. A gap longer than [resetGapMs] since the previous
 *   character resets the buffer, so slow human typing never accumulates into a
 *   "scan". It returns true when the character should be SWALLOWED (hidden from a
 *   focused field) — only for machine-fast chars (gap <= [swallowGapMs]) after the
 *   first — so normal typing is never eaten.
 * - [finish] is called on a terminator key; it returns the code iff the buffer was
 *   built as a fast burst of at least [minLen] characters, then resets.
 *
 * No Android types → fully unit-testable.
 */
class BarcodeAssembler(
    private val resetGapMs: Long = RESET_GAP_MS,
    private val swallowGapMs: Long = SWALLOW_GAP_MS,
    private val minLen: Int = MIN_LEN,
) {
    private val buffer = StringBuilder()
    private var lastMs = 0L

    fun feed(
        ch: Char,
        atMs: Long,
    ): Boolean {
        val gap = atMs - lastMs
        if (gap > resetGapMs) buffer.setLength(0)
        buffer.append(ch)
        lastMs = atMs
        return buffer.length > 1 && gap in 0..swallowGapMs
    }

    fun finish(atMs: Long): String? {
        val fast = atMs - lastMs <= resetGapMs
        val code = buffer.toString()
        buffer.setLength(0)
        lastMs = 0L
        return code.takeIf { fast && it.length >= minLen }
    }

    companion object {
        const val RESET_GAP_MS = 100L
        const val SWALLOW_GAP_MS = 40L
        const val MIN_LEN = 3
    }
}
