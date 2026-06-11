package com.nexapos.retail.data.hardware.labels

import com.nexapos.retail.domain.hardware.LabelSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TsplTest {
    private val size = LabelSize(widthMm = 40, heightMm = 30, gapMm = 2)

    private fun lines(spec: LabelSpec) = Tspl.labelText(spec, size).trim().split("\r\n")

    @Test
    fun `header sets size, gap, direction, codepage then clears`() {
        val l = lines(LabelSpec("Hammer", "HMR-16", "5901234123457"))
        assertEquals("SIZE 40 mm,30 mm", l[0])
        assertEquals("GAP 2 mm,0 mm", l[1])
        assertEquals("DIRECTION 1", l[2])
        assertEquals("CODEPAGE 1252", l[3])
        assertEquals("CLS", l[4])
    }

    @Test
    fun `valid ean13 prints as EAN13 with 12-digit payload`() {
        val barcode = lines(LabelSpec("Hammer", "HMR-16", "5901234123457")).first { it.startsWith("BARCODE") }
        assertTrue(barcode.contains("\"EAN13\""))
        assertTrue(barcode.endsWith("\"590123412345\"")) // printer adds the check digit
    }

    @Test
    fun `non-ean content falls back to code128 with the full payload`() {
        val barcode = lines(LabelSpec("Bolt", "B-1", "HMR-16")).first { it.startsWith("BARCODE") }
        assertTrue(barcode.contains("\"128\""))
        assertTrue(barcode.endsWith("\"HMR-16\""))
    }

    @Test
    fun `copies map to PRINT and zero is coerced to one`() {
        assertEquals("PRINT 1,4", lines(LabelSpec("X", "", "123", copies = 4)).last())
        assertEquals("PRINT 1,1", lines(LabelSpec("X", "", "123", copies = 0)).last())
    }

    @Test
    fun `quotes and newlines are sanitised out of content`() {
        val all = Tspl.labelText(LabelSpec("Drill \"Pro\"\nMax", "S\"1", "123"), size)
        assertFalse(all.contains("Drill \"Pro\""))
        assertTrue(all.contains("Drill 'Pro' Max"))
        assertTrue(all.contains("\"S'1\""))
    }

    @Test
    fun `long name wraps to two lines and the tail is ellipsised`() {
        val spec = LabelSpec("Extra Heavy Duty Galvanised Steel Wood Screws Box of Five Hundred", "", "123")
        val texts = lines(spec).filter { it.startsWith("TEXT") }
        assertEquals(2, texts.size)
        assertTrue(texts[1].contains("…"))
    }

    @Test
    fun `short name is a single centred line and blank sku is omitted`() {
        val texts = lines(LabelSpec("Hammer", "", "5901234123457")).filter { it.startsWith("TEXT") }
        assertEquals(1, texts.size)
        assertTrue(texts[0].contains("\"Hammer\""))
    }

    @Test
    fun `price line appears only when priceCents is set`() {
        val with = lines(LabelSpec("Hammer", "", "123", priceCents = 12_550))
        assertTrue(with.any { it.startsWith("TEXT") && it.contains("Rs 125.50") })
        val without = lines(LabelSpec("Hammer", "", "123"))
        assertFalse(without.any { it.contains("Rs ") })
    }

    @Test
    fun `wide code128 drops narrow bar width to fit the label`() {
        // 15 chars ⇒ 200 modules ⇒ 400 dots at narrow 2 > 304 usable ⇒ narrow 1.
        val barcode = lines(LabelSpec("X", "", "ABCDEFGHIJKLMNO")).first { it.startsWith("BARCODE") }
        assertTrue(barcode.contains(",1,1,\"ABCDEFGHIJKLMNO\""))
        // 6 chars fits comfortably at narrow 2.
        val short = lines(LabelSpec("X", "", "ABC-12")).first { it.startsWith("BARCODE") }
        assertTrue(short.contains(",2,2,\"ABC-12\""))
    }

    @Test
    fun `test label is a valid in-store ean13 job`() {
        val text = Tspl.testLabel(size).toString(charset("windows-1252"))
        assertTrue(text.contains("\"EAN13\""))
        assertTrue(text.contains("NexaPOS test label"))
        assertTrue(text.trim().endsWith("PRINT 1,1"))
    }
}
