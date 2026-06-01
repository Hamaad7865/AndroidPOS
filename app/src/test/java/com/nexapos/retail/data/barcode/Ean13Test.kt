package com.nexapos.retail.data.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ean13Test {
    // --- isValid ---

    @Test
    fun `isValid returns true for a known valid EAN-13`() {
        // "4006381333931" is a well-known public EAN-13 (Stabilo Boss)
        assertTrue(Ean13.isValid("4006381333931"))
    }

    @Test
    fun `isValid returns false when check digit is wrong`() {
        // Last digit mutated: 1 -> 2
        assertFalse(Ean13.isValid("4006381333932"))
    }

    @Test
    fun `isValid returns false for a 12-digit string`() {
        assertFalse(Ean13.isValid("400638133393"))
    }

    @Test
    fun `isValid returns false for a 14-digit string`() {
        assertFalse(Ean13.isValid("40063813339310"))
    }

    @Test
    fun `isValid returns false for a non-digit string`() {
        assertFalse(Ean13.isValid("400638133393X"))
    }

    @Test
    fun `isValid returns false for an empty string`() {
        assertFalse(Ean13.isValid(""))
    }

    // --- checkDigit ---

    @Test
    fun `checkDigit matches known example`() {
        // Body of "4006381333931" without the trailing check digit
        assertEquals(1, Ean13.checkDigit("400638133393"))
    }

    @Test
    fun `checkDigit returns 0 when weighted sum is a multiple of 10`() {
        // "000000000000" -> all zeros -> sum=0 -> check digit = (10 - 0%10)%10 = 0
        assertEquals(0, Ean13.checkDigit("000000000000"))
    }

    @Test
    fun `checkDigit is consistent with isValid for in-store prefix`() {
        // A 12-digit in-store body; appending the computed check digit must be valid
        val body = "200123456789"
        val full = body + Ean13.checkDigit(body)
        assertTrue(Ean13.isValid(full))
    }

    // --- next ---

    @Test
    fun `next returns a 13-digit string`() {
        assertEquals(13, Ean13.next().length)
    }

    @Test
    fun `next returns a valid EAN-13`() {
        // Several calls must all be valid (check digit is correct)
        repeat(20) {
            assertTrue("next() must produce a valid EAN-13", Ean13.isValid(Ean13.next()))
        }
    }

    @Test
    fun `next uses the in-store 200 prefix`() {
        repeat(10) {
            assertTrue(Ean13.next().startsWith("200"))
        }
    }

    // --- encode ---

    @Test
    fun `encode produces 95-bit pattern for a known barcode`() {
        val bits = Ean13.encode("4006381333931")
        assertEquals(95, bits.length)
    }

    @Test
    fun `encode starts and ends with guard bars 101`() {
        val bits = Ean13.encode("4006381333931")
        assertTrue(bits.startsWith("101"))
        assertTrue(bits.endsWith("101"))
    }

    @Test
    fun `encode contains only zeros and ones`() {
        val bits = Ean13.encode("4006381333931")
        assertTrue(bits.all { it == '0' || it == '1' })
    }

    @Test
    fun `encode is deterministic`() {
        val a = Ean13.encode("4006381333931")
        val b = Ean13.encode("4006381333931")
        assertEquals(a, b)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `encode throws for an invalid EAN-13`() {
        Ean13.encode("4006381333932") // wrong check digit
    }
}
