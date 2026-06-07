package com.nexapos.retail.ui.purchase.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserTest {
    private fun lines(vararg text: String): List<OcrLine> =
        text.mapIndexed { i, t -> OcrLine(t, top = i * 30, left = 0, right = 400) }

    @Test
    fun `extracts qty name and unit cost from a printed line`() {
        val parsed = ReceiptParser.parse(lines("2 Hammer Claw 16oz   250.00"))
        assertEquals(1, parsed.lines.size)
        assertEquals(ReceiptDraftLine("Hammer Claw 16oz", 2, 250), parsed.lines.first())
    }

    @Test
    fun `defaults quantity to 1 when absent`() {
        val parsed = ReceiptParser.parse(lines("Cement Bag 50kg   Rs 520"))
        assertEquals(ReceiptDraftLine("Cement Bag 50kg", 1, 520), parsed.lines.first())
    }

    @Test
    fun `excludes total vat and subtotal lines`() {
        val parsed = ReceiptParser.parse(
            lines(
                "Nails 1kg 45",
                "Subtotal 45",
                "VAT 15% 6.75",
                "TOTAL 51.75",
                "Balance Due 51.75",
            ),
        )
        assertEquals(listOf("Nails 1kg"), parsed.lines.map { it.name })
    }

    @Test
    fun `guesses supplier from the top non-amount line`() {
        val parsed = ReceiptParser.parse(lines("ACME HARDWARE LTD", "Tel 5712 3456", "Bolt M8 x10  120"))
        assertEquals("ACME HARDWARE LTD", parsed.supplierGuess)
    }

    @Test
    fun `handles messy money formats`() {
        val parsed = ReceiptParser.parse(lines("Paint White 5L  Rs 1,200.50"))
        assertEquals(1200, parsed.lines.first().unitCostRupees)
    }

    @Test
    fun `empty input yields empty draft and a warning`() {
        val parsed = ReceiptParser.parse(emptyList())
        assertTrue(parsed.lines.isEmpty())
        assertTrue(parsed.warnings.isNotEmpty())
    }
}
