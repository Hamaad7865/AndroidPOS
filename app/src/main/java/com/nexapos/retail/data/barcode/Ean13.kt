package com.nexapos.retail.data.barcode

import kotlin.random.Random

/**
 * EAN-13 utilities. Generated codes use the **200 prefix** (reserved for
 * restricted-circulation / in-store use), which is the standard practice when
 * a shop creates its own barcodes for items that don't come with a
 * manufacturer-issued GTIN. The check digit is computed per the EAN-13 spec,
 * so the resulting 13-digit string is readable by any retail laser scanner.
 *
 * Encoding tables (L-odd / G-even / R) and parity patterns follow the public
 * EAN-13 specification — anyone can render or read the result with off-the-
 * shelf hardware/software.
 */
object Ean13 {
    private const val IN_STORE_PREFIX = "200"

    private val LEFT_ODD =
        listOf(
            "0001101", "0011001", "0010011", "0111101", "0100011",
            "0110001", "0101111", "0111011", "0110111", "0001011",
        )
    private val LEFT_EVEN =
        listOf(
            "0100111", "0110011", "0011011", "0100001", "0011101",
            "0111001", "0000101", "0010001", "0001001", "0010111",
        )
    private val RIGHT =
        listOf(
            "1110010", "1100110", "1101100", "1000010", "1011100",
            "1001110", "1010000", "1000100", "1001000", "1110100",
        )
    private val PARITY =
        listOf(
            "LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG",
            "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL",
        )

    /** Generates a fresh EAN-13: "200" + 9 random digits + computed check digit. */
    fun next(): String {
        val random = (1..9).joinToString("") { Random.nextInt(0, 10).toString() }
        val body = IN_STORE_PREFIX + random
        return body + checkDigit(body)
    }

    /** Computes the check digit (0–9) for a 12-digit EAN body. */
    fun checkDigit(body12: String): Int {
        require(body12.length == 12 && body12.all { it.isDigit() }) { "expects 12 digits" }
        var sum = 0
        body12.forEachIndexed { i, ch ->
            val n = ch.digitToInt()
            sum += if (i % 2 == 0) n else n * 3
        }
        return (10 - sum % 10) % 10
    }

    /** True if [value] is exactly 13 digits with a valid EAN-13 check digit. */
    fun isValid(value: String): Boolean {
        if (value.length != 13 || !value.all { it.isDigit() }) return false
        return checkDigit(value.substring(0, 12)) == value.last().digitToInt()
    }

    /**
     * Encodes a valid EAN-13 string into the 95-bit bar pattern:
     * 3 guard + 42 left + 5 center + 42 right + 3 guard. '1' = bar, '0' = space.
     */
    fun encode(value: String): String {
        require(isValid(value)) { "not a valid EAN-13: $value" }
        val numberSystem = value[0].digitToInt()
        val left = value.substring(1, 7)
        val right = value.substring(7, 13)
        val parity = PARITY[numberSystem]
        val sb = StringBuilder()
        sb.append("101")
        left.forEachIndexed { i, ch ->
            val table = if (parity[i] == 'L') LEFT_ODD else LEFT_EVEN
            sb.append(table[ch.digitToInt()])
        }
        sb.append("01010")
        right.forEach { ch -> sb.append(RIGHT[ch.digitToInt()]) }
        sb.append("101")
        return sb.toString()
    }
}
